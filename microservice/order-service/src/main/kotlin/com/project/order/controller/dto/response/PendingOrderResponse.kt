package com.project.order.controller.dto.response

import com.project.order.domain.Order.OrderStatus
import com.project.order.service.dto.PendingOrderView
import java.time.LocalDateTime

data class PendingOrderResponse(
    val orderId: Long,
    val status: OrderStatus,
    val recoveryAttempts: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(view: PendingOrderView): PendingOrderResponse =
            PendingOrderResponse(
                orderId = view.orderId,
                status = view.status,
                recoveryAttempts = view.recoveryAttempts,
                createdAt = view.createdAt,
                updatedAt = view.updatedAt,
            )
    }
}
