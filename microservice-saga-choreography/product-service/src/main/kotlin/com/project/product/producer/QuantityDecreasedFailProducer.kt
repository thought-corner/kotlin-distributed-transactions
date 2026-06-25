package com.project.product.producer

import com.project.product.producer.event.QuantityDecreasedFailEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class QuantityDecreasedFailProducer(
    private val kafkaTemplate: KafkaTemplate<String, QuantityDecreasedFailEvent>,
) {
    fun send(event: QuantityDecreasedFailEvent) {
        kafkaTemplate.send(
            "quantity-decreased-fail",
            event.orderId.toString(),
            event,
        )
    }
}
