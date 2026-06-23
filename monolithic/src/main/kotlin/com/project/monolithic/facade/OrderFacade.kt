package com.project.monolithic.facade

import com.project.monolithic.exception.BusinessException
import com.project.monolithic.service.OrderService
import com.project.monolithic.service.RedisLockService
import com.project.monolithic.service.dto.PlaceOrderCommand
import org.springframework.stereotype.Component

/**
 * 주문 처리 시 분산락 획득/해제 흐름을 책임진다.
 * 락은 트랜잭션 바깥에서 감싸야 하므로, @Transactional 인 OrderService 를 락 안에서 호출한다.
 */
@Component
class OrderFacade(
    private val orderService: OrderService,
    private val redisLockService: RedisLockService,
) {
    fun placeOrder(command: PlaceOrderCommand) {
        val key = "order:monolithic:${command.orderId}"
        val acquiredLock = redisLockService.tryLock(key, command.orderId.toString())

        if (!acquiredLock) {
            throw BusinessException("락획득에 실패하였습니다.")
        }

        try {
            orderService.placeOrder(command)
        } finally {
            redisLockService.releaseLock(key)
        }
    }
}