package com.project.point.controller

import com.project.point.controller.dto.request.PointReserveCancelRequest
import com.project.point.controller.dto.request.PointReserveConfirmRequest
import com.project.point.controller.dto.request.PointReserveRequest
import com.project.point.facade.PointFacadeService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PointController(
    private val pointFacadeService: PointFacadeService,
) {
    @PostMapping("/point/reserve")
    fun reserve(
        @RequestBody request: PointReserveRequest,
    ) {
        pointFacadeService.tryReserve(request.toCommand())
    }

    @PostMapping("/point/confirm")
    fun confirm(
        @RequestBody request: PointReserveConfirmRequest,
    ) {
        pointFacadeService.confirmReserve(request.toCommand())
    }

    @PostMapping("/point/cancel")
    fun cancel(
        @RequestBody request: PointReserveCancelRequest,
    ) {
        pointFacadeService.cancelReserve(request.toCommand())
    }
}
