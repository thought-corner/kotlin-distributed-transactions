package com.project.order.controller.dto

import com.project.order.service.dto.command.CreateOrderCommand

data class CreateOrderRequest(
    val orderItems: List<OrderItem>,
) {
    fun toCreateOrderCommand(): CreateOrderCommand =
        CreateOrderCommand(
            orderItems = orderItems.map { CreateOrderCommand.OrderItem(it.productId, it.quantity) },
        )

    data class OrderItem(
        val productId: Long,
        val quantity: Long,
    )
}
