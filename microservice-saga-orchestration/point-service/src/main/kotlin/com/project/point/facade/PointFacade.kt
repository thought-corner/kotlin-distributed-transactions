package com.project.point.facade

import com.project.point.service.PointService
import com.project.point.service.RedisLockService
import com.project.point.service.dto.command.PointUseCancelCommand
import com.project.point.service.dto.command.PointUseCommand
import org.springframework.stereotype.Component

/**
 * 동기(REST) 포인트 사용/취소 흐름을 분산락으로 감싸는 책임만 가진다.
 * 락은 오케스트레이션 바깥에서 잡아야 하므로 PointService 를 락 안에서 호출한다.
 */
@Component
class PointFacade(
    private val pointService: PointService,
    private val redisLockService: RedisLockService,
) {
    fun use(command: PointUseCommand) =
        redisLockService.withLock("point:orchestration:${command.requestId}", command.requestId) {
            pointService.use(command)
        }

    fun cancel(command: PointUseCancelCommand) =
        redisLockService.withLock("point:orchestration:${command.requestId}", command.requestId) {
            pointService.cancel(command)
        }
}
