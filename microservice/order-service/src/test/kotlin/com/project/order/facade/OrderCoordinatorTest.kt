package com.project.order.facade

import com.project.order.client.PointApiClient
import com.project.order.client.ProductApiClient
import com.project.order.client.dto.ProductReserveApiResponse
import com.project.order.service.OrderService
import com.project.order.service.dto.OrderDto
import com.project.order.service.dto.command.PlaceOrderCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * TCC 코디네이터의 오케스트레이션을 검증한다(참여자 서비스는 Mock).
 * - 해피패스: Try(reserve) → Confirm 을 정확한 순서로 호출하는가
 * - Try 실패: 주문/상품/포인트를 보상(cancel)하고 Confirm 으로 넘어가지 않는가
 */
class OrderCoordinatorTest {
    private val orderService = mockk<OrderService>(relaxed = true)
    private val productApiClient = mockk<ProductApiClient>(relaxed = true)
    private val pointApiClient = mockk<PointApiClient>(relaxed = true)
    private val coordinator = OrderCoordinator(orderService, productApiClient, pointApiClient)

    @Test
    fun `해피패스 - Try(reserve) 후 Confirm 을 순서대로 수행한다`() {
        // given - 주문 조회/상품 예약이 정상 응답
        every { orderService.getOrder(1L) } returns OrderDto(listOf(OrderDto.OrderItem(10L, 2L)))
        every { productApiClient.reserve(any()) } returns ProductReserveApiResponse(2000L)

        // when - 주문 결제(TCC)
        coordinator.placeOrder(PlaceOrderCommand(1L))

        // then - Try(reserve) → Confirm 순서대로, 보상은 호출되지 않음
        verifyOrder {
            orderService.reserve(1L) // Try
            productApiClient.reserve(any())
            pointApiClient.reservePoint(any())
            productApiClient.confirm(any()) // Confirm
            pointApiClient.confirmPoint(any())
            orderService.confirm(1L)
        }
        verify(exactly = 0) { orderService.cancel(any()) }
        verify(exactly = 0) { orderService.pending(any()) }
    }

    @Test
    fun `Try 단계에서 포인트 예약이 실패하면 보상(cancel)하고 Confirm 으로 넘어가지 않는다`() {
        // given - 포인트 예약이 실패
        every { orderService.getOrder(1L) } returns OrderDto(listOf(OrderDto.OrderItem(10L, 2L)))
        every { productApiClient.reserve(any()) } returns ProductReserveApiResponse(2000L)
        every { pointApiClient.reservePoint(any()) } throws RuntimeException("포인트 부족")

        // when - 주문 결제 시도(Try 실패는 보상 후 호출자에게 전파)
        assertThrows<RuntimeException> { coordinator.placeOrder(PlaceOrderCommand(1L)) }

        // then - 주문·상품·포인트 모두 보상(cancel)
        verify { orderService.cancel(1L) }
        verify { productApiClient.cancel(any()) }
        verify { pointApiClient.cancelPoint(any()) }

        // then - Confirm 단계로는 절대 진입하지 않음
        verify(exactly = 0) { productApiClient.confirm(any()) }
        verify(exactly = 0) { pointApiClient.confirmPoint(any()) }
        verify(exactly = 0) { orderService.confirm(any()) }
    }
}
