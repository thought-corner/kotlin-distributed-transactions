package com.project.monolithic.controller

import com.project.monolithic.controller.dto.CreateOrderRequest
import com.project.monolithic.controller.dto.CreateOrderResponse
import com.project.monolithic.controller.dto.PlaceOrderRequest
import com.project.monolithic.facade.OrderFacade
import com.project.monolithic.service.OrderService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderService: OrderService,
    private val orderFacade: OrderFacade,
) {
    @PostMapping("/order")
    fun createOrder(
        @RequestBody request: CreateOrderRequest,
    ): CreateOrderResponse {
        val result = orderService.createOrder(request.toCreateOrderCommand())
        return CreateOrderResponse(result.orderId)
    }

    @PostMapping("/order/place")
    fun placeOrder(
        @RequestBody request: PlaceOrderRequest,
    ) {
        orderFacade.placeOrder(request.toPlaceOrderCommand())
    }
}