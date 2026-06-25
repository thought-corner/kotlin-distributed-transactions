package com.project.product.facade

import com.project.product.service.ProductService
import com.project.product.service.RedisLockService
import com.project.product.service.dto.command.ProductBuyCancelCommand
import com.project.product.service.dto.command.ProductBuyCommand
import com.project.product.service.dto.result.ProductBuyCancelResult
import com.project.product.service.dto.result.ProductBuyResult
import org.springframework.stereotype.Component

/**
 * 동기(REST) 흐름을 분산락으로 감싸는 책임만 가진다.
 * 락은 오케스트레이션/컨트롤러 바깥에서 잡아야 하므로 ProductService 를 락 안에서 호출한다.
 */
@Component
class ProductFacade(
    private val productService: ProductService,
    private val redisLockService: RedisLockService,
) {
    fun buy(command: ProductBuyCommand): ProductBuyResult =
        redisLockService.withLock("product:orchestration:${command.requestId}", command.requestId) {
            productService.buy(command)
        }

    fun cancel(command: ProductBuyCancelCommand): ProductBuyCancelResult =
        redisLockService.withLock("product:orchestration:${command.requestId}", command.requestId) {
            productService.cancel(command)
        }
}
