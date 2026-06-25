package com.project.product.facade

import com.project.product.consumer.event.OrderPlacedEvent
import com.project.product.consumer.event.PointUseFailEvent
import com.project.product.exception.BusinessException
import com.project.product.producer.QuantityDecreasedFailProducer
import com.project.product.producer.QuantityDecreasedProducer
import com.project.product.producer.event.QuantityDecreasedEvent
import com.project.product.producer.event.QuantityDecreasedFailEvent
import com.project.product.service.ProductService
import com.project.product.service.dto.command.ProductBuyCancelCommand
import com.project.product.service.dto.result.ProductBuyResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

/**
 * 사가 1단계(product) 오케스트레이션을 검증한다(참여 서비스는 Mock).
 * 핵심: 재고 차감이 실패하면 보상(cancel)하고 quantity-decreased-fail 을 발행하는가.
 */
class ProductSagaCoordinatorTest {
    private val productService = mockk<ProductService>(relaxed = true)
    private val quantityDecreasedProducer = mockk<QuantityDecreasedProducer>(relaxed = true)
    private val quantityDecreasedFailProducer = mockk<QuantityDecreasedFailProducer>(relaxed = true)
    private val coordinator =
        ProductSagaCoordinator(productService, quantityDecreasedProducer, quantityDecreasedFailProducer)

    @Test
    fun `order-placed 성공 - 재고 차감 후 quantity-decreased 를 발행하고 보상하지 않는다`() {
        // given - 재고 차감 성공(총액 2000)
        every { productService.buy(any()) } returns ProductBuyResult(2000L)
        val event = OrderPlacedEvent(1L, listOf(OrderPlacedEvent.ProductInfo(10L, 2L)))

        // when
        coordinator.handleOrderPlaced(event)

        // then - 성공 이벤트만 발행, 보상은 호출되지 않음
        verify { quantityDecreasedProducer.send(QuantityDecreasedEvent(1L, 2000L)) }
        verify(exactly = 0) { productService.cancel(any()) }
        verify(exactly = 0) { quantityDecreasedFailProducer.send(any()) }
    }

    @Test
    fun `order-placed 실패 - 보상(cancel) 후 quantity-decreased-fail 을 발행하고 성공 이벤트는 발행하지 않는다`() {
        // given - 재고 차감이 실패
        every { productService.buy(any()) } throws BusinessException("재고가 부족합니다.")
        val event = OrderPlacedEvent(1L, listOf(OrderPlacedEvent.ProductInfo(10L, 2L)))

        // when - 코디네이터가 예외를 삼키고 보상으로 전환한다
        coordinator.handleOrderPlaced(event)

        // then - 보상(cancel) → 실패 이벤트 순서, 성공 이벤트는 없음
        verifyOrder {
            productService.cancel(ProductBuyCancelCommand("1"))
            quantityDecreasedFailProducer.send(QuantityDecreasedFailEvent(1L))
        }
        verify(exactly = 0) { quantityDecreasedProducer.send(any()) }
    }

    @Test
    fun `point-use-fail 보상 - 차감했던 재고를 복원(cancel)하고 quantity-decreased-fail 을 발행한다`() {
        // when - 포인트 사용 실패로 인한 보상 트리거 수신
        coordinator.handlePointUseFail(PointUseFailEvent(1L))

        // then
        verifyOrder {
            productService.cancel(ProductBuyCancelCommand("1"))
            quantityDecreasedFailProducer.send(QuantityDecreasedFailEvent(1L))
        }
    }
}
