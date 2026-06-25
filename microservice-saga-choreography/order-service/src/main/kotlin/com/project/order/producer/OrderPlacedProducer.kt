package com.project.order.producer

import com.project.order.producer.event.OrderPlacedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderPlacedProducer(
    private val kafkaTemplate: KafkaTemplate<String, OrderPlacedEvent>,
) {
    fun send(event: OrderPlacedEvent) {
        kafkaTemplate.send(
            "order-placed",
            event.orderId.toString(),
            event,
        )
    }
}
