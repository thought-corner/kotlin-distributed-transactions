package com.project.order.service.dto

import com.project.order.domain.Order
import com.project.order.domain.Order.OrderStatus
import java.time.LocalDateTime

/** PENDING 주문을 스케줄러/어드민에 노출하기 위한 읽기 전용 뷰. */
data class PendingOrderView(
    val orderId: Long,
    val status: OrderStatus,
    val recoveryAttempts: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(order: Order): PendingOrderView =
            PendingOrderView(
                orderId = requireNotNull(order.id) { "저장되지 않은 주문은 조회할 수 없습니다." },
                status = order.status,
                recoveryAttempts = order.recoveryAttempts,
                createdAt = order.createdAt,
                updatedAt = order.updatedAt,
            )
    }
}
