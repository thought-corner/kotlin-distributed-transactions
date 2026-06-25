package com.project.order.consumer

import com.project.order.consumer.event.QuantityDecreasedFailEvent
import com.project.order.service.OrderService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * 사가 실패 종착점: quantity-decreased-fail 이벤트를 받으면 주문을 FAILED 로 전이한다.
 * (재고 차감 실패 또는 포인트 사용 실패로 인한 보상이 product 까지 끝난 뒤 도달한다.)
 */
@Component
class QuantityDecreasedFailConsumer(
    private val orderService: OrderService,
) {
    @KafkaListener(
        topics = ["quantity-decreased-fail"],
        groupId = "quantity-decreased-fail-consumer",
        properties = ["spring.json.value.default.type=com.project.order.consumer.event.QuantityDecreasedFailEvent"],
    )
    fun handle(event: QuantityDecreasedFailEvent) {
        orderService.fail(event.orderId)
    }
}
