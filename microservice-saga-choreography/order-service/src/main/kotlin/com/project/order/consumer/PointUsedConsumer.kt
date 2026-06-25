package com.project.order.consumer

import com.project.order.consumer.event.PointUsedEvent
import com.project.order.service.OrderService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * 사가 성공 종착점: point-used 이벤트를 받으면 주문을 COMPLETED 로 확정한다.
 */
@Component
class PointUsedConsumer(
    private val orderService: OrderService,
) {
    @KafkaListener(
        topics = ["point-used"],
        groupId = "point-used-consumer",
        properties = ["spring.json.value.default.type=com.project.order.consumer.event.PointUsedEvent"],
    )
    fun handle(event: PointUsedEvent) {
        orderService.complete(event.orderId)
    }
}
