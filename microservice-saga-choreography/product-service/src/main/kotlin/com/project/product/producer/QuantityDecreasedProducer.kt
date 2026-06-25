package com.project.product.producer

import com.project.product.producer.event.QuantityDecreasedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class QuantityDecreasedProducer(
    private val kafkaTemplate: KafkaTemplate<String, QuantityDecreasedEvent>,
) {
    fun send(event: QuantityDecreasedEvent) {
        kafkaTemplate.send(
            "quantity-decreased",
            event.orderId.toString(),
            event,
        )
    }
}
