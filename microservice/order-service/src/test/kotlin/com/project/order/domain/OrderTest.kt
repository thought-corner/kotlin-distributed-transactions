package com.project.order.domain

import com.project.order.domain.Order.OrderStatus
import com.project.order.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 주문 상태머신이 TCC 단계(예약→확정 / 예약→취소 / 확정실패→pending)를 정확히 강제하는지 검증한다.
 */
class OrderTest {
    @Test
    fun `정상 흐름 - CREATED 에서 reserve 후 confirm`() {
        // given - 막 생성된 주문
        val order = Order()
        assertEquals(OrderStatus.CREATED, order.status)

        // when - 예약(Try)
        order.reserve()

        // then - RESERVED
        assertEquals(OrderStatus.RESERVED, order.status)

        // when - 확정(Confirm)
        order.confirm()

        // then - CONFIRMED
        assertEquals(OrderStatus.CONFIRMED, order.status)
    }

    @Test
    fun `보상 흐름 - RESERVED 에서 cancel`() {
        // given - 예약된 주문
        val order = Order()
        order.reserve()

        // when - 취소(Cancel 보상)
        order.cancel()

        // then - CANCELLED
        assertEquals(OrderStatus.CANCELLED, order.status)
    }

    @Test
    fun `확정 실패 흐름 - RESERVED 에서 pending 후 재confirm 가능`() {
        // given - 예약된 주문
        val order = Order()
        order.reserve()

        // when - 확정 실패로 pending 전이
        order.pending()

        // then - PENDING
        assertEquals(OrderStatus.PENDING, order.status)

        // when - PENDING 에서 재확정
        order.confirm()

        // then - CONFIRMED (멱등 재시도로 끝까지 확정 가능)
        assertEquals(OrderStatus.CONFIRMED, order.status)
    }

    @Test
    fun `CREATED 에서 곧바로 confirm 할 수 없다`() {
        // given - 예약 전(CREATED) 주문
        val order = Order()

        // when & then - 예약 없이 확정 불가
        assertThrows<BusinessException> { order.confirm() }
    }

    @Test
    fun `복구 흐름 - PENDING 에서 재confirm 실패 시 pending 재진입은 멱등하다`() {
        // given - 확정 실패로 PENDING 인 주문
        val order = Order()
        order.reserve()
        order.pending()

        // when - 스케줄러 재confirm 도 실패해 다시 pending 호출
        order.pending()

        // then - 예외 없이 PENDING 유지(멱등)
        assertEquals(OrderStatus.PENDING, order.status)
    }

    @Test
    fun `어드민 강제취소 - PENDING 에서 cancel 가능`() {
        // given - PENDING 인 주문
        val order = Order()
        order.reserve()
        order.pending()

        // when - 어드민 강제 취소(보상)
        order.cancel()

        // then - CANCELLED
        assertEquals(OrderStatus.CANCELLED, order.status)
    }

    @Test
    fun `복구 시도 횟수 누적`() {
        // given - 주문
        val order = Order()

        // when - 스케줄러가 두 번 재구동 시도
        order.recordRecoveryAttempt()
        order.recordRecoveryAttempt()

        // then - 시도 횟수 누적
        assertEquals(2, order.recoveryAttempts)
    }

    @Test
    fun `CANCELLED 는 종료 상태 - 재예약 불가`() {
        // given - 취소된 주문
        val order = Order()
        order.reserve()
        order.cancel()

        // when & then - 종료 상태에서 재예약 불가
        assertThrows<BusinessException> { order.reserve() }
    }
}
