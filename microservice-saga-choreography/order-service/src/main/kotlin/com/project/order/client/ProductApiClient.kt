package com.project.order.client

import com.project.order.client.dto.ProductBuyApiRequest
import com.project.order.client.dto.ProductBuyApiResponse
import com.project.order.client.dto.ProductBuyCancelApiRequest
import com.project.order.client.dto.ProductBuyCancelApiResponse
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.RestClient

open class ProductApiClient(
    private val restClient: RestClient,
) {
    @Retryable(retryFor = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 500))
    open fun buy(request: ProductBuyApiRequest): ProductBuyApiResponse? =
        restClient
            .post()
            .uri("/product/buy")
            .body(request)
            .retrieve()
            .body(ProductBuyApiResponse::class.java)

    @Retryable(retryFor = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 500))
    open fun cancel(request: ProductBuyCancelApiRequest): ProductBuyCancelApiResponse? =
        restClient
            .post()
            .uri("/product/buy/cancel")
            .body(request)
            .retrieve()
            .body(ProductBuyCancelApiResponse::class.java)
}
