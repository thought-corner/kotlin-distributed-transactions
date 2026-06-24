# 마이크로서비스(TCC) 구현 분석 — 비즈니스 요구사항을 어떻게 기술적으로 풀었는가

> 대상: `microservice` 멀티모듈 (order / product / point — Spring Boot · Kotlin · JPA(MySQL) + Redis)
> 작성일: 2026-06-24
> 기준: 루트 `README.md` 요구사항 1~5
> 성격: **학습용 기록.** monolithic 보고서와 같은 틀로, "분산 환경에서 각 요구사항을 어떤 기술 선택으로 풀어냈는가"에 초점을 둔다.
> 짝 문서: [`../monolithic/REQUIREMENTS_ANALYSIS.md`](../monolithic/REQUIREMENTS_ANALYSIS.md)

---

## 1. 한눈에 보기

| # | 요구사항 | 기술적 해법 | 핵심 키워드 |
|---|----------|------------|------------|
| 1 | 주문 데이터 저장 | order-service 전용 DB(`commerce_order`)에 영속화 | `Order`(상태머신) / `OrderItem` |
| 2 | 재고관리 | product-service에서 **가예약→확정/취소** 2단계 | `reservedQuantity` + `ProductReservation` |
| 3 | 포인트 사용 | point-service에서 동일한 2단계 차감 | `reservedAmount` + `PointReservation` |
| 4 | 데이터 정합성 | **TCC(Try-Confirm-Cancel)** 분산 트랜잭션 | `OrderCoordinator` + 보상(Cancel) |
| 5 | 동일 주문 1회 | **분산락 + requestId 멱등 + 상태머신** | `withLock` + `requestId(=orderId)` |

monolithic이 단일 트랜잭션으로 "공짜로" 얻던 정합성(요구 4)이 **서비스·DB 분리로 사라지는 지점**이 이 모듈의 본질이다. 그 빈자리를 **TCC 패턴**으로 메운다.

---

## 2. 구성과 처리 흐름

### 서비스 분리 (Database per Service)

| 서비스 | 포트 | 전용 스키마 | 책임 |
|--------|:----:|------------|------|
| order-service | 8081 | `commerce_order` | 주문 + **TCC 코디네이터** (+Redis 락) |
| product-service | 8082 | `commerce_product` | 재고 가예약/확정/취소 |
| point-service | 8083 | `commerce_point` | 포인트 가예약/확정/취소 |

각 서비스는 **남의 DB를 직접 보지 못한다.** 따라서 주문·재고·포인트를 한 트랜잭션으로 묶을 수 없고, order-service가 **HTTP로 각 참여자를 오케스트레이션**한다.

### 결제(place) 흐름 — TCC 2단계

```
POST /order/place
  -> OrderController          HTTP 어댑터 (얇게 위임만)
  -> OrderFacade              Redis 분산락(order:{orderId}) 안에서 아래를 호출
  -> OrderCoordinator         TCC 오케스트레이션

  [Try] 가예약
    1. order.reserve()        CREATED -> RESERVED
    2. POST product/reserve   reservedQuantity++   (가예약 · 실재고 보존)
    3. POST point/reserve     reservedAmount++     (가예약 · 실잔액 보존)
    +- 하나라도 실패하면: order/product/point 전체 Cancel 보상 -> 예외 전파 (Confirm 미진입)

  [Confirm] 확정
    1. POST product/confirm   quantity--           (실재고 차감)
    2. POST point/confirm     amount--             (실잔액 차감)
    3. order.confirm()        RESERVED -> CONFIRMED
    +- 실패하면: order.pending(PENDING) -> 예외 전파 (멱등 재시도로 완수 대상)
```

핵심은 **"가예약(Try)"과 "실확정(Confirm)"의 분리**다. Confirm 이전에는 어느 자원도 실제로 소비되지 않으므로, Try 도중 실패하면 가예약만 되돌리면(Cancel) 깔끔히 원복된다.

---

## 3. 요구사항별 기술적 해법

### 요구 1 — 주문 데이터 저장
- `Order`(상태머신: CREATED/RESERVED/CONFIRMED/CANCELLED/PENDING/COMPLETED), `OrderItem`을 **order-service 전용 DB**에 JPA로 영속화.
- monolithic과 달리 주문 데이터가 **독립 서비스/스키마**에 격리된다.
- **충족.**

### 요구 2 — 재고관리 (TCC 참여자)
- 재고를 한 번에 차감하지 않고 **2단계**로 나눈 것이 핵심.
  - `reserve()` — 실재고(`quantity`)는 그대로 두고 **`reservedQuantity`만 증가**(가예약), `ProductReservation(RESERVED)` 저장.
  - `confirm()` — 이때 **`quantity`를 실제 차감**, 예약 `CONFIRMED`.
  - `cancel()` — `reservedQuantity` 원복, 예약 `CANCELLED`(보상).
- 동시 재고 변경은 `@Version` **낙관적 락**으로 보호하고, 충돌 시 파사드가 최대 3회 재시도.
- **충족.** (monolithic의 단순 `decreaseStock`보다 분산 환경에 맞게 정교화됨)

### 요구 3 — 포인트 사용 (TCC 참여자)
- 재고와 동일한 2단계: `reserve()`(`reservedAmount`↑) → `confirm()`(`amount`↓) / `cancel()`(원복).
- `@Version` 낙관적 락 + 파사드 재시도 동일.
- 현재 사용자는 코디네이터에서 `userId = 1L`로 고정 — 인증/사용자 식별 도입 전 **의도적 단순화**.
- **부분 충족.**

### 요구 4 — 데이터 정합성 (이 모듈의 핵심)
monolithic은 단일 DB·단일 `@Transactional`로 원자성을 공짜로 얻었다. 여기서는 **서비스마다 DB가 분리되어 그 단일 트랜잭션이 불가능**하다. 대신 **TCC(Try-Confirm-Cancel)** 로 애플리케이션 레벨의 정합성을 만든다.

```
단일 트랜잭션 없음 — 참여자별 로컬 트랜잭션 + 애플리케이션 오케스트레이션

  Try 단계     세 참여자에 '가예약'
    +- 하나라도 실패하면: 모두 Cancel(보상) -> 예외 전파  => 아무 자원도 소비되지 않음
  Confirm 단계  세 참여자에 '실확정'
    +- 자원은 Try 에서 이미 확보됨 -> 멱등 재시도로 끝까지 완수가 원칙
```

- **Try 실패 → 완전 보상**: `OrderCoordinator.reserve()`의 `catch`가 order/product/point를 모두 cancel한 뒤 **예외를 전파(`throw e`)** → Confirm으로 넘어가지 않는다. (보상과 실패 전파가 맞물려, 부분 반영 없이 원복)
- **Confirm 실패 → 보상하지 않음**: 이미 자원을 확보했으므로 취소하면 안 된다. 멱등(`CONFIRMED`면 조기 return) + HTTP `@Retryable`(3회)로 재시도하고, 그래도 실패하면 주문을 **PENDING**으로 표시한다.
- 결과적으로 보장 수준은 **즉시(ACID) 일관성이 아니라 최종적 일관성(eventual consistency)**. 이것이 분산 트랜잭션의 본질적 트레이드오프다.
- **충족 — 단, "최종적" 일관성**이며, 아래 5장의 미구현(PENDING 복구기)으로 인해 완결성에 한계가 있다.

### 요구 5 — 동일 주문 1회 보장 (멱등성)
분산 환경에서는 재시도·중복 호출이 일상이므로, **세 겹**으로 방어한다.

1. **분산락** — order-service가 `order:{orderId}`로 동시 결제 진입을 1개로 제한(`OrderFacade.withLock`). 각 참여자도 `product:{requestId}`, `point:{requestId}` 락으로 보호.
2. **requestId 멱등키** — 모든 참여자 호출에 `requestId = orderId`를 실어, 참여자가 **요청 단위로 예약을 식별**한다.
   - reserve: 같은 requestId 예약이 이미 있으면 그대로 반환/스킵.
   - confirm/cancel: 이미 `CONFIRMED`/`CANCELLED`면 조기 return.
   - → 코디네이터가 같은 호출을 재시도(@Retryable)해도 **중복 부작용이 없다.**
3. **주문 상태머신** — CREATED→RESERVED→CONFIRMED 전이 가드로 재처리를 막는다.

- **충족.** (단순 락 + 상태였던 monolithic보다, requestId 멱등을 전 구간에 깐 점이 분산 환경에 더 견고하다)

---

## 4. 이 구현에서 배울 핵심 설계 원칙

1. **DB per Service가 정합성을 깬다** — 서비스를 나누는 순간 단일 트랜잭션이 사라지고, 정합성은 "공짜"에서 "직접 설계해야 하는 것"이 된다.
2. **TCC = 자원 확보(Try)와 확정(Confirm)의 분리** — Confirm 전에는 실제 소비가 없어, 실패 비용이 "가예약 취소"로 국한된다.
3. **Try와 Confirm의 실패 처리는 다르다** — Try 실패는 **보상(Cancel) 후 중단**, Confirm 실패는 **취소 금지 + 멱등 재시도로 완수**. (자원을 이미 잡았기 때문)
4. **멱등성은 분산 트랜잭션의 척추** — `requestId`로 모든 참여자 연산을 멱등하게 만들어야 재시도가 안전하다.
5. **책임 최소화(SRP)** — 락 생명주기는 `RedisLockService.withLock`이 단독으로, 컨트롤러는 HTTP 매핑만, 코디네이터는 오케스트레이션만 담당한다.

---

## 5. 현재 단계의 단순화와 다음 학습 주제

| 주제 | 현재 단계의 단순화 | 다음 단계에서 다룰 것 |
|------|------------------|---------------------|
| Confirm 완결성 | 실패 시 PENDING 표시 + HTTP 3회 재시도까지 | **PENDING 복구기**(스케줄러/메시지 재처리)로 "끝까지 성공" 완성 |
| 보상(Cancel) 견고성 | Cancel 실패는 `@Retryable` 3회까지만 | 보상 실패 영속화 + 복구 → 가예약 누수 방지 |
| 가예약 수명 | RESERVED 예약을 정리하지 않음 | TTL/스윕으로 미완 가예약 회수 |
| 사용자 식별 | `userId = 1L` 고정 | 주문 주체를 받아 실제 사용자 포인트 차감 |
| DB 격리 | 단일 MySQL + 스키마 3개(논리 격리) | 인스턴스 분리(물리 격리) |
| 코디네이터 내구성 | 진행 상태를 주문 status로만 추적 | Saga 로그 영속화로 크래시 복구 |
| 런타임 검증 | 컴파일·단위 테스트까지 | 3서비스 기동 스모크(E2E) + spring-retry on Boot4 동작 확인 |

> 검증 현황: 참여자 도메인 단위 테스트(Product/Point/Order) + 코디네이터 단위 테스트(MockK, Try→Confirm 순서 / Try 실패 시 보상·Confirm 미진입)로 TCC 로직을 결정적으로 검증. 풀 E2E는 다음 단계.

---

## 6. 결론

이 마이크로서비스 모듈은 5개 요구사항을 **(1) 서비스별 전용 DB로 주문 데이터를, (2)(3) 재고·포인트를 가예약→확정 2단계로, (4) TCC로 분산 정합성을, (5) 분산락+requestId 멱등+상태머신으로 동일 주문 1회를** 푸는 방식으로 구현했다.

monolithic과 대비하면 명확하다. **monolithic은 단일 트랜잭션으로 정합성을 공짜로 얻는 대신 확장·독립 배포가 어렵고, 마이크로서비스는 독립성을 얻는 대신 정합성을 TCC로 직접 build해야 한다.** 그리고 그 대가는 **즉시 일관성 → 최종적 일관성**으로의 후퇴다.

기능적으로 5개 요구사항을 모두 구현했으나, 요구 4의 "완결성"은 **PENDING 복구기와 보상 실패 복구**가 채워져야 견고해진다(5장). 요구 3의 `userId` 고정도 사용자 식별 도입 시 정정 대상이다.
