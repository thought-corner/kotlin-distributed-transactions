package com.project.order.service

import com.project.order.domain.Order
import com.project.order.domain.OrderItem
import com.project.order.repository.OrderItemRepository
import com.project.order.repository.OrderRepository
import com.project.order.service.dto.OrderDto
import com.project.order.service.dto.command.CreateOrderCommand
import com.project.order.service.dto.result.CreateOrderResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {
    @Transactional
    fun createOrder(command: CreateOrderCommand): CreateOrderResult {
        val order = orderRepository.save(Order())

        val orderItems =
            command.items.map { item ->
                OrderItem(orderId = order.id!!, productId = item.productId, quantity = item.quantity)
            }
        orderItemRepository.saveAll(orderItems)

        return CreateOrderResult(order.id!!)
    }

    fun getOrder(orderId: Long): OrderDto {
        val orderItems = orderItemRepository.findAllByOrderId(orderId)

        return OrderDto(
            orderItems.map { OrderDto.OrderItem(it.productId, it.quantity) },
        )
    }

    @Transactional
    fun request(orderId: Long) {
        val order = orderRepository.findById(orderId).orElseThrow()
        order.request()
        orderRepository.save(order)
    }

    @Transactional
    fun complete(orderId: Long) {
        val order = orderRepository.findById(orderId).orElseThrow()
        order.complete()
        orderRepository.save(order)
    }

    @Transactional
    fun fail(orderId: Long) {
        val order = orderRepository.findById(orderId).orElseThrow()
        order.fail()
        orderRepository.save(order)
    }
}
