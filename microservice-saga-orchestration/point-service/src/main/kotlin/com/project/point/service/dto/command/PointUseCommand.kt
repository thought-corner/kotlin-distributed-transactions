package com.project.point.service.dto.command

data class PointUseCommand(
    val requestId: String,
    val userId: Long,
    val amount: Long,
)
