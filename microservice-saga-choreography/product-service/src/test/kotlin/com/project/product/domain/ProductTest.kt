package com.project.product.domain

import com.project.product.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * 재고 도메인 규칙과 보상(cancel)의 상태 복원을 검증한다.
 */
class ProductTest {
    @Test
    fun `구매 - 재고가 수량만큼 차감된다`() {
        val product = Product(quantity = 100L, price = 200L)

        product.buy(30L)

        assertEquals(70L, product.quantity)
    }

    @Test
    fun `재고 부족 - BusinessException 을 던지고 재고는 그대로다`() {
        val product = Product(quantity = 10L, price = 200L)

        assertThrows<BusinessException> { product.buy(20L) }
        assertEquals(10L, product.quantity)
    }

    @Test
    fun `보상(cancel) - 차감했던 재고가 원래대로 복원된다`() {
        val product = Product(quantity = 100L, price = 200L)
        product.buy(30L)

        product.cancel(30L)

        assertEquals(100L, product.quantity)
    }

    @Test
    fun `가격 계산 - 단가 곱하기 수량`() {
        val product = Product(quantity = 100L, price = 200L)

        assertEquals(600L, product.calculatePrice(3L))
    }
}
