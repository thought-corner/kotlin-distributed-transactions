package com.project.order.facade

import com.project.order.client.PointApiClient
import com.project.order.client.ProductApiClient
import com.project.order.client.dto.PointUseApiRequest
import com.project.order.client.dto.PointUseCancelApiRequest
import com.project.order.client.dto.ProductBuyApiRequest
import com.project.order.client.dto.ProductBuyCancelApiRequest
import com.project.order.domain.CompensationRegistry
import com.project.order.repository.CompensationRegistryRepository
import com.project.order.service.OrderService
import com.project.order.service.dto.command.PlaceOrderCommand
import org.springframework.stereotype.Component

/**
 * 동기(오케스트레이션/TCC) 방식의 주문 조정자. 원본 레퍼런스에 함께 들어 있던 코드로,
 * 현재 코레오그래피 흐름(OrderService.placeOrder → 이벤트 발행)에서는 사용되지 않는다.
 * 동기 방식과의 비교 학습용으로 남겨 둔다.
 */
@Component
class OrderCoordinator(
    private val orderService: OrderService,
    private val compensationRegistryRepository: CompensationRegistryRepository,
    private val productApiClient: ProductApiClient,
    private val pointApiClient: PointApiClient,
) {
    fun placeOrder(command: PlaceOrderCommand) {
        orderService.request(command.orderId)
        val orderDto = orderService.getOrder(command.orderId)

        try {
            val productBuyApiRequest =
                ProductBuyApiRequest(
                    command.orderId.toString(),
                    orderDto.orderItems.map { ProductBuyApiRequest.ProductInfo(it.productId, it.quantity) },
                )

            val buyApiResponse = productApiClient.buy(productBuyApiRequest)!!

            val pointUseApiRequest =
                PointUseApiRequest(
                    command.orderId.toString(),
                    1L,
                    buyApiResponse.totalPrice,
                )

            pointApiClient.use(pointUseApiRequest)

            orderService.complete(command.orderId)
        } catch (e: Exception) {
            rollback(command.orderId)
            throw e
        }
    }

    fun rollback(orderId: Long) {
        try {
            val productBuyCancelApiResponse =
                productApiClient.cancel(ProductBuyCancelApiRequest(orderId.toString()))!!

            if (productBuyCancelApiResponse.totalPrice > 0) {
                pointApiClient.cancel(PointUseCancelApiRequest(orderId.toString()))
            }

            orderService.fail(orderId)
        } catch (e: Exception) {
            compensationRegistryRepository.save(CompensationRegistry(orderId))
            throw e
        }
    }
}
