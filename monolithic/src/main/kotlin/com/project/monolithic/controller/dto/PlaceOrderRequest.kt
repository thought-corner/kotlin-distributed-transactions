package com.project.monolithic.controller.dto

import com.project.monolithic.service.dto.PlaceOrderCommand

data class PlaceOrderRequest(
    val orderId: Long,
) {
    fun toPlaceOrderCommand(): PlaceOrderCommand = PlaceOrderCommand(orderId)
}