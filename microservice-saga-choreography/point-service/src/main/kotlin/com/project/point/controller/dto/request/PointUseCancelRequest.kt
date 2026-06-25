package com.project.point.controller.dto.request

import com.project.point.service.dto.command.PointUseCancelCommand

data class PointUseCancelRequest(
    val requestId: String,
) {
    fun toCommand(): PointUseCancelCommand = PointUseCancelCommand(requestId)
}
