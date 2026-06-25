package com.project.point.consumer.event

data class QuantityDecreasedEvent(
    val orderId: Long,
    val totalPrice: Long,
)
