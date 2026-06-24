package com.project.order.scheduler

import com.project.order.facade.OrderCoordinator
import com.project.order.notification.AdminAlertNotifier
import com.project.order.service.OrderService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Confirm 단계 실패로 PENDING 에 박힌 주문을 주기적으로 재구동한다.
 *
 * - graceMillis 보다 오래 묵은 PENDING 만 대상으로 한다(진행 중 요청과의 경합 방지).
 * - recoveryAttempts 가 maxAttempts 미만이면 confirm 을 멱등 재시도한다.
 * - maxAttempts 이상이면 자동 복구를 포기하고 어드민에게 알려 수동 제어(강제 confirm/cancel)에 맡긴다.
 *
 * ## 설계 근거: 왜 '제한된 자동 재시도 → 어드민 수동 제어'로 이원화했는가
 *
 * PENDING 은 Confirm 단계가 일부만 성공한 in-doubt(미결) 상태다. 이 시점에는 참여자(상품/포인트)의
 * 실제 상태가 제각각일 수 있어(예: 포인트는 확정됐는데 재고 확정은 실패), 시스템이 최종 결과를
 * confirm 으로 끝낼지 cancel 로 되돌릴지 스스로 안전하게 단정할 수 없다.
 *
 * 1. 실패 원인은 크게 (a) 일시적 장애(네트워크 순단·타임아웃·락 경합)와 (b) 영속적 장애
 *    (참여자의 영구 거부, 데이터 불일치, 비즈니스 규칙 위반)로 나뉜다.
 * 2. (a)는 그냥 다시 시도하면 대부분 자연히 해소되므로, 사람을 부르지 않고 멱등 재시도로 싸게 처리한다.
 * 3. 그러나 (b)에 대해 무한 자동 재시도는 수렴하지 않고 부하만 키운다. 또 자동으로 일괄 cancel/confirm 을
 *    강행하면, 실제로는 반대 결과가 맞는 주문까지 잘못 뒤집을 위험이 있다
 *    (이미 확정된 거래를 잘못 환불하거나, 취소돼야 할 거래를 잘못 확정).
 * 4. 따라서 재시도 횟수에 상한을 두어 (a)는 자동으로 흡수하고, 상한을 넘긴 잔여 건만 사람에게 escalation 한다.
 *    사람은 참여자들의 실제 상태를 보고 confirm/cancel 중 올바른 쪽을 판단할 수 있다.
 *
 * 결과적으로 (1) 잘못된 자동 판단의 폭발 반경과 (2) 운영 알림 소음을 동시에 최소화하면서,
 * 정말로 판단이 필요한 소수 건에만 사람의 개입을 집중시킨다.
 */
@Component
class PendingOrderRecoveryScheduler(
    private val orderService: OrderService,
    private val orderCoordinator: OrderCoordinator,
    private val adminAlertNotifier: AdminAlertNotifier,
    @Value("\${order.recovery.max-attempts:5}") private val maxAttempts: Int,
    @Value("\${order.recovery.grace-millis:30000}") private val graceMillis: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${order.recovery.interval-millis:60000}")
    fun recoverPendingOrders() {
        val threshold = LocalDateTime.now().minusNanos(graceMillis * 1_000_000)
        val candidates = orderService.findRecoverablePendingOrders(threshold)
        if (candidates.isEmpty()) return

        log.info("PENDING 주문 복구 시작: {}건", candidates.size)

        candidates.forEach { order ->
            if (order.recoveryAttempts >= maxAttempts) {
                // 이미 한계를 넘겼다면 자동 재시도하지 않고 어드민 수동 제어로 넘긴다.
                adminAlertNotifier.alertStuckPendingOrder(order)
                return@forEach
            }

            try {
                orderService.recordRecoveryAttempt(order.orderId)
                orderCoordinator.confirm(order.orderId)
                log.info("PENDING 주문 자동 복구 성공: orderId={}", order.orderId)
            } catch (e: Exception) {
                val attempts = order.recoveryAttempts + 1
                log.warn(
                    "PENDING 주문 복구 실패: orderId={}, attempts={}/{}",
                    order.orderId,
                    attempts,
                    maxAttempts,
                    e,
                )
                // 이번 시도로 한계에 도달했다면 즉시 어드민에 알린다(다음 주기를 기다리지 않음).
                if (attempts >= maxAttempts) {
                    adminAlertNotifier.alertStuckPendingOrder(order.copy(recoveryAttempts = attempts))
                }
            }
        }
    }
}
