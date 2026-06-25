package com.project.point.controller

import com.project.point.controller.dto.request.PointUseCancelRequest
import com.project.point.controller.dto.request.PointUseRequest
import com.project.point.facade.PointFacade
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 동기(REST) 진입점. 오케스트레이션 흐름에서 오케스트레이터가 호출하는 포인트 참여자(participant) 엔드포인트.
 * 분산락 처리는 PointFacade 가 담당하므로 컨트롤러는 얇게 위임만 한다.
 */
@RestController
class PointController(
    private val pointFacade: PointFacade,
) {
    @PostMapping("/point/use")
    fun use(@RequestBody request: PointUseRequest) {
        pointFacade.use(request.toCommand())
    }

    @PostMapping("/point/use/cancel")
    fun cancel(@RequestBody request: PointUseCancelRequest) {
        pointFacade.cancel(request.toCommand())
    }
}
