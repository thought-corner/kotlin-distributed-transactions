package com.project.product.controller

import com.project.product.controller.dto.request.ProductBuyCancelRequest
import com.project.product.controller.dto.request.ProductBuyRequest
import com.project.product.controller.dto.response.ProductBuyCancelResponse
import com.project.product.controller.dto.response.ProductBuyResponse
import com.project.product.facade.ProductFacade
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 동기(REST) 진입점. 오케스트레이터가 호출하는 참여자(participant) 엔드포인트다.
 * 분산락은 ProductFacade 가 담당하므로 컨트롤러는 얇게 위임만 한다.
 */
@RestController
class ProductController(
    private val productFacade: ProductFacade,
) {
    @PostMapping("/product/buy")
    fun buy(@RequestBody request: ProductBuyRequest): ProductBuyResponse {
        val buyResult = productFacade.buy(request.toCommand())
        return ProductBuyResponse(buyResult.totalPrice)
    }

    @PostMapping("/product/buy/cancel")
    fun cancel(@RequestBody request: ProductBuyCancelRequest): ProductBuyCancelResponse {
        val cancelResult = productFacade.cancel(request.toCommand())
        return ProductBuyCancelResponse(cancelResult.totalPrice)
    }
}
