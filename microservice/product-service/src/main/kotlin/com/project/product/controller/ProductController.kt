package com.project.product.controller

import com.project.product.controller.dto.request.ProductReserveCancelRequest
import com.project.product.controller.dto.request.ProductReserveConfirmRequest
import com.project.product.controller.dto.request.ProductReserveRequest
import com.project.product.controller.dto.response.ProductReserveResponse
import com.project.product.facade.ProductFacadeService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductController(
    private val productFacadeService: ProductFacadeService,
) {
    @PostMapping("/product/reserve")
    fun reserve(
        @RequestBody request: ProductReserveRequest,
    ): ProductReserveResponse {
        val result = productFacadeService.tryReserve(request.toCommand())
        return ProductReserveResponse(result.totalPrice)
    }

    @PostMapping("/product/confirm")
    fun confirm(
        @RequestBody request: ProductReserveConfirmRequest,
    ) {
        productFacadeService.confirmReserve(request.toCommand())
    }

    @PostMapping("/product/cancel")
    fun cancel(
        @RequestBody request: ProductReserveCancelRequest,
    ) {
        productFacadeService.cancelReserve(request.toCommand())
    }
}
