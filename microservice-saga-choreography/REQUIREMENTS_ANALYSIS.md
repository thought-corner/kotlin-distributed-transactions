# 사가 코레오그래피(Saga Choreography) 구현 분석 — 비즈니스 요구사항을 어떻게 기술적으로 풀었는가

> 대상: `microservice-saga-choreography` 멀티모듈 (order / product / point — Spring Boot · Kotlin · JPA(MySQL) + Redis + **Kafka**)
> 작성일: 2026-06-25
> 기준: 루트 `README.md` 요구사항 1~5
> 성격: **학습용 기록.** TCC 보고서와 같은 틀로, "분산 환경에서 각 요구사항을 어떤 기술 선택으로 풀어냈는가"에 초점을 둔다.
> 짝 문서: [`../microservice-saga-orchestration/REQUIREMENTS_ANALYSIS.md`](../microservice-saga-orchestration/REQUIREMENTS_ANALYSIS.md) · [`../microservice/REQUIREMENTS_ANALYSIS.md`](../microservice/REQUIREMENTS_ANALYSIS.md)

---

## 1. 한눈에 보기

| # | 요구사항 | 기술적 해법 | 핵심 키워드 |
|---|----------|------------|------------|
| 1 | 주문 데이터 저장 | order-service 전용 DB(`commerce_order`)에 영속화 + 커밋 후 이벤트 발행 | `Order`(상태머신) / `afterCommit` |
| 2 | 재고관리 | product-service가 `order-placed` 구독 → **즉시 차감**, 실패 시 보상 | `ProductTransactionHistory` / `quantity--` |
| 3 | 포인트 사용 | point-service가 `quantity-decreased` 구독 → **즉시 사용**, 실패 시 보상 | `PointTransactionHistory` / `amount--` |
| 4 | 데이터 정합성 | **사가 코레오그래피** — 중앙 조정자 없이 이벤트 체인 + 보상 이벤트 | Kafka 토픽 / `SagaCoordinator` |
| 5 | 동일 주문 1회 | **분산락 + requestId 멱등 + 상태머신 + 파티션 키** | `withLock` + `requestId(=orderId)` |

이 모듈의 본질은 **"누가 다음 단계를 지휘하는가"** 다. TCC/오케스트레이션이 중앙 조정자(order-service)에게 제어를 모았다면, 코레오그래피는 **제어를 분산**한다 — 각 서비스는 자기가 구독한 이벤트에 반응해 일을 하고 **다음 이벤트를 발행할 뿐**, 전체 흐름을 지휘하는 주체가 없다.

---

## 2. 구성과 처리 흐름

### 서비스 분리 (Database per Service)

| 서비스 | 포트 | 전용 스키마 | 책임 |
|--------|:----:|------------|------|
| order-service | 8081 | `commerce_order` | 주문 + 사가 시작/종착 (+Redis 락) |
| product-service | 8082 | `commerce_product` | 재고 차감/보상 (`order-placed` 구독) |
| point-service | 8083 | `commerce_point` | 포인트 사용/보상 (`quantity-decreased` 구독) |

각 서비스는 **남의 DB를 직접 보지 못하고**, 서로를 **직접 호출하지도 않는다.** 오직 **Kafka 이벤트**로만 연결된다.

### 이벤트 체인 — 성공 흐름

```
POST /order/place
  -> OrderController -> OrderFacade(Redis 락) -> OrderService.placeOrder
       order.request()  CREATED -> REQUESTED  (로컬 트랜잭션 커밋)
       afterCommit: publish [order-placed]            # 커밋 후에만 발행(유령 이벤트 방지)

  product: @KafkaListener[order-placed]
       재고 차감(buy) + 이력 PURCHASE  -> publish [quantity-decreased]

  point:   @KafkaListener[quantity-decreased]
       포인트 사용(use) + 이력 USE     -> publish [point-used]

  order:   @KafkaListener[point-used]
       order.complete()  REQUESTED -> COMPLETED
```

### 이벤트 체인 — 실패/보상 흐름 (제어가 분산되어 "되감긴다")

```
재고 차감 실패:   product 보상(cancel) -> publish [quantity-decreased-fail] -> order.fail() FAILED
포인트 사용 실패: point 보상(cancel)  -> publish [point-use-fail]
                 -> product 가 [point-use-fail] 구독 -> 재고 보상(cancel) -> publish [quantity-decreased-fail]
                 -> order.fail() FAILED
```

토픽: `order-placed`, `quantity-decreased`, `point-used`, `quantity-decreased-fail`, `point-use-fail` (Kafka 키 = `orderId`).

핵심은 **보상도 "이벤트"** 라는 점이다. 실패한 서비스가 실패 이벤트를 발행하면, **앞 단계 서비스가 그걸 구독해 스스로 되돌린다.** 누구도 "롤백하라"고 명령하지 않는다.

---

## 3. 요구사항별 기술적 해법

### 요구 1 — 주문 데이터 저장
- `Order`(상태머신: CREATED/REQUESTED/COMPLETED/FAILED), `OrderItem`을 **order-service 전용 DB**에 JPA로 영속화.
- 사가 시작점은 **`afterCommit`** 으로 `order-placed`를 발행한다 — DB 커밋이 끝난 뒤에만 이벤트가 나가므로, 트랜잭션이 롤백되면 유령 이벤트가 발생하지 않는다.
- **충족.**

### 요구 2 — 재고관리 (사가 참여자)
- product-service가 `order-placed`를 구독해 **재고를 즉시 차감**(`Product.buy` → `quantity--`)하고 `ProductTransactionHistory(PURCHASE)`를 남긴다.
- TCC의 "가예약→확정" 2단계와 달리, 코레오그래피는 **"즉시 차감 + 실패 시 보상(cancel)"** 방식이다. 보상은 `quantity++` + 이력 `CANCEL`.
- 동시 재고 변경은 `@Version` **낙관적 락**으로 보호.
- **충족.**

### 요구 3 — 포인트 사용 (사가 참여자)
- point-service가 `quantity-decreased`를 구독해 **포인트를 즉시 사용**(`Point.use` → `amount--`), 이력 `USE`. 보상은 `amount++` + `CANCEL`.
- 사용자는 코디네이터에서 `userId = 1L`로 고정 — 인증/사용자 식별 도입 전 **의도적 단순화**.
- **부분 충족.**

### 요구 4 — 데이터 정합성 (이 모듈의 핵심)
단일 DB·단일 트랜잭션이 불가능한 분산 환경에서, **사가 코레오그래피**로 애플리케이션 레벨의 정합성을 만든다.

```
중앙 조정자 없음 — 각 서비스의 로컬 트랜잭션 + 이벤트 연쇄

  성공: 로컬 트랜잭션 커밋 -> 다음 이벤트 발행 -> 다음 참여자가 이어받음
  실패: 보상(cancel) 후 '실패 이벤트' 발행 -> 앞 참여자가 구독해 스스로 보상 -> order FAILED
```

- **제어의 분산**: 전체 흐름을 지휘하는 코드가 한 곳에 없다. 각 서비스의 `SagaCoordinator`는 **"내가 받은 이벤트에 대한 성공/실패 분기"만** 책임진다.
- **보상의 전파**: 포인트 실패는 `point-use-fail` → product가 재고 복원 → `quantity-decreased-fail` → order `FAILED`로 **이벤트를 타고 거꾸로 흐른다.**
- 보장 수준은 **즉시(ACID) 일관성이 아니라 최종적 일관성(eventual consistency)**. Kafka는 **at-least-once** 전달이므로 같은 이벤트가 중복 도착할 수 있고, 그래서 요구 5의 멱등성이 **정합성의 전제 조건**이 된다.
- **충족 — 단, "최종적" 일관성**이며, 5장의 미구현(아웃박스/DLT)으로 완결성에 한계가 있다.

### 요구 5 — 동일 주문 1회 보장 (멱등성)
비동기·재전송이 일상인 환경이라 **네 겹**으로 방어한다.

1. **분산락** — `order:{orderId}`로 동시 `place` 진입을 1개로 제한(`OrderFacade.withLock`).
2. **requestId 멱등키** — 모든 참여 연산에 `requestId = orderId`. product/point는 `TransactionHistory`를 조회해 **이미 처리한 요청이면 조기 return**(중복 차감·중복 보상 방지).
3. **주문 상태머신** — CREATED→REQUESTED→COMPLETED/FAILED 전이 가드.
4. **파티션 키 = orderId** — 같은 주문의 이벤트는 같은 파티션으로 가 **순서가 보장**된다.
- **충족.**

---

## 4. 이 구현에서 배울 핵심 설계 원칙

1. **코레오그래피 = 제어의 분산** — 중앙 조정자가 없다. 결합도가 낮아 서비스 추가/확장이 쉽지만, **전체 흐름이 코드 한곳에 보이지 않아** 추적·디버깅이 어렵다.
2. **보상도 이벤트다** — 실패를 "실패 이벤트"로 발행하면 앞 서비스가 구독해 자가 보상한다. 명령형 롤백이 아니라 **반응형 되감기**.
3. **커밋 후 발행(afterCommit)** — 이벤트는 로컬 트랜잭션이 커밋된 뒤에만 내보내야 유령 이벤트가 없다.
4. **at-least-once → 멱등은 필수** — Kafka 중복 전달을 전제로, `requestId` 기반 멱등이 없으면 정합성이 깨진다.
5. **컨슈머는 얇게, 오케스트레이션은 코디네이터로** — `@KafkaListener`는 역직렬화+위임만, 성공/실패 분기는 `SagaCoordinator`가(SRP).

---

## 5. 현재 단계의 단순화와 다음 학습 주제

| 주제 | 현재 단계의 단순화 | 다음 단계에서 다룰 것 |
|------|------------------|---------------------|
| 이벤트 발행 원자성 | 로컬 커밋 후 `producer.send`(별도 단계) | **트랜잭셔널 아웃박스**(커밋 사이 크래시 시 이벤트 유실 방지) |
| 소비 실패 처리 | 컨슈머 예외 시 보상 발행까지 | **DLT/재시도 정책**(무한 재처리·유실 방지) |
| 사용자 식별 | `userId = 1L` 고정 | 주문 주체를 실어 실제 사용자 포인트 차감 |
| 보상 멱등 견고성 | history 기반 조기 return | 보상 실패 적재·재처리 (가예약 누수 방지) |
| 흐름 가시성 | 흐름이 여러 서비스에 흩어짐 | 사가 추적(correlation-id/분산 트레이싱) |
| DB 격리 | 단일 MySQL + 스키마 3개(논리 격리) | 인스턴스 분리(물리 격리) |
| 런타임 검증 | 단위 + 통합(`@SpringBootTest`+H2) 테스트 | 3서비스 + 실 Kafka E2E |

> 검증 현황: 도메인 단위 테스트 + `SagaCoordinator` 단위 테스트(MockK, 성공/보상 분기) + **통합 테스트(`@SpringBootTest`+H2)** 로 재고/잔액 보상이 실 DB에 반영되는지·멱등성을 결정적으로 검증. 풀 E2E는 다음 단계.

---

## 6. 결론

이 모듈은 5개 요구사항을 **(1) 전용 DB + 커밋 후 이벤트로 주문을, (2)(3) 재고·포인트를 "즉시 차감 + 보상"으로, (4) 사가 코레오그래피로 분산 정합성을, (5) 분산락+requestId 멱등+상태머신+파티션 키로 동일 주문 1회를** 푸는 방식으로 구현했다.

TCC·오케스트레이션과의 본질적 차이는 **제어의 위치**다. 코레오그래피는 중앙 지휘자를 없애 **느슨한 결합과 확장성**을 얻는 대신, **흐름의 가시성과 추적성**을 내준다. 그리고 정합성은 여기서도 **즉시 일관성 → 최종적 일관성**으로 후퇴하며, 그 완결성은 **아웃박스와 DLT**가 채워져야 견고해진다(5장).
