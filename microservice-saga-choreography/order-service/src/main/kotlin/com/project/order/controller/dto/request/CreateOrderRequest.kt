package com.project.order.controller.dto.request

import com.project.order.service.dto.command.CreateOrderCommand

data class CreateOrderRequest(
    val items: List<OrderItem>,
) {
    fun toCommand(): CreateOrderCommand =
        CreateOrderCommand(
            items.map { CreateOrderCommand.OrderItem(it.productId, it.quantity) },
        )

    data class OrderItem(
        val productId: Long,
        val quantity: Long,
    )
}
