package com.project.point.domain

import com.project.point.exception.BusinessException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "point_reservations")
class PointReservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val requestId: String = "",
    val pointId: Long = 0,
    val reservedAmount: Long = 0,
) {
    @Enumerated(EnumType.STRING)
    var status: PointReservationStatus = PointReservationStatus.RESERVED
        protected set

    fun confirm() {
        if (status == PointReservationStatus.CANCELLED) {
            throw BusinessException("취소된 예약은 확정할 수 없습니다.")
        }

        status = PointReservationStatus.CONFIRMED
    }

    fun cancel() {
        if (status == PointReservationStatus.CONFIRMED) {
            throw BusinessException("확정된 예약은 취소할 수 없습니다.")
        }

        status = PointReservationStatus.CANCELLED
    }

    enum class PointReservationStatus {
        RESERVED,
        CONFIRMED,
        CANCELLED,
    }
}
