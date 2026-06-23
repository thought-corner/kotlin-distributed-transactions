package com.project.monolithic.service

import com.project.monolithic.domain.Order
import com.project.monolithic.domain.OrderItem
import com.project.monolithic.exception.BusinessException
import com.project.monolithic.repository.OrderItemRepository
import com.project.monolithic.repository.OrderRepository
import com.project.monolithic.service.dto.CreateOrderCommand
import com.project.monolithic.service.dto.CreateOrderResult
import com.project.monolithic.service.dto.PlaceOrderCommand
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val pointService: PointService,
    private val productService: ProductService,
) {
    @Transactional
    fun createOrder(command: CreateOrderCommand): CreateOrderResult {
        val order = orderRepository.save(Order())
        val orderId = requireNotNull(order.id) { "주문 저장 후 id 가 존재하지 않습니다." }

        val orderItems = command.orderItems.map { item ->
            OrderItem(orderId = orderId, productId = item.productId, quantity = item.quantity)
        }
        orderItemRepository.saveAll(orderItems)

        return CreateOrderResult(orderId)
    }

    @Transactional
    fun placeOrder(command: PlaceOrderCommand) {
        val order = orderRepository.findByIdOrNull(command.orderId)
            ?: throw BusinessException("주문정보가 존재하지 않습니다.")

        if (order.isCompleted()) {
            return
        }

        val orderId = requireNotNull(order.id)
        var totalPrice = 0L
        val orderItems = orderItemRepository.findAllByOrderId(orderId)
        for (item in orderItems) {
            val price = productService.buyProduct(item.productId, item.quantity)
            totalPrice += price
        }

        pointService.usePoint(1L, totalPrice)

        order.complete()

        println("결제 완료!!!")
        Thread.sleep(3000)
    }
}