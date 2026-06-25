package com.project.point.facade

import com.project.point.consumer.event.QuantityDecreasedEvent
import com.project.point.producer.PointUseFailProducer
import com.project.point.producer.PointUsedProducer
import com.project.point.producer.event.PointUseFailEvent
import com.project.point.producer.event.PointUsedEvent
import com.project.point.service.PointService
import com.project.point.service.dto.command.PointUseCancelCommand
import com.project.point.service.dto.command.PointUseCommand
import org.springframework.stereotype.Component

/**
 * 사가 2단계 오케스트레이션 책임.
 * - 성공 → point-used (order 를 COMPLETED 로 확정)
 * - 실패 → 보상(cancel) 후 point-use-fail (product 가 재고를 복원하도록 트리거)
 */
@Component
class PointSagaCoordinator(
    private val pointService: PointService,
    private val pointUsedProducer: PointUsedProducer,
    private val pointUseFailProducer: PointUseFailProducer,
) {
    fun handleQuantityDecreased(event: QuantityDecreasedEvent) {
        val requestId = event.orderId.toString()
        try {
            pointService.use(PointUseCommand(requestId, 1L, event.totalPrice))
            pointUsedProducer.send(PointUsedEvent(event.orderId))
        } catch (e: Exception) {
            pointService.cancel(PointUseCancelCommand(requestId))
            pointUseFailProducer.send(PointUseFailEvent(event.orderId))
        }
    }
}
