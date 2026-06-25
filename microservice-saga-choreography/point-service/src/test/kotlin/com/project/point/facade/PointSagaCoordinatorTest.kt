package com.project.point.facade

import com.project.point.consumer.event.QuantityDecreasedEvent
import com.project.point.exception.BusinessException
import com.project.point.producer.PointUseFailProducer
import com.project.point.producer.PointUsedProducer
import com.project.point.producer.event.PointUseFailEvent
import com.project.point.producer.event.PointUsedEvent
import com.project.point.service.PointService
import com.project.point.service.dto.command.PointUseCancelCommand
import com.project.point.service.dto.command.PointUseCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

/**
 * 사가 2단계(point) 오케스트레이션을 검증한다(참여 서비스는 Mock).
 * 핵심: 포인트 사용이 실패하면 보상(cancel)하고 point-use-fail 을 발행하는가.
 * (현재 PointService.use 에는 학습용 강제 예외가 있어 실제 흐름은 항상 이 실패 분기를 탄다.)
 */
class PointSagaCoordinatorTest {
    private val pointService = mockk<PointService>(relaxed = true)
    private val pointUsedProducer = mockk<PointUsedProducer>(relaxed = true)
    private val pointUseFailProducer = mockk<PointUseFailProducer>(relaxed = true)
    private val coordinator = PointSagaCoordinator(pointService, pointUsedProducer, pointUseFailProducer)

    @Test
    fun `quantity-decreased 성공 - 포인트 사용 후 point-used 를 발행하고 보상하지 않는다`() {
        // given - 포인트 사용 성공 (강제 예외가 없다고 가정)
        every { pointService.use(any()) } returns Unit
        val event = QuantityDecreasedEvent(1L, 2000L)

        // when
        coordinator.handleQuantityDecreased(event)

        // then - userId=1 로 2000 사용, 성공 이벤트 발행, 보상 없음
        verify { pointService.use(PointUseCommand("1", 1L, 2000L)) }
        verify { pointUsedProducer.send(PointUsedEvent(1L)) }
        verify(exactly = 0) { pointService.cancel(any()) }
        verify(exactly = 0) { pointUseFailProducer.send(any()) }
    }

    @Test
    fun `quantity-decreased 실패 - 보상(cancel) 후 point-use-fail 을 발행하고 성공 이벤트는 발행하지 않는다`() {
        // given - 포인트 사용이 실패 (잔액 부족 또는 학습용 강제 예외)
        every { pointService.use(any()) } throws BusinessException("잔액이 부족합니다.")
        val event = QuantityDecreasedEvent(1L, 2000L)

        // when - 코디네이터가 예외를 삼키고 보상으로 전환한다
        coordinator.handleQuantityDecreased(event)

        // then - 보상(cancel) → 실패 이벤트 순서, 성공 이벤트는 없음
        verifyOrder {
            pointService.cancel(PointUseCancelCommand("1"))
            pointUseFailProducer.send(PointUseFailEvent(1L))
        }
        verify(exactly = 0) { pointUsedProducer.send(any()) }
    }
}
