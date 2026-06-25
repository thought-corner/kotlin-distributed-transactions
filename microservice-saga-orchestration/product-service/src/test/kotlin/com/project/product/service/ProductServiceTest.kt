package com.project.product.service

import com.project.product.exception.BusinessException
import com.project.product.repository.ProductRepository
import com.project.product.service.dto.command.ProductBuyCancelCommand
import com.project.product.service.dto.command.ProductBuyCommand
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

/**
 * 참여 서비스(상품)의 실제 비즈니스/보상 동작을 H2 로 검증한다.
 * 실패는 "재고 부족"이라는 실제 조건으로 강제 유발한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {
    @Autowired lateinit var productService: ProductService
    @Autowired lateinit var productRepository: ProductRepository

    private fun seededProduct() = productRepository.findAll().minByOrNull { it.id!! }!!
    private fun buyCmd(requestId: String, productId: Long, qty: Long) =
        ProductBuyCommand(requestId, listOf(ProductBuyCommand.ProductInfo(productId, qty)))

    @Test
    fun `정상 구매 - 재고가 차감되고 총액을 반환한다`() {
        val product = seededProduct()
        val before = product.quantity

        val result = productService.buy(buyCmd("req-1", product.id!!, 2L))

        assertEquals(product.price * 2, result.totalPrice)
        assertEquals(before - 2, productRepository.findById(product.id!!).get().quantity)
    }

    @Test
    fun `재고 부족 - BusinessException 을 던지고 재고는 그대로다`() {
        val product = seededProduct()
        val before = product.quantity

        assertThrows<BusinessException> { productService.buy(buyCmd("req-2", product.id!!, before + 1)) }

        assertEquals(before, productRepository.findById(product.id!!).get().quantity)
    }

    @Test
    fun `보상(cancel) - 구매했던 재고가 원래대로 복원된다`() {
        val product = seededProduct()
        val before = product.quantity
        productService.buy(buyCmd("req-3", product.id!!, 5L))
        assertEquals(before - 5, productRepository.findById(product.id!!).get().quantity)

        productService.cancel(ProductBuyCancelCommand("req-3"))

        assertEquals(before, productRepository.findById(product.id!!).get().quantity)
    }

    @Test
    fun `구매 멱등성 - 같은 requestId 로 두 번 구매해도 재고는 한 번만 차감된다`() {
        val product = seededProduct()
        val before = product.quantity
        val cmd = buyCmd("req-4", product.id!!, 3L)

        productService.buy(cmd)
        productService.buy(cmd) // 중복 호출

        assertEquals(before - 3, productRepository.findById(product.id!!).get().quantity)
    }

    @Test
    fun `보상 멱등성 - 같은 requestId 로 두 번 취소해도 재고는 한 번만 복원된다`() {
        val product = seededProduct()
        val before = product.quantity
        productService.buy(buyCmd("req-5", product.id!!, 4L))

        productService.cancel(ProductBuyCancelCommand("req-5"))
        productService.cancel(ProductBuyCancelCommand("req-5")) // 중복 보상

        assertEquals(before, productRepository.findById(product.id!!).get().quantity)
    }
}
