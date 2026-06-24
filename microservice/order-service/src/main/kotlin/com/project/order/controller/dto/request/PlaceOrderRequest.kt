package com.project.order.controller.dto.request

import com.project.order.service.dto.command.PlaceOrderCommand

data class PlaceOrderRequest(
    val orderId: Long,
) {
    fun toCommand(): PlaceOrderCommand = PlaceOrderCommand(orderId)
}
