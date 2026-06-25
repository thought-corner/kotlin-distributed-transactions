package com.project.order.facade

import com.project.order.client.PointApiClient
import com.project.order.client.ProductApiClient
import com.project.order.client.dto.ProductBuyApiResponse
import com.project.order.client.dto.ProductBuyCancelApiResponse
import com.project.order.domain.CompensationRegistry
import com.project.order.repository.CompensationRegistryRepository
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
 * 오케스트레이션 조정자(Orchestrator)의 흐름을 검증한다(참여 서비스/클라이언트는 Mock).
 * 실패 시나리오에 초점: 예외는 테스트에서 클라이언트 Mock 으로 강제 유발한다.
 * - 해피패스: 상품 구매 → 포인트 사용 → 주문 완료
 * - 상품 구매 실패: 롤백(구매 이력 없음) 후 주문 실패
 * - 포인트 사용 실패: 상품 재고 보상 + 포인트 보상 후 주문 실패
 * - 보상(rollback) 자체 실패: CompensationRegistry 적재 후 예외 재전파
 */
class OrderCoordinatorTest {
    private val orderService = mockk<OrderService>(relaxed = true)
    private val compensationRegistryRepository = mockk<CompensationRegistryRepository>(relaxed = true)
    private val productApiClient = mockk<ProductApiClient>(relaxed = true)
    private val pointApiClient = mockk<PointApiClient>(relaxed = true)
    private val coordinator =
        OrderCoordinator(orderService, compensationRegistryRepository, productApiClient, pointApiClient)

    private val orderId = 1L

    private fun givenOrderItems() {
        every { orderService.getOrder(orderId) } returns OrderDto(listOf(OrderDto.OrderItem(10L, 2L)))
    }

    @Test
    fun `해피패스 - 상품 구매 후 포인트 사용, 주문 완료까지 순서대로 수행한다`() {
        // given - 구매/사용 모두 성공
        givenOrderItems()
        every { productApiClient.buy(any()) } returns ProductBuyApiResponse(2000L)

        // when
        coordinator.placeOrder(PlaceOrderCommand(orderId))

        // then - request → buy → use → complete, 보상은 일어나지 않음
        verifyOrder {
            orderService.request(orderId)
            productApiClient.buy(any())
            pointApiClient.use(any())
            orderService.complete(orderId)
        }
        verify(exactly = 0) { productApiClient.cancel(any()) }
        verify(exactly = 0) { orderService.fail(any()) }
        verify(exactly = 0) { compensationRegistryRepository.save(any()) }
    }

    @Test
    fun `상품 구매 실패 - 롤백 후 주문을 실패 처리하고 예외를 전파한다`() {
        // given - 상품 구매가 실패(재고 부족 등)
        givenOrderItems()
        every { productApiClient.buy(any()) } throws RuntimeException("재고가 부족합니다.")
        // 롤백: 구매 이력이 없으므로 취소 총액 0
        every { productApiClient.cancel(any()) } returns ProductBuyCancelApiResponse(0L)

        // when - 보상 후 예외 재전파
        assertThrows<RuntimeException> { coordinator.placeOrder(PlaceOrderCommand(orderId)) }

        // then - 포인트는 손도 안 댔고, 취소 총액 0 이라 포인트 보상도 없음, 주문은 실패
        verify(exactly = 0) { pointApiClient.use(any()) }
        verify { productApiClient.cancel(any()) }
        verify(exactly = 0) { pointApiClient.cancel(any()) }
        verify { orderService.fail(orderId) }
        verify(exactly = 0) { orderService.complete(any()) }
        verify(exactly = 0) { compensationRegistryRepository.save(any()) }
    }

    @Test
    fun `포인트 사용 실패 - 상품 재고와 포인트를 보상한 뒤 주문을 실패 처리한다`() {
        // given - 구매는 성공했지만 포인트 사용이 실패
        givenOrderItems()
        every { productApiClient.buy(any()) } returns ProductBuyApiResponse(2000L)
        every { pointApiClient.use(any()) } throws RuntimeException("잔액이 부족합니다.")
        // 롤백: 구매 취소 총액 > 0 이므로 포인트 보상까지 수행
        every { productApiClient.cancel(any()) } returns ProductBuyCancelApiResponse(2000L)

        // when
        assertThrows<RuntimeException> { coordinator.placeOrder(PlaceOrderCommand(orderId)) }

        // then - 상품 보상 → 포인트 보상 → 주문 실패 순서
        verifyOrder {
            productApiClient.buy(any())
            pointApiClient.use(any())
            productApiClient.cancel(any())
            pointApiClient.cancel(any())
            orderService.fail(orderId)
        }
        verify(exactly = 0) { orderService.complete(any()) }
        verify(exactly = 0) { compensationRegistryRepository.save(any()) }
    }

    @Test
    fun `보상 자체가 실패하면 - CompensationRegistry 에 적재하고 예외를 전파한다`() {
        // given - 포인트 사용 실패로 롤백에 진입했는데, 보상(상품 취소)마저 실패
        givenOrderItems()
        every { productApiClient.buy(any()) } returns ProductBuyApiResponse(2000L)
        every { pointApiClient.use(any()) } throws RuntimeException("잔액이 부족합니다.")
        every { productApiClient.cancel(any()) } throws RuntimeException("상품 보상 실패")

        // when - 보상 실패는 수동 복구 대상으로 적재 후 예외 재전파
        assertThrows<RuntimeException> { coordinator.placeOrder(PlaceOrderCommand(orderId)) }

        // then - CompensationRegistry 적재, 주문 실패 처리는 도달하지 못함
        verify { compensationRegistryRepository.save(any<CompensationRegistry>()) }
        verify(exactly = 0) { orderService.fail(any()) }
        verify(exactly = 0) { orderService.complete(any()) }
    }
}
