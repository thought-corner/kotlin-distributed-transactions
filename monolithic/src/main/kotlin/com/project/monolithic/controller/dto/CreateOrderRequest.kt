package com.project.monolithic.controller.dto

import com.project.monolithic.service.dto.CreateOrderCommand

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