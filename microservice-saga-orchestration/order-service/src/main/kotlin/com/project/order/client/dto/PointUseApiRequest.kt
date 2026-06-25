package com.project.order.client.dto

data class PointUseApiRequest(
    val requestId: String,
    val userId: Long,
    val amount: Long,
)
