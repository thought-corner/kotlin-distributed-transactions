package com.project.point.facade

import com.project.point.consumer.event.QuantityDecreasedEvent
import com.project.point.producer.PointUseFailProducer
import com.project.point.producer.PointUsedProducer
import com.project.point.producer.event.PointUseFailEvent
import com.project.point.producer.event.PointUsedEvent
import com.project.point.repository.PointRepository
import com.project.point.service.PointService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

/**
 * 포인트 단계 사가 흐름을 실제 서비스 + H2 로 검증한다(통합).
 * Kafka 프로듀서만 Mock(경계)으로 두고, 잔액 차감/보상(복원)이 실제 DB 에 반영되는지 확인한다.
 * 실패는 "잔액 부족"이라는 실제 조건으로 유발한다.
 */
@SpringBootTest(
    properties = ["spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"],
)
@ActiveProfiles("test")
class PointSagaFlowTest {
    @Autowired lateinit var pointService: PointService
    @Autowired lateinit var pointRepository: PointRepository

    private val pointUsedProducer = mockk<PointUsedProducer>(relaxed = true)
    private val pointUseFailProducer = mockk<PointUseFailProducer>(relaxed = true)
    private val coordinator by lazy {
        PointSagaCoordinator(pointService, pointUsedProducer, pointUseFailProducer)
    }

    private fun seededPoint() = pointRepository.findAll().first()

    @Test
    fun `정상 - 포인트가 차감되고 point-used 가 발행된다`() {
        val point = seededPoint()
        val before = point.amount
        val orderId = 2001L

        coordinator.handleQuantityDecreased(QuantityDecreasedEvent(orderId, 2000L))

        assertEquals(before - 2000, pointRepository.findById(point.id!!).get().amount)
        verify { pointUsedProducer.send(PointUsedEvent(orderId)) }
        verify(exactly = 0) { pointUseFailProducer.send(any()) }
    }

    @Test
    fun `잔액 부족 - 보상 후 point-use-fail 이 발행되고 잔액은 그대로다`() {
        val point = seededPoint()
        val before = point.amount
        val orderId = 2002L

        // 잔액보다 큰 금액을 요청해 실제 예외(잔액 부족)를 유발한다.
        coordinator.handleQuantityDecreased(QuantityDecreasedEvent(orderId, before + 1))

        assertEquals(before, pointRepository.findById(point.id!!).get().amount)
        verify { pointUseFailProducer.send(PointUseFailEvent(orderId)) }
        verify(exactly = 0) { pointUsedProducer.send(any()) }
    }

    @Test
    fun `멱등성 - 같은 quantity-decreased 를 두 번 받아도 포인트는 한 번만 차감된다`() {
        val point = seededPoint()
        val before = point.amount
        val orderId = 2003L
        val event = QuantityDecreasedEvent(orderId, 1000L)

        coordinator.handleQuantityDecreased(event)
        coordinator.handleQuantityDecreased(event) // 중복 전달(재처리)

        assertEquals(before - 1000, pointRepository.findById(point.id!!).get().amount)
    }
}
