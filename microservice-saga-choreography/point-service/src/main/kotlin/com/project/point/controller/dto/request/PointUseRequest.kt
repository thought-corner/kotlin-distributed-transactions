package com.project.point.controller.dto.request

import com.project.point.service.dto.command.PointUseCommand

data class PointUseRequest(
    val requestId: String,
    val userId: Long,
    val amount: Long,
) {
    fun toCommand(): PointUseCommand = PointUseCommand(requestId, userId, amount)
}
