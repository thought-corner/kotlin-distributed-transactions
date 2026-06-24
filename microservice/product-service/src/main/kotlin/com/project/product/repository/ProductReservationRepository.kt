package com.project.product.repository

import com.project.product.domain.ProductReservation
import org.springframework.data.jpa.repository.JpaRepository

interface ProductReservationRepository : JpaRepository<ProductReservation, Long> {
    fun findAllByRequestId(requestId: String): List<ProductReservation>
}
