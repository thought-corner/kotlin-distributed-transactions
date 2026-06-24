package com.project.point.service.dto.command

data class PointReserveCommand(
    val requestId: String,
    val userId: Long,
    val reserveAmount: Long,
)
