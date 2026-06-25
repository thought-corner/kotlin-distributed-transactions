package com.project.product.consumer

import com.project.product.consumer.event.PointUseFailEvent
import com.project.product.facade.ProductSagaCoordinator
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * 사가 보상 단계 컨슈머. 역직렬화 + 위임만 담당하고, 실제 오케스트레이션은 ProductSagaCoordinator 가 수행한다.
 */
@Component
class PointUseFailConsumer(
    private val productSagaCoordinator: ProductSagaCoordinator,
) {
    @KafkaListener(
        topics = ["point-use-fail"],
        groupId = "point-use-fail-consumer",
        properties = ["spring.json.value.default.type=com.project.product.consumer.event.PointUseFailEvent"],
    )
    fun handle(event: PointUseFailEvent) {
        productSagaCoordinator.handlePointUseFail(event)
    }
}
