package com.project.point.consumer

import com.project.point.consumer.event.QuantityDecreasedEvent
import com.project.point.facade.PointSagaCoordinator
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * 사가 2단계: quantity-decreased 를 받아 오케스트레이션(PointSagaCoordinator)에 위임한다.
 */
@Component
class QuantityDecreasedConsumer(
    private val pointSagaCoordinator: PointSagaCoordinator,
) {
    @KafkaListener(
        topics = ["quantity-decreased"],
        groupId = "quantity-decreased-consumer",
        properties = ["spring.json.value.default.type=com.project.point.consumer.event.QuantityDecreasedEvent"],
    )
    fun handle(event: QuantityDecreasedEvent) {
        pointSagaCoordinator.handleQuantityDecreased(event)
    }
}
