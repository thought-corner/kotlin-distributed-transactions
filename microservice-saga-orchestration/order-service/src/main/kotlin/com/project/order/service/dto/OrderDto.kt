package com.project.order.service.dto

data class OrderDto(
    val orderItems: List<OrderItem>,
) {
    data class OrderItem(
        val productId: Long,
        val quantity: Long,
    )
}
