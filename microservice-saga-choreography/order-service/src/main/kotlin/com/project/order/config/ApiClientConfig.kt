package com.project.order.config

import com.project.order.client.PointApiClient
import com.project.order.client.ProductApiClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class ApiClientConfig {

    @Bean
    fun productApiClient(): ProductApiClient =
        ProductApiClient(
            RestClient.builder()
                .baseUrl("http://localhost:8082")
                .build(),
        )

    @Bean
    fun pointApiClient(): PointApiClient =
        PointApiClient(
            RestClient.builder()
                .baseUrl("http://localhost:8083")
                .build(),
        )
}
