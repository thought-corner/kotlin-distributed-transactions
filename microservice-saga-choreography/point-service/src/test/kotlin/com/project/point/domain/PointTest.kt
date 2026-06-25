package com.project.point.domain

import com.project.point.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * 포인트 도메인 규칙과 보상(cancel)의 잔액 복원을 검증한다.
 */
class PointTest {
    @Test
    fun `사용 - 잔액이 금액만큼 차감된다`() {
        val point = Point(userId = 1L, amount = 10000L)

        point.use(2000L)

        assertEquals(8000L, point.amount)
    }

    @Test
    fun `잔액 부족 - BusinessException 을 던지고 잔액은 그대로다`() {
        val point = Point(userId = 1L, amount = 1000L)

        assertThrows<BusinessException> { point.use(2000L) }
        assertEquals(1000L, point.amount)
    }

    @Test
    fun `보상(cancel) - 사용했던 포인트가 원래대로 복원된다`() {
        val point = Point(userId = 1L, amount = 10000L)
        point.use(2000L)

        point.cancel(2000L)

        assertEquals(10000L, point.amount)
    }
}
