package com.project.monolithic.service

import com.project.monolithic.domain.Order
import com.project.monolithic.domain.OrderItem
import com.project.monolithic.exception.BusinessException
import com.project.monolithic.repository.OrderItemRepository
import com.project.monolithic.repository.OrderRepository
import com.project.monolithic.service.dto.CreateOrderCommand
import com.project.monolithic.service.dto.PlaceOrderCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.Optional

@Tag("unit")
@Tag("service")
class OrderServiceTest {
    private val orderRepository = mockk<OrderRepository>()
    private val orderItemRepository = mockk<OrderItemRepository>(relaxed = true)
    private val pointService = mockk<PointService>(relaxed = true)
    private val productService = mockk<ProductService>()
    private val orderService = OrderService(orderRepository, orderItemRepository, pointService, productService)

    @Test
    fun `createOrder 는 각 항목을 주문 id 와 함께 OrderItem 으로 저장한다`() {
        // given: 저장 시 id=100 인 주문이 반환되고, 저장될 항목을 캡처한다
        every { orderRepository.save(any()) } returns Order(id = 100)
        val savedItems = slot<List<OrderItem>>()
        every { orderItemRepository.saveAll(capture(savedItems)) } returns mutableListOf()
        val command = CreateOrderCommand(
            listOf(
                CreateOrderCommand.OrderItem(productId = 10, quantity = 2),
                CreateOrderCommand.OrderItem(productId = 11, quantity = 3),
            ),
        )

        // when: 주문 생성
        val result = orderService.createOrder(command)

        // then: 반환 id 와, 각 항목이 주문 id(100) 와 함께 올바르게 매핑되어 저장됨
        assertEquals(100, result.orderId)
        val items = savedItems.captured
        assertEquals(2, items.size)
        assertEquals(100, items[0].orderId)
        assertEquals(10, items[0].productId)
        assertEquals(2, items[0].quantity)
        assertEquals(100, items[1].orderId)
        assertEquals(11, items[1].productId)
        assertEquals(3, items[1].quantity)
    }

    @Test
    fun `createOrder 는 빈 주문 항목도 처리하고 주문 id 를 반환한다`() {
        // given: id=7 주문 저장
        every { orderRepository.save(any()) } returns Order(id = 7)
        every { orderItemRepository.saveAll(any<List<OrderItem>>()) } returns mutableListOf()

        // when: 빈 항목으로 주문 생성
        val result = orderService.createOrder(CreateOrderCommand(emptyList()))

        // then: id 반환, 빈 리스트로 saveAll 호출
        assertEquals(7, result.orderId)
        verify(exactly = 1) { orderItemRepository.saveAll(emptyList<OrderItem>()) }
    }

    @Test
    fun `placeOrder 는 주문이 없으면 BusinessException 을 던지고 후속 작업을 하지 않는다`() {
        // given: 주문 99 가 존재하지 않음
        every { orderRepository.findById(99) } returns Optional.empty()

        // when: 주문 처리 시도
        val ex = assertThrows<BusinessException> { orderService.placeOrder(PlaceOrderCommand(99)) }

        // then: 예외 메시지 일치 + 구매/포인트 사용은 전혀 일어나지 않음
        assertEquals("주문정보가 존재하지 않습니다.", ex.message)
        verify(exactly = 0) { orderItemRepository.findAllByOrderId(any()) }
        verify(exactly = 0) { productService.buyProduct(any(), any()) }
        verify(exactly = 0) { pointService.usePoint(any(), any()) }
    }

    @Test
    fun `placeOrder 는 이미 완료된 주문이면 결제 없이 즉시 종료한다`() {
        // given: 이미 완료(COMPLETED)된 주문
        val order = Order(id = 1).apply { complete() }
        every { orderRepository.findById(1) } returns Optional.of(order)

        // when: 주문 처리 시도
        orderService.placeOrder(PlaceOrderCommand(1))

        // then: 단축 경로 - 항목 조회/구매/포인트 사용이 전혀 일어나지 않음
        verify(exactly = 0) { orderItemRepository.findAllByOrderId(any()) }
        verify(exactly = 0) { productService.buyProduct(any(), any()) }
        verify(exactly = 0) { pointService.usePoint(any(), any()) }
    }

    @Test
    fun `placeOrder 는 모든 항목 결제금액의 합계로 포인트를 사용하고 주문을 완료한다`() {
        // 주의: placeOrder 끝에 Thread.sleep(3000) 이 있어 약 3초 소요됨 (학습용 원본 유지)
        // given: 미완료 주문 + 항목 2개, 각 항목의 구매 결과 금액
        val order = Order(id = 1)
        every { orderRepository.findById(1) } returns Optional.of(order)
        every { orderItemRepository.findAllByOrderId(1) } returns listOf(
            OrderItem(orderId = 1, productId = 10, quantity = 2),
            OrderItem(orderId = 1, productId = 11, quantity = 1),
        )
        every { productService.buyProduct(10, 2) } returns 2000
        every { productService.buyProduct(11, 1) } returns 500

        // when: 주문 처리
        orderService.placeOrder(PlaceOrderCommand(1))

        // then: 각 항목 구매 + 합계 2500 으로 포인트 사용(userId 는 원본대로 1) + 주문 완료
        verify(exactly = 1) { productService.buyProduct(10, 2) }
        verify(exactly = 1) { productService.buyProduct(11, 1) }
        verify(exactly = 1) { pointService.usePoint(1, 2500) }
        assertTrue(order.isCompleted())
    }

    @Test
    fun `동일 주문을 순차로 두 번 처리해도 결제는 정확히 1번만 일어난다`() {
        // given: 같은 주문 인스턴스가 매 조회 시 반환됨 (재시도/중복요청 상황)
        val order = Order(id = 1)
        every { orderRepository.findById(1) } returns Optional.of(order)
        every { orderItemRepository.findAllByOrderId(1) } returns listOf(
            OrderItem(orderId = 1, productId = 10, quantity = 2),
        )
        every { productService.buyProduct(10, 2) } returns 2000

        // when: 같은 주문을 두 번 처리
        orderService.placeOrder(PlaceOrderCommand(1))
        orderService.placeOrder(PlaceOrderCommand(1))

        // then: 첫 호출에서 완료되고 두 번째는 isCompleted() 가드로 단축 종료 → 결제는 1회뿐
        verify(exactly = 1) { productService.buyProduct(10, 2) }
        verify(exactly = 1) { pointService.usePoint(1, 2000) }
        assertTrue(order.isCompleted())
    }
}