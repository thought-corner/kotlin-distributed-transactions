package com.project.product.domain

import com.project.product.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * TCC 참여자(Product)의 Try/Confirm/Cancel 의미를 도메인 단위로 검증한다.
 * 핵심: reserve(Try)는 재고를 차감하지 않고 '가예약'만, confirm 에서 비로소 실재고를 차감한다.
 */
class ProductTccTest {
    @Test
    fun `reserve 는 재고를 차감하지 않고 가용 수량만 줄인다`() {
        // given - 재고 100, 단가 100 인 상품
        val product = Product(quantity = 100, price = 100)

        // when - 60 개를 가예약(Try)
        val price = product.reserve(60)

        // then - 예약 금액은 단가*수량, 재고는 줄지 않아 남은 가용(40)까지만 추가 예약 가능
        assertEquals(6000L, price)
        assertThrows<BusinessException> { product.reserve(41) } // 가용 40 초과
        assertEquals(4000L, product.reserve(40)) // 40 까지는 가능 = 재고 보존의 방증
    }

    @Test
    fun `cancel(보상) 은 가예약을 되돌려 다시 예약 가능하게 한다`() {
        // given - 재고 전부를 가예약해 더 이상 예약 불가한 상태
        val product = Product(quantity = 100, price = 100)
        product.reserve(100)
        assertThrows<BusinessException> { product.reserve(1) }

        // when - 가예약을 취소(보상)
        product.cancel(100)

        // then - 다시 예약 가능
        assertEquals(100L, product.reserve(1))
    }

    @Test
    fun `confirm 에서 비로소 실제 재고가 차감된다`() {
        // given - 60 개를 가예약한 상태
        val product = Product(quantity = 100, price = 100)
        product.reserve(60)

        // when - 확정(Confirm)
        product.confirm(60) // 재고 100→40, 가예약 0

        // then - 재고 자체가 40 으로 줄어 41 예약 불가, 40 가능
        assertThrows<BusinessException> { product.reserve(41) }
        assertEquals(4000L, product.reserve(40))
    }

    @Test
    fun `예약하지 않은 수량은 confirm 할 수 없다`() {
        // given - 가예약이 전혀 없는 상품
        val product = Product(quantity = 100, price = 100)

        // when & then - 예약 없는 confirm 은 실패
        assertThrows<BusinessException> { product.confirm(10) }
    }
}
