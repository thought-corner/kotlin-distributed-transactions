package com.project.monolithic.facade

import com.project.monolithic.exception.BusinessException
import com.project.monolithic.service.OrderService
import com.project.monolithic.service.RedisLockService
import com.project.monolithic.service.dto.PlaceOrderCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
@Tag("facade")
class OrderFacadeTest {
    private val orderService = mockk<OrderService>(relaxed = true)
    private val redisLockService = mockk<RedisLockService>(relaxed = true)
    private val orderFacade = OrderFacade(orderService, redisLockService)

    @Test
    fun `락 획득에 성공하면 획득-주문처리-해제 순서로 실행된다`() {
        // given: 락 획득 성공
        every { redisLockService.tryLock(any(), any()) } returns true

        // when: 주문 처리
        orderFacade.placeOrder(PlaceOrderCommand(1))

        // then: 락 획득 → 주문 처리 → 락 해제 순서가 정확히 지켜짐
        verifyOrder {
            redisLockService.tryLock("order:monolithic:1", "1")
            orderService.placeOrder(PlaceOrderCommand(1))
            redisLockService.releaseLock("order:monolithic:1")
        }
    }

    @Test
    fun `락 획득에 실패하면 BusinessException 을 던지고 주문처리도 해제도 하지 않는다`() {
        // given: 락 획득 실패
        every { redisLockService.tryLock(any(), any()) } returns false

        // when: 주문 처리 시도
        val ex = assertThrows<BusinessException> { orderFacade.placeOrder(PlaceOrderCommand(1)) }

        // then: 예외 발생 + 주문 처리 안 함 + 해제도 안 함(남의 락 삭제 방지)
        assertEquals("락획득에 실패하였습니다.", ex.message)
        verify(exactly = 0) { orderService.placeOrder(any()) }
        verify(exactly = 0) { redisLockService.releaseLock(any()) }
    }

    @Test
    fun `주문 처리 중 예외가 발생해도 락은 반드시 해제된다`() {
        // given: 락은 획득했으나 주문 처리가 실패하는 상황
        every { redisLockService.tryLock(any(), any()) } returns true
        every { orderService.placeOrder(any()) } throws RuntimeException("결제 실패")

        // when: 주문 처리 시도
        val ex = assertThrows<RuntimeException> { orderFacade.placeOrder(PlaceOrderCommand(1)) }

        // then: 예외가 전파되더라도 finally 로 락 해제는 정확히 1회 일어남
        assertEquals("결제 실패", ex.message)
        verify(exactly = 1) { redisLockService.releaseLock("order:monolithic:1") }
    }
}