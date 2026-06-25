package com.project.order.facade

import com.project.order.service.OrderService
import com.project.order.service.RedisLockService
import com.project.order.service.dto.command.PlaceOrderCommand
import org.springframework.stereotype.Component

/**
 * 주문 결제(코레오그래피) 흐름을 분산락으로 감싸는 책임만 가진다.
 * 락은 흐름 바깥에서 잡아야 하므로 OrderService.placeOrder 를 락 안에서 호출한다.
 */
@Component
class OrderFacade(
    private val orderService: OrderService,
    private val redisLockService: RedisLockService,
) {
    fun placeOrder(command: PlaceOrderCommand) {
        redisLockService.withLock("order:${command.orderId}", command.orderId.toString()) {
            orderService.placeOrder(command)
        }
    }
}
