package com.project.order.client

import com.project.order.client.dto.ProductReserveApiRequest
import com.project.order.client.dto.ProductReserveApiResponse
import com.project.order.client.dto.ProductReserveCancelApiRequest
import com.project.order.client.dto.ProductReserveConfirmApiRequest
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

class ProductApiClient(
    private val restClient: RestClient,
) {
    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [HttpClientErrorException.BadRequest::class, HttpClientErrorException.NotFound::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 500),
    )
    fun reserve(request: ProductReserveApiRequest): ProductReserveApiResponse? =
        restClient.post()
            .uri("/product/reserve")
            .body(request)
            .retrieve()
            .body(ProductReserveApiResponse::class.java)

    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [HttpClientErrorException.BadRequest::class, HttpClientErrorException.NotFound::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 500),
    )
    fun confirm(request: ProductReserveConfirmApiRequest) {
        restClient.post()
            .uri("/product/confirm")
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [HttpClientErrorException.BadRequest::class, HttpClientErrorException.NotFound::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 500),
    )
    fun cancel(request: ProductReserveCancelApiRequest) {
        restClient.post()
            .uri("/product/cancel")
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }
}
