package com.project.order.facade

import com.project.order.service.RedisLockService
import com.project.order.service.dto.command.PlaceOrderCommand
import org.springframework.stereotype.Component

/**
 * 주문 결제(TCC) 흐름을 분산락으로 감싸는 책임만 가진다.
 * 락은 오케스트레이션 바깥에서 잡아야 하므로 OrderCoordinator 를 락 안에서 호출한다.
 */
@Component
class OrderFacade(
    private val orderCoordinator: OrderCoordinator,
    private val redisLockService: RedisLockService,
) {
    fun placeOrder(command: PlaceOrderCommand) {
        redisLockService.withLock("order:${command.orderId}", command.orderId.toString()) {
            orderCoordinator.placeOrder(command)
        }
    }
}
