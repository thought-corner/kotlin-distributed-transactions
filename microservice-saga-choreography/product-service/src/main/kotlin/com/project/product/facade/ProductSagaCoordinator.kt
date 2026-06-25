package com.project.product.facade

import com.project.product.consumer.event.OrderPlacedEvent
import com.project.product.consumer.event.PointUseFailEvent
import com.project.product.producer.QuantityDecreasedFailProducer
import com.project.product.producer.QuantityDecreasedProducer
import com.project.product.producer.event.QuantityDecreasedEvent
import com.project.product.producer.event.QuantityDecreasedFailEvent
import com.project.product.service.ProductService
import com.project.product.service.dto.command.ProductBuyCancelCommand
import com.project.product.service.dto.command.ProductBuyCommand
import org.springframework.stereotype.Component

/**
 * 코레오그래피 사가 오케스트레이션 책임만 가진다. (컨슈머는 역직렬화 + 위임만 담당)
 * - order-placed: 재고 차감 성공 → quantity-decreased, 실패 → 보상(cancel) + quantity-decreased-fail
 * - point-use-fail: 보상(cancel) + quantity-decreased-fail
 *
 * 자신은 @Transactional 이 아니며, @Transactional 인 ProductService 메서드를 호출한다.
 */
@Component
class ProductSagaCoordinator(
    private val productService: ProductService,
    private val quantityDecreasedProducer: QuantityDecreasedProducer,
    private val quantityDecreasedFailProducer: QuantityDecreasedFailProducer,
) {
    /**
     * 사가 1단계: order-placed 를 받아 재고를 차감한다.
     * - 성공 → quantity-decreased (다음 단계인 point 로 진행)
     * - 실패 → 보상(cancel) 후 quantity-decreased-fail (order 를 FAILED 로 되돌림)
     */
    fun handleOrderPlaced(event: OrderPlacedEvent) {
        val requestId = event.orderId.toString()

        try {
            val result =
                productService.buy(
                    ProductBuyCommand(
                        requestId,
                        event.productInfos.map { ProductBuyCommand.ProductInfo(it.productId, it.quantity) },
                    ),
                )

            quantityDecreasedProducer.send(
                QuantityDecreasedEvent(event.orderId, result.totalPrice),
            )
        } catch (e: Exception) {
            productService.cancel(ProductBuyCancelCommand(requestId))
            quantityDecreasedFailProducer.send(QuantityDecreasedFailEvent(event.orderId))
        }
    }

    /**
     * 사가 보상 단계: point 사용이 실패(point-use-fail)하면 앞서 차감한 재고를 복원하고,
     * quantity-decreased-fail 을 발행해 order 를 FAILED 로 되돌린다.
     */
    fun handlePointUseFail(event: PointUseFailEvent) {
        productService.cancel(ProductBuyCancelCommand(event.orderId.toString()))
        quantityDecreasedFailProducer.send(QuantityDecreasedFailEvent(event.orderId))
    }
}
