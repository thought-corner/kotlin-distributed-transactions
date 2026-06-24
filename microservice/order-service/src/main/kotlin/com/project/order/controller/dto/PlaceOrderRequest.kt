package com.project.order.controller.dto

import com.project.order.service.dto.command.PlaceOrderCommand

data class PlaceOrderRequest(
    val orderId: Long,
) {
    fun toCommand(): PlaceOrderCommand = PlaceOrderCommand(orderId)
}
