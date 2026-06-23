package com.project.monolithic.service

import com.project.monolithic.exception.BusinessException
import com.project.monolithic.repository.PointRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PointService(
    private val pointRepository: PointRepository,
) {
    @Transactional
    fun usePoint(
        userId: Long,
        amount: Long,
    ) {
        val point =
            pointRepository.findByUserId(userId)
                ?: throw BusinessException("포인트 정보를 찾을 수 없습니다: userId=$userId")
        point.usePoint(amount)
    }
}
