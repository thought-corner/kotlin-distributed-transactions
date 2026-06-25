package com.project.point.producer

import com.project.point.producer.event.PointUsedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class PointUsedProducer(
    private val kafkaTemplate: KafkaTemplate<String, PointUsedEvent>,
) {
    fun send(event: PointUsedEvent) {
        kafkaTemplate.send(
            "point-used",
            event.orderId.toString(),
            event,
        )
    }
}
