package com.project.order.controller

import com.project.order.controller.dto.request.CreateOrderRequest
import com.project.order.controller.dto.request.PlaceOrderRequest
import com.project.order.controller.dto.response.CreateOrderResponse
import com.project.order.domain.Order
import com.project.order.facade.OrderFacade
import com.project.order.service.OrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderService: OrderService,
    private val orderFacade: OrderFacade,
) {
    @PostMapping("/order")
    fun createOrder(@RequestBody request: CreateOrderRequest): CreateOrderResponse {
        val result = orderService.createOrder(request.toCommand())
        return CreateOrderResponse(result.orderId)
    }

    @PostMapping("/order/place")
    fun placeOrder(@RequestBody request: PlaceOrderRequest) {
        orderFacade.placeOrder(request.toCommand())
    }

    @GetMapping("/order/{id}/status")
    fun getStatus(@PathVariable("id") id: Long): Order.OrderStatus = orderService.getStatus(id)
}
