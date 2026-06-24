package com.project.point.domain

import com.project.point.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * TCC 참여자(Point)의 Try/Confirm/Cancel 의미를 도메인 단위로 검증한다.
 * reserve(Try)는 잔액을 차감하지 않고 '가예약'만, confirm 에서 실제 잔액을 차감한다.
 */
class PointTccTest {
    @Test
    fun `confirm 에서 비로소 실제 잔액이 차감된다`() {
        // given - 잔액 10000 에서 3000 을 가예약(잔액은 아직 10000)
        val point = Point(userId = 1, amount = 10000)
        point.reserve(3000)

        // when - 확정(Confirm)
        point.confirm(3000) // 잔액 10000→7000, 가예약 0

        // then - 잔액이 7000 으로 줄어 7001 확정은 불가
        point.reserve(3000)
        assertThrows<BusinessException> { point.confirm(7001) }
    }

    @Test
    fun `cancel(보상) 은 가예약을 되돌린다`() {
        // given - 4000 을 가예약한 상태
        val point = Point(userId = 1, amount = 10000)
        point.reserve(4000)

        // when - 가예약 취소(보상)
        point.cancel(4000)

        // then - 되돌렸으므로 다시 예약 가능(예외 없음)
        point.reserve(4000)
    }

    @Test
    fun `reserve 는 잔액 한도까지 누적 예약 가능하고 초과는 거부한다`() {
        // given - 잔액 10000
        val point = Point(userId = 1, amount = 10000)

        // when - 6000 가예약(잔액 내이면 절반 초과도 정상)
        point.reserve(6000)

        // then - 남은 가용(4000) 초과는 거부, 잔액 전부까지는 예약 가능
        assertThrows<BusinessException> { point.reserve(4001) }
        point.reserve(4000)
    }
}
