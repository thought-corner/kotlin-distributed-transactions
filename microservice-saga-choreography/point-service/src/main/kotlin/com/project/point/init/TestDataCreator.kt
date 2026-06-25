package com.project.point.init

import com.project.point.domain.Point
import com.project.point.repository.PointRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class TestDataCreator(
    private val pointRepository: PointRepository,
) {
    @PostConstruct
    fun createTestData() {
        pointRepository.save(Point(userId = 1L, amount = 10000L))
    }
}
