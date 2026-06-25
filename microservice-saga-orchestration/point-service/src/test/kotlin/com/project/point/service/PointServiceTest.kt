package com.project.point.service

import com.project.point.exception.BusinessException
import com.project.point.repository.PointRepository
import com.project.point.service.dto.command.PointUseCancelCommand
import com.project.point.service.dto.command.PointUseCommand
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

/**
 * 참여 서비스(포인트)의 실제 비즈니스/보상 동작을 H2 로 검증한다.
 * 실패는 "잔액 부족"이라는 실제 조건으로 강제 유발한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PointServiceTest {
    @Autowired
    lateinit var pointService: PointService

    @Autowired
    lateinit var pointRepository: PointRepository

    private fun seededPoint() = pointRepository.findAll().first() // userId 1, amount 10000

    @Test
    fun `정상 사용 - 잔액이 차감된다`() {
        val point = seededPoint()
        val before = point.amount

        pointService.use(PointUseCommand("req-1", point.userId, 2000L))

        assertEquals(before - 2000, pointRepository.findById(point.id!!).get().amount)
    }

    @Test
    fun `잔액 부족 - BusinessException 을 던지고 잔액은 그대로다`() {
        val point = seededPoint()
        val before = point.amount

        assertThrows<BusinessException> { pointService.use(PointUseCommand("req-2", point.userId, before + 1)) }

        assertEquals(before, pointRepository.findById(point.id!!).get().amount)
    }

    @Test
    fun `보상(cancel) - 사용했던 포인트가 원래대로 복원된다`() {
        val point = seededPoint()
        val before = point.amount
        pointService.use(PointUseCommand("req-3", point.userId, 3000L))
        assertEquals(before - 3000, pointRepository.findById(point.id!!).get().amount)

        pointService.cancel(PointUseCancelCommand("req-3"))

        assertEquals(before, pointRepository.findById(point.id!!).get().amount)
    }

    @Test
    fun `사용 멱등성 - 같은 requestId 로 두 번 사용해도 포인트는 한 번만 차감된다`() {
        val point = seededPoint()
        val before = point.amount
        val cmd = PointUseCommand("req-4", point.userId, 1000L)

        pointService.use(cmd)
        pointService.use(cmd) // 중복 호출

        assertEquals(before - 1000, pointRepository.findById(point.id!!).get().amount)
    }
}
