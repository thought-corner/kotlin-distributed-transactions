package com.project.monolithic.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Tag("unit")
@Tag("domain")
class PointTest {
    @Test
    fun `보유 포인트와 동일한 금액은 경계값으로 사용에 성공한다`() {
        // given: 1000 포인트 보유
        val point = Point(userId = 1, amount = 1000)

        // when & then: balance == amount → 정확히 0 으로 차감되고,
        // 잔액 0 이므로 1 만 더 사용해도 실패해야 한다
        assertDoesNotThrow { point.usePoint(1000) }
        assertThrows<IllegalArgumentException> { point.usePoint(1) }
    }

    @Test
    fun `보유 포인트보다 1 많은 금액은 경계값으로 실패한다`() {
        // given: 1000 포인트 보유
        val point = Point(userId = 1, amount = 1000)

        // when: 잔액 + 1 사용 시도
        val ex = assertThrows<IllegalArgumentException> { point.usePoint(1001) }

        // then: 부족 메시지가 정확히 일치
        assertEquals("포인트가 부족합니다: balance=1000, amount=1001", ex.message)
    }

    @Test
    fun `사용에 실패하면 잔액은 변하지 않는다`() {
        // given: 500 포인트 보유
        val point = Point(userId = 1, amount = 500)

        // when: 501 사용 시도 → 실패
        assertThrows<IllegalArgumentException> { point.usePoint(501) }

        // then: 잔액이 그대로 500 임을 검증 (500 사용은 여전히 성공)
        assertDoesNotThrow { point.usePoint(500) }
    }

    @Test
    fun `여러 번 나누어 사용해도 누적되어 잔액을 초과할 수 없다`() {
        // given: 1000 포인트 보유
        val point = Point(userId = 1, amount = 1000)

        // when: 600 사용 후 남은 잔액은 400
        point.usePoint(600)

        // then: 401 사용은 경계 초과로 실패, 정확히 남은 400 만 성공
        val ex = assertThrows<IllegalArgumentException> { point.usePoint(401) }
        assertEquals("포인트가 부족합니다: balance=400, amount=401", ex.message)
        assertDoesNotThrow { point.usePoint(400) }
    }

    @ParameterizedTest(name = "amount={0} 이면 예외")
    @ValueSource(longs = [0, -1, -100, Long.MIN_VALUE])
    fun `사용 포인트가 0 이하면 예외가 발생한다`(amount: Long) {
        // given: 500 포인트 보유
        val point = Point(userId = 1, amount = 500)

        // when: 0 이하 금액 사용 시도
        val ex = assertThrows<IllegalArgumentException> { point.usePoint(amount) }

        // then: 금액 검증 메시지가 정확히 일치
        assertEquals("사용 포인트는 1 이상이어야 합니다: amount=$amount", ex.message)
    }

    @Test
    fun `잔액이 0 인 포인트는 1 도 사용할 수 없다`() {
        // given: 잔액 0
        val point = Point(userId = 1, amount = 0)

        // when: 1 사용 시도
        val ex = assertThrows<IllegalArgumentException> { point.usePoint(1) }

        // then: 부족 메시지가 정확히 일치
        assertEquals("포인트가 부족합니다: balance=0, amount=1", ex.message)
    }

    @Test
    fun `금액 검증이 잔액 검증보다 먼저 수행된다`() {
        // given: 잔액도 0 이고
        val point = Point(userId = 1, amount = 0)

        // when: 금액도 0 이하 → 두 검증이 모두 위반되는 상황
        val ex = assertThrows<IllegalArgumentException> { point.usePoint(0) }

        // then: 금액(amount) 검증 메시지가 우선이어야 한다
        assertTrue(ex.message!!.startsWith("사용 포인트는"))
    }
}