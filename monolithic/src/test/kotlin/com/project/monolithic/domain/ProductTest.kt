package com.project.monolithic.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Tag("unit")
@Tag("domain")
class ProductTest {
    @Nested
    @DisplayName("decreaseStock - 재고 차감")
    inner class DecreaseStock {
        @Test
        fun `재고와 동일한 수량은 경계값으로 차감에 성공한다`() {
            // given: 재고 10 인 상품
            val product = Product(price = 1000, stock = 10)

            // when & then: stock == quantity → 정확히 0 으로 차감되고,
            // 잔여 재고 0 이므로 1 만 더 차감해도 실패해야 한다
            assertDoesNotThrow { product.decreaseStock(10) }
            assertThrows<IllegalArgumentException> { product.decreaseStock(1) }
        }

        @Test
        fun `재고보다 1 많은 수량은 경계값으로 실패한다`() {
            // given: 재고 10 인 상품
            val product = Product(price = 1000, stock = 10)

            // when: 재고 + 1 차감 시도
            val ex = assertThrows<IllegalArgumentException> { product.decreaseStock(11) }

            // then: 부족 메시지가 정확히 일치
            assertEquals("재고가 부족합니다: stock=10, quantity=11", ex.message)
        }

        @Test
        fun `차감에 실패하면 재고는 변하지 않는다`() {
            // given: 재고 5 인 상품
            val product = Product(price = 1000, stock = 5)

            // when: 6 차감 시도 → 실패
            assertThrows<IllegalArgumentException> { product.decreaseStock(6) }

            // then: 재고가 그대로 5 임을 검증 (5 차감은 여전히 성공)
            assertDoesNotThrow { product.decreaseStock(5) }
        }

        @Test
        fun `여러 번 나누어 차감해도 누적되어 재고를 초과할 수 없다`() {
            // given: 재고 10 인 상품
            val product = Product(price = 1000, stock = 10)

            // when: 7 차감 후 남은 재고는 3
            product.decreaseStock(7)

            // then: 4 차감은 경계 초과로 실패, 정확히 남은 3 만 성공
            val ex = assertThrows<IllegalArgumentException> { product.decreaseStock(4) }
            assertEquals("재고가 부족합니다: stock=3, quantity=4", ex.message)
            assertDoesNotThrow { product.decreaseStock(3) }
        }

        @ParameterizedTest(name = "quantity={0} 이면 예외")
        @ValueSource(longs = [0, -1, -100, Long.MIN_VALUE])
        fun `차감 수량이 0 이하면 예외가 발생한다`(quantity: Long) {
            // given: 재고 5 인 상품
            val product = Product(price = 1000, stock = 5)

            // when: 0 이하 수량 차감 시도
            val ex = assertThrows<IllegalArgumentException> { product.decreaseStock(quantity) }

            // then: 수량 검증 메시지가 정확히 일치
            assertEquals("구매 수량은 1 이상이어야 합니다: quantity=$quantity", ex.message)
        }

        @Test
        fun `재고가 0 인 상품은 1 도 차감할 수 없다`() {
            // given: 재고 0 인 상품
            val product = Product(price = 1000, stock = 0)

            // when: 1 차감 시도
            val ex = assertThrows<IllegalArgumentException> { product.decreaseStock(1) }

            // then: 부족 메시지가 정확히 일치
            assertEquals("재고가 부족합니다: stock=0, quantity=1", ex.message)
        }

        @Test
        fun `수량 검증이 재고 검증보다 먼저 수행된다`() {
            // given: 재고도 0 이고
            val product = Product(price = 1000, stock = 0)

            // when: 수량도 0 이하 → 두 검증이 모두 위반되는 상황
            val ex = assertThrows<IllegalArgumentException> { product.decreaseStock(0) }

            // then: 수량(quantity) 검증 메시지가 우선이어야 한다
            assertTrue(ex.message!!.startsWith("구매 수량은"))
        }
    }

    @Nested
    @DisplayName("calculateTotalPrice - 총액 계산")
    inner class CalculateTotalPrice {
        @Test
        fun `가격 곱하기 수량을 반환한다`() {
            // given: 단가 1000 인 상품
            val product = Product(price = 1000, stock = 10)

            // when & then: 1000 * 3 = 3000
            assertEquals(3000, product.calculateTotalPrice(3))
        }

        @Test
        fun `수량이 0 이면 0 을 반환한다`() {
            // given: 단가 1000 인 상품
            val product = Product(price = 1000, stock = 10)

            // when & then: 수량 0 → 0
            assertEquals(0, product.calculateTotalPrice(0))
        }

        @Test
        fun `가격이 0 이면 수량과 무관하게 0 을 반환한다`() {
            // given: 단가 0 인 상품
            val product = Product(price = 0, stock = 10)

            // when & then: 0 * 999 = 0
            assertEquals(0, product.calculateTotalPrice(999))
        }
    }
}