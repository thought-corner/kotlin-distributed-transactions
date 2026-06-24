package com.project.point.controller.dto.request

import com.project.point.service.dto.command.PointReserveCancelCommand

data class PointReserveCancelRequest(
    val requestId: String,
) {
    fun toCommand(): PointReserveCancelCommand = PointReserveCancelCommand(requestId)
}
