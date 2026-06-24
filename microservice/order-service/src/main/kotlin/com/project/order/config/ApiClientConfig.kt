package com.project.order.config

import com.project.order.client.PointApiClient
import com.project.order.client.ProductApiClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class ApiClientConfig {
    @Bean
    fun productApiClient(): ProductApiClient {
        val factory = HttpComponentsClientHttpRequestFactory()
        factory.setReadTimeout(Duration.ofSeconds(2))

        return ProductApiClient(
            RestClient.builder()
                .requestFactory(factory)
                .baseUrl("http://localhost:8082")
                .build(),
        )
    }

    @Bean
    fun pointApiClient(): PointApiClient {
        val factory = HttpComponentsClientHttpRequestFactory()
        factory.setReadTimeout(Duration.ofSeconds(2))

        return PointApiClient(
            RestClient.builder()
                .requestFactory(factory)
                .baseUrl("http://localhost:8083")
                .build(),
        )
    }
}
