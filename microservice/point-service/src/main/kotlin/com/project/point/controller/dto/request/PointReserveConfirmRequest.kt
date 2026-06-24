package com.project.point.controller.dto.request

import com.project.point.service.dto.command.PointReserveConfirmCommand

data class PointReserveConfirmRequest(
    val requestId: String,
) {
    fun toCommand(): PointReserveConfirmCommand = PointReserveConfirmCommand(requestId)
}
