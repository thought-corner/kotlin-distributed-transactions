package com.project.monolithic.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
@Tag("domain")
class OrderTest {
    @Test
    fun `생성된 주문의 초기 상태는 CREATED 이며 완료 상태가 아니다`() {
        // given & when: 주문 생성
        val order = Order()

        // then: 초기 상태는 CREATED, 완료 아님
        assertEquals(Order.OrderStatus.CREATED, order.status)
        assertFalse(order.isCompleted())
    }

    @Test
    fun `complete 를 호출하면 COMPLETED 상태가 되고 완료로 판정된다`() {
        // given: 생성된 주문
        val order = Order()

        // when: 완료 처리
        order.complete()

        // then: COMPLETED 상태이며 완료로 판정
        assertEquals(Order.OrderStatus.COMPLETED, order.status)
        assertTrue(order.isCompleted())
    }

    @Test
    fun `이미 완료된 주문에 complete 를 다시 호출해도 멱등하게 완료 상태를 유지한다`() {
        // given: 이미 완료된 주문
        val order = Order()
        order.complete()

        // when: complete 재호출
        order.complete()

        // then: 여전히 완료 상태 (멱등)
        assertTrue(order.isCompleted())
    }
}