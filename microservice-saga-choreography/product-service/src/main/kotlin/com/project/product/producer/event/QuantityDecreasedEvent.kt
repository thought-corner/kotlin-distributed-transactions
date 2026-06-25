package com.project.product.producer.event

data class QuantityDecreasedEvent(
    val orderId: Long,
    val totalPrice: Long,
)
