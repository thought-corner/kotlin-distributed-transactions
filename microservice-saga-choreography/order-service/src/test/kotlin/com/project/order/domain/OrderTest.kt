package com.project.order.domain

import com.project.order.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * 주문 상태 전이 규칙을 검증한다. (사가의 시작/성공 종착/실패 종착)
 */
class OrderTest {
    @Test
    fun `생성 직후 상태는 CREATED 다`() {
        assertEquals(Order.OrderStatus.CREATED, Order().status)
    }

    @Test
    fun `request - CREATED 에서 REQUESTED 로 전이한다`() {
        val order = Order()

        order.request()

        assertEquals(Order.OrderStatus.REQUESTED, order.status)
    }

    @Test
    fun `complete - 성공 종착으로 COMPLETED 가 된다`() {
        val order = Order()
        order.request()

        order.complete()

        assertEquals(Order.OrderStatus.COMPLETED, order.status)
    }

    @Test
    fun `fail - REQUESTED 에서만 FAILED 로 전이한다`() {
        val order = Order()
        order.request()

        order.fail()

        assertEquals(Order.OrderStatus.FAILED, order.status)
    }

    @Test
    fun `fail - REQUESTED 가 아니면 BusinessException 을 던진다`() {
        // CREATED 상태에서 곧장 fail 시도
        assertThrows<BusinessException> { Order().fail() }
    }
}
