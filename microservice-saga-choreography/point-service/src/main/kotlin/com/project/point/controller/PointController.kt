package com.project.point.controller

import com.project.point.controller.dto.request.PointUseCancelRequest
import com.project.point.controller.dto.request.PointUseRequest
import com.project.point.facade.PointFacade
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 동기(REST) 진입점. 코레오그래피 흐름에서는 QuantityDecreasedConsumer 가 PointSagaCoordinator 를 통해
 * 포인트를 사용하므로, 이 컨트롤러는 동기 방식 비교/단독 테스트용으로 남겨 둔다.
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
