# 사가 오케스트레이션(Saga Orchestration) 구현 분석 — 비즈니스 요구사항을 어떻게 기술적으로 풀었는가

> 대상: `microservice-saga-orchestration` 멀티모듈 (order / product / point — Spring Boot · Kotlin · JPA(MySQL) + Redis, **동기 REST**)
> 작성일: 2026-06-25
> 기준: 루트 `README.md` 요구사항 1~5
> 성격: **학습용 기록.** TCC 보고서와 같은 틀로, "분산 환경에서 각 요구사항을 어떤 기술 선택으로 풀어냈는가"에 초점을 둔다.
> 짝 문서: [`../microservice-saga-choreography/REQUIREMENTS_ANALYSIS.md`](../microservice-saga-choreography/REQUIREMENTS_ANALYSIS.md) · [`../microservice/REQUIREMENTS_ANALYSIS.md`](../microservice/REQUIREMENTS_ANALYSIS.md)

---

## 1. 한눈에 보기

| # | 요구사항 | 기술적 해법 | 핵심 키워드 |
|---|----------|------------|------------|
| 1 | 주문 데이터 저장 | order-service 전용 DB(`commerce_order`)에 영속화 | `Order`(상태머신) / `OrderItem` |
| 2 | 재고관리 | product-service가 REST 호출을 받아 **즉시 차감**, 실패 시 보상 | `ProductTransactionHistory` / `quantity--` |
| 3 | 포인트 사용 | point-service가 REST 호출을 받아 **즉시 사용**, 실패 시 보상 | `PointTransactionHistory` / `amount--` |
| 4 | 데이터 정합성 | **사가 오케스트레이션** — 중앙 `OrderCoordinator`가 순차 호출·보상 | `OrderCoordinator` / `CompensationRegistry` |
| 5 | 동일 주문 1회 | **분산락 + requestId 멱등 + 상태머신** | `withLock` + `requestId(=orderId)` |

이 모듈의 본질은 **"제어를 한 곳에 모은다"** 다. 코레오그래피가 제어를 분산했다면, 오케스트레이션은 **중앙 조정자(`OrderCoordinator`)** 가 참여 서비스를 **동기 REST로 순차 호출**하고, 실패하면 **자기가 직접 보상(rollback)을 호출**한다. 전체 흐름이 코드 한 곳에 모여 있다.

---

## 2. 구성과 처리 흐름

### 서비스 분리 (Database per Service)

| 서비스 | 포트 | 전용 스키마 | 책임 |
|--------|:----:|------------|------|
| order-service | 8081 | `commerce_order` | 주문 + **오케스트레이터**(`OrderCoordinator`) (+Redis 락) |
| product-service | 8082 | `commerce_product` | 재고 차감/보상 (REST `/product/buy`·`/cancel`) |
| point-service | 8083 | `commerce_point` | 포인트 사용/보상 (REST `/point/use`·`/cancel`) |

각 서비스는 **남의 DB를 직접 보지 못하고**, order-service가 **HTTP(`RestClient`)로 각 참여자를 직접 호출**한다. (Kafka 없음 — 코레오그래피와의 가장 큰 차이)

### 결제(place) 흐름 — 중앙 조정자가 순차 호출

```
POST /order/place
  -> OrderController -> OrderFacade(Redis 락) -> OrderCoordinator.placeOrder

  order.request()                 CREATED -> REQUESTED
  try {
    POST product/buy   (RestClient, @Retryable 3회)   재고 즉시 차감
    POST point/use     (RestClient, @Retryable 3회)   포인트 즉시 사용
    order.complete()              REQUESTED -> COMPLETED
  } catch (e) {
    rollback(orderId)             # 아래 보상
    throw e                       # 호출자에게 실패 전파
  }
```

### 보상(rollback) 흐름 — 조정자가 직접 되돌린다

```
rollback(orderId):
  try {
    POST product/buy/cancel       재고 복원 (취소 총액 반환)
    if (취소 총액 > 0) POST point/use/cancel   포인트 복원
    order.fail()                  REQUESTED -> FAILED
  } catch (e) {
    compensationRegistryRepository.save(CompensationRegistry(orderId))   # 보상마저 실패 -> 수동 복구 대상
    throw e
  }
```

핵심은 **보상이 "명령형"** 이라는 점이다. 조정자가 `cancel` 엔드포인트를 **직접 호출**해 되돌린다. 그리고 **보상마저 실패하면** `CompensationRegistry`에 적재해 **운영자/배치가 나중에 복구**하도록 남긴다 — 분산 환경에서 "되돌리기조차 실패할 때"의 마지막 안전망이다.

---

## 3. 요구사항별 기술적 해법

### 요구 1 — 주문 데이터 저장
- `Order`(상태머신: CREATED/REQUESTED/COMPLETED/FAILED), `OrderItem`을 **order-service 전용 DB**에 JPA로 영속화.
- 코레오그래피와 동일하나, 사가 시작은 이벤트 발행이 아니라 **`OrderCoordinator`의 동기 호출**로 진행된다.
- **충족.**

### 요구 2 — 재고관리 (사가 참여자)
- product-service가 `/product/buy`를 받아 **재고를 즉시 차감**(`Product.buy` → `quantity--`) + `ProductTransactionHistory(PURCHASE)`. 보상은 `/product/buy/cancel`(`quantity++` + `CANCEL`).
- TCC의 "가예약→확정"이 아니라 **"즉시 차감 + 실패 시 보상"** 방식(코레오그래피와 동일한 보상 사가 스타일).
- 동시 재고 변경은 `@Version` **낙관적 락**.
- **충족.**

### 요구 3 — 포인트 사용 (사가 참여자)
- point-service가 `/point/use`를 받아 **포인트를 즉시 사용**(`Point.use` → `amount--`), 이력 `USE`. 보상은 `/point/use/cancel`.
- 사용자는 조정자에서 `userId = 1L`로 고정 — 인증/사용자 식별 도입 전 **의도적 단순화**.
- **부분 충족.**

### 요구 4 — 데이터 정합성 (이 모듈의 핵심)
단일 트랜잭션이 불가능한 분산 환경에서, **사가 오케스트레이션**으로 정합성을 만든다.

```
중앙 조정자(OrderCoordinator) — 동기 REST 순차 호출 + 명령형 보상

  성공: product/buy -> point/use -> order.complete
  실패: catch -> rollback(product/cancel -> point/cancel -> order.fail) -> 예외 전파
  보상 실패: CompensationRegistry 적재 -> 예외 전파  (수동 복구 대상)
```

- **제어의 중앙화**: 성공/보상의 전체 시퀀스가 `OrderCoordinator` 한 곳에 있어 흐름이 명확하다.
- **명령형 보상 + 마지막 안전망**: 조정자가 `cancel`을 직접 호출하고, 그마저 실패하면 `CompensationRegistry`로 영속화한다.
- **동기 호출의 함의**: 호출자가 결과를 **즉시** 알 수 있어 흐름·디버깅이 쉽지만, 참여자 가용성에 강하게 의존하고(참여자 다운 → 전체 실패), 조정자가 **단일 장애점**이 된다.
- 보장 수준은 여기서도 **최종적 일관성** — `@Retryable`(3회)·`CompensationRegistry`로 완수/복구를 지향한다.
- **충족 — 단, "최종적" 일관성**이며, 5장의 미구현(조정자 크래시 복구 등)으로 완결성에 한계가 있다.

### 요구 5 — 동일 주문 1회 보장 (멱등성)
재시도·중복 호출을 전제로 **세 겹**으로 방어한다.

1. **분산락** — `order:{orderId}`로 동시 `place` 진입을 1개로 제한(`OrderFacade.withLock`). 참여자도 `product:orchestration:{requestId}`·`point:orchestration:{requestId}` 락으로 보호.
2. **requestId 멱등키** — 모든 참여 호출에 `requestId = orderId`. product/point는 `TransactionHistory`로 **이미 처리한 요청이면 조기 return**(중복 차감·중복 보상 방지) → 조정자의 `@Retryable` 재호출이 안전하다.
3. **주문 상태머신** — CREATED→REQUESTED→COMPLETED/FAILED 전이 가드.
- **충족.** (동기 순차 호출이라 코레오그래피의 "파티션 키 순서" 같은 장치는 불필요)

---

## 4. 이 구현에서 배울 핵심 설계 원칙

1. **오케스트레이션 = 제어의 중앙화** — 전체 시퀀스가 조정자 한 곳에 모여 **흐름이 명확하고 디버깅이 쉽다**. 대신 결합도가 높고(참여자 주소·가용성 의존) 조정자가 단일 장애점이 된다.
2. **보상은 명령형** — 조정자가 `cancel`을 직접 호출해 되돌린다(코레오그래피의 "보상 이벤트 전파"와 대비).
3. **되돌리기조차 실패할 때** — `CompensationRegistry`에 적재해 수동 복구 경로를 남기는 것이 분산 보상의 마지막 안전망.
4. **동기의 트레이드오프** — 즉시 결과를 알 수 있는 대신, 참여자 지연/장애가 호출 체인 전체를 막는다.
5. **책임 최소화(SRP)** — 락은 `OrderFacade`/`RedisLockService.withLock`이, 오케스트레이션은 `OrderCoordinator`가, HTTP 매핑은 컨트롤러가.

---

## 5. 현재 단계의 단순화와 다음 학습 주제

| 주제 | 현재 단계의 단순화 | 다음 단계에서 다룰 것 |
|------|------------------|---------------------|
| 조정자 내구성 | 진행 상태를 주문 status로만 추적 | **사가 상태 영속화**(조정자 크래시 후 재개) |
| 보상 실패 복구 | `CompensationRegistry`에 **적재까지** | 적재분 **재처리 배치/스케줄러**로 실제 복구 완성 |
| 동기 호출 장애 | `@Retryable` 3회까지 | 서킷 브레이커/타임아웃/벌크헤드 (조정자 보호) |
| 사용자 식별 | `userId = 1L` 고정 | 주문 주체를 받아 실제 사용자 포인트 차감 |
| 단일 장애점 | order-service가 조정자 겸임 | 조정자 이중화/리더십, 혹은 코레오그래피와의 혼용 |
| DB 격리 | 단일 MySQL + 스키마 3개(논리 격리) | 인스턴스 분리(물리 격리) |
| 런타임 검증 | 단위 + 통합(`@SpringBootTest`+H2) 테스트 | 3서비스 기동 스모크(E2E) |

> 검증 현황: `OrderCoordinator` 단위 테스트(MockK)로 **실패 시나리오에 초점** — 구매 실패→보상, 포인트 실패→재고·포인트 보상, **보상 자체 실패→`CompensationRegistry` 적재** 를 결정적으로 검증. 참여자(product/point)는 **통합 테스트(`@SpringBootTest`+H2)** 로 재고/잔액 부족 예외·보상 복원·멱등성을 검증. 풀 E2E는 다음 단계.

---

## 6. 결론

이 모듈은 5개 요구사항을 **(1) 전용 DB로 주문을, (2)(3) 재고·포인트를 "즉시 차감 + 보상"으로, (4) 사가 오케스트레이션(중앙 조정자 + 명령형 보상 + `CompensationRegistry`)으로 분산 정합성을, (5) 분산락+requestId 멱등+상태머신으로 동일 주문 1회를** 푸는 방식으로 구현했다.

코레오그래피와의 본질적 차이는 **제어의 위치**다. 오케스트레이션은 제어를 조정자에 모아 **흐름의 명확성·디버깅 용이성**을 얻는 대신, **결합도와 단일 장애점**을 떠안는다. 정합성은 여기서도 **즉시 일관성 → 최종적 일관성**으로 후퇴하며, 그 완결성은 **사가 상태 영속화와 보상 실패 재처리**가 채워져야 견고해진다(5장).
