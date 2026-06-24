package com.project.order.client

import com.project.order.client.dto.PointReserveApiRequest
import com.project.order.client.dto.PointReserveCancelApiRequest
import com.project.order.client.dto.PointReserveConfirmApiRequest
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

class PointApiClient(
    private val restClient: RestClient,
) {
    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [HttpClientErrorException.BadRequest::class, HttpClientErrorException.NotFound::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 500),
    )
    fun reservePoint(request: PointReserveApiRequest) {
        restClient
            .post()
            .uri("/point/reserve")
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
    fun confirmPoint(request: PointReserveConfirmApiRequest) {
        restClient
            .post()
            .uri("/point/confirm")
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
    fun cancelPoint(request: PointReserveCancelApiRequest) {
        restClient
            .post()
            .uri("/point/cancel")
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }
}
