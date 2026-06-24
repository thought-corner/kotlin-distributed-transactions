package com.project.product.facade

import com.project.product.exception.BusinessException
import com.project.product.service.ProductService
import com.project.product.service.RedisLockService
import com.project.product.service.dto.command.ProductReserveCancelCommand
import com.project.product.service.dto.command.ProductReserveCommand
import com.project.product.service.dto.command.ProductReserveConfirmCommand
import com.project.product.service.dto.result.ProductReserveResult
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component

/**
 * 예약/확정/취소를 분산락 + 낙관적 락 재시도로 감싸 안전하게 실행하는 책임을 가진다.
 * 실제 도메인 처리(@Transactional)는 ProductService 가, 락 생명주기는 RedisLockService 가 담당한다.
 */
@Component
class ProductFacadeService(
    private val productService: ProductService,
    private val redisLockService: RedisLockService,
) {
    fun tryReserve(command: ProductReserveCommand): ProductReserveResult =
        withLockAndRetry(command.requestId) { productService.tryReserve(command) }

    fun confirmReserve(command: ProductReserveConfirmCommand) =
        withLockAndRetry(command.requestId) { productService.confirmReserve(command) }

    fun cancelReserve(command: ProductReserveCancelCommand) = withLockAndRetry(command.requestId) { productService.cancelReserve(command) }

    private fun <T> withLockAndRetry(
        requestId: String,
        block: () -> T,
    ): T =
        redisLockService.withLock("product:$requestId", requestId) {
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
