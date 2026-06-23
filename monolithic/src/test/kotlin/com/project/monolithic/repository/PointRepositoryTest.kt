package com.project.monolithic.repository

import com.project.monolithic.domain.Point
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * PointRepository.findByUserId 파생 쿼리 검증 (실제 MySQL).
 */
@Tag("repository")
@Tag("integration")
class PointRepositoryTest
    @Autowired
    constructor(
        private val pointRepository: PointRepository,
    ) : AbstractRepositoryTest() {
        @Test
        fun `findByUserId 는 해당 사용자의 포인트를 반환한다`() {
            // given: userId=1 의 포인트 저장
            pointRepository.save(Point(userId = 1, amount = 1000))

            // when: userId=1 로 조회
            val point = pointRepository.findByUserId(1)

            // then: 존재함
            assertNotNull(point)
        }

        @Test
        fun `findByUserId 는 없는 사용자면 null 을 반환한다`() {
            // given: userId=1 만 저장
            pointRepository.save(Point(userId = 1, amount = 1000))

            // when & then: 존재하지 않는 userId=999 는 null
            assertNull(pointRepository.findByUserId(999))
        }
    }
