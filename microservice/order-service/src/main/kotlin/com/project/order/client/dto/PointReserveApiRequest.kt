package com.project.order.client.dto

data class PointReserveApiRequest(
    val requestId: String,
    val userId: Long,
    val reserveAmount: Long,
)
