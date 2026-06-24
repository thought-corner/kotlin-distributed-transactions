package com.project.order.service

import com.project.order.domain.Order
import com.project.order.domain.OrderItem
import com.project.order.exception.BusinessException
import com.project.order.repository.OrderItemRepository
import com.project.order.repository.OrderRepository
import com.project.order.domain.Order.OrderStatus
import com.project.order.service.dto.OrderDto
import com.project.order.service.dto.PendingOrderView
import com.project.order.service.dto.command.CreateOrderCommand
import com.project.order.service.dto.result.CreateOrderResult
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {
    fun getOrder(orderId: Long): OrderDto {
        orderRepository.findByIdOrNull(orderId)
            ?: throw BusinessException("주문정보가 존재하지 않습니다: orderId=$orderId")
        val orderItems = orderItemRepository.findAllByOrderId(orderId)

        return OrderDto(
            orderItems = orderItems.map { OrderDto.OrderItem(it.productId, it.quantity) },
        )
    }

    @Transactional
    fun createOrder(command: CreateOrderCommand): CreateOrderResult {
        val order = orderRepository.save(Order())
        val orderId = requireNotNull(order.id) { "주문 저장 후 id 가 존재하지 않습니다." }

        val orderItems =
            command.orderItems.map { item ->
                OrderItem(orderId = orderId, productId = item.productId, quantity = item.quantity)
            }
        orderItemRepository.saveAll(orderItems)

        return CreateOrderResult(orderId)
    }

    @Transactional
    fun reserve(orderId: Long) {
        val order =
            orderRepository.findByIdOrNull(orderId)
                ?: throw BusinessException("주문정보가 존재하지 않습니다: orderId=$orderId")
        order.reserve()
        orderRepository.save(order)
    }

    @Transactional
    fun cancel(orderId: Long) {
        val order =
            orderRepository.findByIdOrNull(orderId)
                ?: throw BusinessException("주문정보가 존재하지 않습니다: orderId=$orderId")
        order.cancel()
        orderRepository.save(order)
    }

    @Transactional
    fun confirm(orderId: Long) {
        val order =
            orderRepository.findByIdOrNull(orderId)
                ?: throw BusinessException("주문정보가 존재하지 않습니다: orderId=$orderId")
        order.confirm()
        orderRepository.save(order)
    }

    @Transactional
    fun pending(orderId: Long) {
        val order =
            orderRepository.findByIdOrNull(orderId)
                ?: throw BusinessException("주문정보가 존재하지 않습니다: orderId=$orderId")
        order.pending()
        orderRepository.save(order)
    }

    /** 어드민 PENDING 주문 전체 조회. */
    @Transactional(readOnly = true)
    fun findPendingOrders(): List<PendingOrderView> =
        orderRepository.findByStatus(OrderStatus.PENDING).map(PendingOrderView::from)

    /** 스케줄러 복구 대상 조회. updatedAt 이 threshold 이전(= 재시도 간격을 넘긴) PENDING 주문만. */
    @Transactional(readOnly = true)
    fun findRecoverablePendingOrders(threshold: LocalDateTime): List<PendingOrderView> =
        orderRepository.findByStatusAndUpdatedAtBefore(OrderStatus.PENDING, threshold)
            .map(PendingOrderView::from)

    /** 스케줄러가 PENDING 주문 재구동을 시도할 때 시도 횟수를 누적한다. */
    @Transactional
    fun recordRecoveryAttempt(orderId: Long) {
        val order =
            orderRepository.findByIdOrNull(orderId)
                ?: throw BusinessException("주문정보가 존재하지 않습니다: orderId=$orderId")
        order.recordRecoveryAttempt()
        orderRepository.save(order)
    }
}
