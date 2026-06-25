package com.project.point.service

import com.project.point.domain.PointTransactionHistory
import com.project.point.exception.BusinessException
import com.project.point.repository.PointRepository
import com.project.point.repository.PointTransactionHistoryRepository
import com.project.point.service.dto.command.PointUseCancelCommand
import com.project.point.service.dto.command.PointUseCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PointService(
    private val pointRepository: PointRepository,
    private val pointTransactionHistoryRepository: PointTransactionHistoryRepository,
) {
    @Transactional
    fun use(command: PointUseCommand) {
        val useHistory =
            pointTransactionHistoryRepository.findByRequestIdAndTransactionType(
                command.requestId,
                PointTransactionHistory.TransactionType.USE,
            )

        // 같은 requestId 로 이미 사용했다면 멱등하게 무시한다.
        if (useHistory != null) {
            println("이미 사용한 이력이 존재합니다.")
            return
        }

        val point =
            pointRepository.findByUserId(command.userId)
                ?: throw BusinessException("포인트가 존재하지 않습니다.")

        point.use(command.amount)
        pointTransactionHistoryRepository.save(
            PointTransactionHistory(
                command.requestId,
                point.id!!,
                command.amount,
                PointTransactionHistory.TransactionType.USE,
            ),
        )
    }

    @Transactional
    fun cancel(command: PointUseCancelCommand) {
        val useHistory =
            pointTransactionHistoryRepository.findByRequestIdAndTransactionType(
                command.requestId,
                PointTransactionHistory.TransactionType.USE,
            ) ?: return

        val cancelHistory =
            pointTransactionHistoryRepository.findByRequestIdAndTransactionType(
                command.requestId,
                PointTransactionHistory.TransactionType.CANCEL,
            )

        // 이미 취소(보상)했다면 멱등하게 무시한다.
        if (cancelHistory != null) {
            println("이미 취소된 요청입니다")
            return
        }

        val point = pointRepository.findById(useHistory.pointId).orElseThrow()

        point.cancel(useHistory.amount)
        pointTransactionHistoryRepository.save(
            PointTransactionHistory(
                command.requestId,
                point.id!!,
                useHistory.amount,
                PointTransactionHistory.TransactionType.CANCEL,
            ),
        )
    }
}
