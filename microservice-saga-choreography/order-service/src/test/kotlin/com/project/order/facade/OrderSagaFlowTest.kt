package com.project.order.facade

import com.project.order.domain.Order
import com.project.order.producer.OrderPlacedProducer
import com.project.order.repository.OrderRepository
import com.project.order.service.OrderService
import com.project.order.service.dto.command.CreateOrderCommand
import com.project.order.service.dto.command.PlaceOrderCommand
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

/**
 * 주문 단계 사가 흐름을 실제 서비스 + H2 로 검증한다(통합).
 * 사가 시작(order-placed 발행)과 성공/실패 종착(상태 전이)을 확인한다.
 * Kafka 발행 경계인 OrderPlacedProducer 만 Mock 으로 대체한다.
 */
@SpringBootTest(
    properties = ["spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"],
)
@ActiveProfiles("test")
@Import(OrderSagaFlowTest.MockProducerConfig::class)
class OrderSagaFlowTest {
    @TestConfiguration
    class MockProducerConfig {
        @Bean
        @Primary
        fun orderPlacedProducerMock(): OrderPlacedProducer = mockk(relaxed = true)
    }

    @Autowired lateinit var orderService: OrderService
    @Autowired lateinit var orderRepository: OrderRepository
    @Autowired lateinit var orderPlacedProducer: OrderPlacedProducer // @Primary mock

    private fun createOrder(): Long =
        orderService.createOrder(
            CreateOrderCommand(listOf(CreateOrderCommand.OrderItem(productId = 1L, quantity = 2L))),
        ).orderId

    @Test
    fun `주문 생성 - CREATED 상태로 저장된다`() {
        val orderId = createOrder()

        assertEquals(Order.OrderStatus.CREATED, orderService.getStatus(orderId))
    }

    @Test
    fun `사가 시작 - placeOrder 시 REQUESTED 로 전이하고 order-placed 가 발행된다`() {
        val orderId = createOrder()

        orderService.placeOrder(PlaceOrderCommand(orderId))

        assertEquals(Order.OrderStatus.REQUESTED, orderService.getStatus(orderId))
        // afterCommit 시점에 주문 항목을 담아 발행된다.
        verify {
            orderPlacedProducer.send(
                match { it.orderId == orderId && it.productInfos.size == 1 && it.productInfos[0].productId == 1L },
            )
        }
    }

    @Test
    fun `성공 종착 - point-used 수신(complete)으로 COMPLETED 가 된다`() {
        val orderId = createOrder()
        orderService.placeOrder(PlaceOrderCommand(orderId))

        orderService.complete(orderId)

        assertEquals(Order.OrderStatus.COMPLETED, orderService.getStatus(orderId))
    }

    @Test
    fun `실패 종착 - quantity-decreased-fail 수신(fail)으로 FAILED 가 된다`() {
        val orderId = createOrder()
        orderService.placeOrder(PlaceOrderCommand(orderId)) // REQUESTED

        orderService.fail(orderId)

        assertEquals(Order.OrderStatus.FAILED, orderService.getStatus(orderId))
    }
}
