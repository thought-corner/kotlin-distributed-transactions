package com.project.point.producer

import com.project.point.producer.event.PointUseFailEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class PointUseFailProducer(
    private val kafkaTemplate: KafkaTemplate<String, PointUseFailEvent>,
) {
    fun send(event: PointUseFailEvent) {
        kafkaTemplate.send(
            "point-use-fail",
            event.orderId.toString(),
            event,
        )
    }
}
