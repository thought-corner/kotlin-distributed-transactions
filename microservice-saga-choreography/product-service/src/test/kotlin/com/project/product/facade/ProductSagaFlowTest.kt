package com.project.product.facade

import com.project.product.consumer.event.OrderPlacedEvent
import com.project.product.consumer.event.PointUseFailEvent
import com.project.product.producer.QuantityDecreasedFailProducer
import com.project.product.producer.QuantityDecreasedProducer
import com.project.product.producer.event.QuantityDecreasedEvent
import com.project.product.producer.event.QuantityDecreasedFailEvent
import com.project.product.repository.ProductRepository
import com.project.product.service.ProductService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

/**
 * 상품 단계 사가 흐름을 실제 서비스 + H2 로 검증한다(통합).
 * Kafka 프로듀서만 Mock(경계)으로 두고, 재고 차감/보상(복원)이 실제 DB 에 반영되는지 확인한다.
 * 실패는 "재고 부족"이라는 실제 조건으로 유발한다.
 */
@SpringBootTest(
    properties = ["spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"],
)
@ActiveProfiles("test")
class ProductSagaFlowTest {
    @Autowired lateinit var productService: ProductService
    @Autowired lateinit var productRepository: ProductRepository

    private val quantityDecreasedProducer = mockk<QuantityDecreasedProducer>(relaxed = true)
    private val quantityDecreasedFailProducer = mockk<QuantityDecreasedFailProducer>(relaxed = true)
    private val coordinator by lazy {
        ProductSagaCoordinator(productService, quantityDecreasedProducer, quantityDecreasedFailProducer)
    }

    private fun seededProduct() = productRepository.findAll().minByOrNull { it.id!! }!!

    @Test
    fun `정상 - 재고가 차감되고 quantity-decreased 가 발행된다`() {
        val product = seededProduct()
        val before = product.quantity
        val orderId = 1001L

        coordinator.handleOrderPlaced(
            OrderPlacedEvent(orderId, listOf(OrderPlacedEvent.ProductInfo(product.id!!, 2L))),
        )

        assertEquals(before - 2, productRepository.findById(product.id!!).get().quantity)
        verify { quantityDecreasedProducer.send(QuantityDecreasedEvent(orderId, product.price * 2)) }
        verify(exactly = 0) { quantityDecreasedFailProducer.send(any()) }
    }

    @Test
    fun `재고 부족 - 보상 후 quantity-decreased-fail 이 발행되고 재고는 그대로다`() {
        val product = seededProduct()
        val before = product.quantity
        val orderId = 1002L

        // 재고보다 많은 수량을 요청해 실제 예외(재고 부족)를 유발한다.
        coordinator.handleOrderPlaced(
            OrderPlacedEvent(orderId, listOf(OrderPlacedEvent.ProductInfo(product.id!!, before + 1))),
        )

        assertEquals(before, productRepository.findById(product.id!!).get().quantity)
        verify { quantityDecreasedFailProducer.send(QuantityDecreasedFailEvent(orderId)) }
        verify(exactly = 0) { quantityDecreasedProducer.send(any()) }
    }

    @Test
    fun `포인트 실패 보상 - 차감했던 재고가 원복되고 quantity-decreased-fail 이 발행된다`() {
        val product = seededProduct()
        val before = product.quantity
        val orderId = 1003L

        // 1) 먼저 정상 차감
        coordinator.handleOrderPlaced(
            OrderPlacedEvent(orderId, listOf(OrderPlacedEvent.ProductInfo(product.id!!, 5L))),
        )
        assertEquals(before - 5, productRepository.findById(product.id!!).get().quantity)

        // 2) 포인트 사용 실패 수신 → 보상(재고 복원)
        coordinator.handlePointUseFail(PointUseFailEvent(orderId))

        assertEquals(before, productRepository.findById(product.id!!).get().quantity)
        verify { quantityDecreasedFailProducer.send(QuantityDecreasedFailEvent(orderId)) }
    }

    @Test
    fun `멱등성 - 같은 order-placed 를 두 번 받아도 재고는 한 번만 차감된다`() {
        val product = seededProduct()
        val before = product.quantity
        val orderId = 1004L
        val event = OrderPlacedEvent(orderId, listOf(OrderPlacedEvent.ProductInfo(product.id!!, 3L)))

        coordinator.handleOrderPlaced(event)
        coordinator.handleOrderPlaced(event) // 중복 전달(재처리)

        assertEquals(before - 3, productRepository.findById(product.id!!).get().quantity)
    }
}
