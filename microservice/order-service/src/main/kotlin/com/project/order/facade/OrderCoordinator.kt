package com.project.order.facade

import com.project.order.client.PointApiClient
import com.project.order.client.ProductApiClient
import com.project.order.client.dto.PointReserveApiRequest
import com.project.order.client.dto.PointReserveCancelApiRequest
import com.project.order.client.dto.PointReserveConfirmApiRequest
import com.project.order.client.dto.ProductReserveApiRequest
import com.project.order.client.dto.ProductReserveCancelApiRequest
import com.project.order.client.dto.ProductReserveConfirmApiRequest
import com.project.order.exception.BusinessException
import com.project.order.service.OrderService
import com.project.order.service.dto.command.PlaceOrderCommand
import org.springframework.stereotype.Component

/**
 * TCC(Try-Confirm-Cancel) 코디네이터.
 * placeOrder = reserve(Try) → confirm(Confirm). reserve 실패 시 각 참여자에 cancel(Cancel) 보상.
 */
@Component
class OrderCoordinator(
    private val orderService: OrderService,
    private val productApiClient: ProductApiClient,
    private val pointApiClient: PointApiClient,
) {
    fun placeOrder(command: PlaceOrderCommand) {
        reserve(command.orderId)
        confirm(command.orderId)
    }

    private fun reserve(orderId: Long) {
        val requestId = orderId.toString()
        orderService.reserve(orderId)

        try {
            val orderInfo = orderService.getOrder(orderId)

            val productReserveApiRequest =
                ProductReserveApiRequest(
                    requestId = requestId,
                    items =
                        orderInfo.orderItems.map {
                            ProductReserveApiRequest.ReserveItem(it.productId, it.quantity)
                        },
                )

            val productReserveApiResponse = productApiClient.reserve(productReserveApiRequest)
            val totalPrice =
                productReserveApiResponse?.totalPrice
                    ?: throw BusinessException("상품 예약 응답이 비어있습니다.")

            val pointReserveApiRequest =
                PointReserveApiRequest(
                    requestId = requestId,
                    userId = 1L,
                    reserveAmount = totalPrice,
                )

            pointApiClient.reservePoint(pointReserveApiRequest)
        } catch (e: Exception) {
            orderService.cancel(orderId)
            productApiClient.cancel(ProductReserveCancelApiRequest(requestId))
            pointApiClient.cancelPoint(PointReserveCancelApiRequest(requestId))
            // Try 실패(보상 완료)를 전파해 Confirm 단계로 넘어가지 않게 한다.
            throw e
        }
    }

    fun confirm(orderId: Long) {
        val requestId = orderId.toString()
        try {
            productApiClient.confirm(ProductReserveConfirmApiRequest(requestId))
            pointApiClient.confirmPoint(PointReserveConfirmApiRequest(requestId))

            orderService.confirm(orderId)
        } catch (e: Exception) {
            orderService.pending(orderId)
            throw e
        }
    }
}
