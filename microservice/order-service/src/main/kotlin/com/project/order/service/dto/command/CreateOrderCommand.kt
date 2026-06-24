package com.project.order.service.dto.command

data class CreateOrderCommand(
    val orderItems: List<OrderItem>,
) {
    data class OrderItem(
        val productId: Long,
        val quantity: Long,
    )
}
