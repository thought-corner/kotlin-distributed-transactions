package com.project.product.domain

import com.project.product.exception.BusinessException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "product_reservations")
class ProductReservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val requestId: String = "",
    val productId: Long = 0,
    val reservedQuantity: Long = 0,
    val reservedPrice: Long = 0,
) {
    @Enumerated(EnumType.STRING)
    var status: ProductReservationStatus = ProductReservationStatus.RESERVED
        protected set

    fun confirm() {
        if (status == ProductReservationStatus.CANCELLED) {
            throw BusinessException("이미 취소된 예약입니다.")
        }

        status = ProductReservationStatus.CONFIRMED
    }

    fun cancel() {
        if (status == ProductReservationStatus.CONFIRMED) {
            throw BusinessException("이미 확정된 예약입니다.")
        }

        status = ProductReservationStatus.CANCELLED
    }

    enum class ProductReservationStatus {
        RESERVED,
        CONFIRMED,
        CANCELLED,
    }
}
