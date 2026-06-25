package com.project.order.client

import com.project.order.client.dto.PointUseApiRequest
import com.project.order.client.dto.PointUseCancelApiRequest
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.RestClient

open class PointApiClient(
    private val restClient: RestClient,
) {
    @Retryable(retryFor = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 500))
    open fun use(request: PointUseApiRequest) {
        restClient
            .post()
            .uri("/point/use")
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    @Retryable(retryFor = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 500))
    open fun cancel(request: PointUseCancelApiRequest) {
        restClient
            .post()
            .uri("/point/use/cancel")
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }
}
