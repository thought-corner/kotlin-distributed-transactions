package com.project.point.controller.dto.request

import com.project.point.service.dto.command.PointReserveCommand

data class PointReserveRequest(
    val requestId: String,
    val userId: Long,
    val reserveAmount: Long,
) {
    fun toCommand(): PointReserveCommand =
        PointReserveCommand(
            requestId = requestId,
            userId = userId,
            reserveAmount = reserveAmount,
        )
}
