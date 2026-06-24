package com.project.point.repository

import com.project.point.domain.PointReservation
import org.springframework.data.jpa.repository.JpaRepository

interface PointReservationRepository : JpaRepository<PointReservation, Long> {
    fun findByRequestId(requestId: String): PointReservation?
}
