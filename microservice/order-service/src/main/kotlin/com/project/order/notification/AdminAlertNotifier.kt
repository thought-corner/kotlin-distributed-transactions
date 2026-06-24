package com.project.order.notification

import com.project.order.service.dto.PendingOrderView
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 자동 복구로 풀리지 않은 PENDING 주문을 어드민에게 알리는 책임.
 * 지금은 WARN 로그로 대체하며, 운영에서는 이 지점을 Slack/메일/PagerDuty 연동으로 교체한다.
 */
@Component
class AdminAlertNotifier {
    private val log = LoggerFactory.getLogger(javaClass)

    fun alertStuckPendingOrder(order: PendingOrderView) {
        log.warn(
            "[ADMIN-ALERT] 자동 복구 한계를 초과한 PENDING 주문입니다. 수동 제어가 필요합니다. " +
                "orderId={}, recoveryAttempts={}, pendingSince={}",
            order.orderId,
            order.recoveryAttempts,
            order.updatedAt,
        )
    }
}
