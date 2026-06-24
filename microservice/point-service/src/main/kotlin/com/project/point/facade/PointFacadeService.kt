package com.project.point.facade

import com.project.point.exception.BusinessException
import com.project.point.service.PointService
import com.project.point.service.RedisLockService
import com.project.point.service.dto.command.PointReserveCancelCommand
import com.project.point.service.dto.command.PointReserveCommand
import com.project.point.service.dto.command.PointReserveConfirmCommand
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component

/**
 * 예약/확정/취소를 분산락 + 낙관적 락 재시도로 감싸 안전하게 실행하는 책임을 가진다.
 * 실제 도메인 처리(@Transactional)는 PointService 가, 락 생명주기는 RedisLockService 가 담당한다.
 */
@Component
class PointFacadeService(
    private val pointService: PointService,
    private val redisLockService: RedisLockService,
) {
    fun tryReserve(command: PointReserveCommand) = withLockAndRetry(command.requestId) { pointService.tryReserve(command) }

    fun confirmReserve(command: PointReserveConfirmCommand) = withLockAndRetry(command.requestId) { pointService.confirmReserve(command) }

    fun cancelReserve(command: PointReserveCancelCommand) = withLockAndRetry(command.requestId) { pointService.cancelReserve(command) }

    private fun <T> withLockAndRetry(
        requestId: String,
        block: () -> T,
    ): T =
        redisLockService.withLock("point:$requestId", requestId) {
            var tryCount = 0
            while (tryCount < 3) {
                try {
                    return@withLock block()
                } catch (e: ObjectOptimisticLockingFailureException) {
                    tryCount++
                }
            }
            throw BusinessException("예약에 실패하였습니다.")
        }
}
