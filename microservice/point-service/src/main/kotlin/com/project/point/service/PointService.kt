package com.project.point.service

import com.project.point.domain.PointReservation
import com.project.point.exception.BusinessException
import com.project.point.repository.PointRepository
import com.project.point.repository.PointReservationRepository
import com.project.point.service.dto.command.PointReserveCancelCommand
import com.project.point.service.dto.command.PointReserveCommand
import com.project.point.service.dto.command.PointReserveConfirmCommand
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PointService(
    private val pointRepository: PointRepository,
    private val pointReservationRepository: PointReservationRepository,
) {
    @Transactional
    fun tryReserve(command: PointReserveCommand) {
        val reservation = pointReservationRepository.findByRequestId(command.requestId)

        if (reservation != null) {
            // 이미 예약된 요청 — 멱등 처리
            return
        }

        val point =
            pointRepository.findByUserId(command.userId)
                ?: throw BusinessException("포인트 정보를 찾을 수 없습니다: userId=${command.userId}")

        point.reserve(command.reserveAmount)
        pointReservationRepository.save(
            PointReservation(
                requestId = command.requestId,
                pointId = requireNotNull(point.id),
                reservedAmount = command.reserveAmount,
            ),
        )
    }

    @Transactional
    fun confirmReserve(command: PointReserveConfirmCommand) {
        val reservation =
            pointReservationRepository.findByRequestId(command.requestId)
                ?: throw BusinessException("예약내역이 존재하지 않습니다.")

        if (reservation.status == PointReservation.PointReservationStatus.CONFIRMED) {
            // 이미 확정된 예약 — 멱등 처리
            return
        }

        val point =
            pointRepository.findByIdOrNull(reservation.pointId)
                ?: throw BusinessException("포인트 정보를 찾을 수 없습니다: pointId=${reservation.pointId}")

        point.confirm(reservation.reservedAmount)
        reservation.confirm()

        pointRepository.save(point)
        pointReservationRepository.save(reservation)
    }

    @Transactional
    fun cancelReserve(command: PointReserveCancelCommand) {
        val reservation =
            pointReservationRepository.findByRequestId(command.requestId)
                ?: throw BusinessException("예약내역이 존재하지 않습니다.")

        if (reservation.status == PointReservation.PointReservationStatus.CANCELLED) {
            // 이미 취소된 예약 — 멱등 처리
            return
        }

        val point =
            pointRepository.findByIdOrNull(reservation.pointId)
                ?: throw BusinessException("포인트 정보를 찾을 수 없습니다: pointId=${reservation.pointId}")

        point.cancel(reservation.reservedAmount)
        reservation.cancel()

        pointRepository.save(point)
        pointReservationRepository.save(reservation)
    }
}
