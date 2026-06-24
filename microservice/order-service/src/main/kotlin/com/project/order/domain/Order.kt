package com.project.order.domain

import com.project.order.exception.BusinessException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.CREATED
        protected set

    fun complete() {
        status = OrderStatus.COMPLETED
    }

    fun reserve() {
        if (status != OrderStatus.CREATED) {
            throw BusinessException("생성된 단계에서만 예약할 수 있습니다.")
        }
        status = OrderStatus.RESERVED
    }

    fun cancel() {
        if (status != OrderStatus.RESERVED) {
            throw BusinessException("예약단계에서만 취소할 수 있습니다.")
        }
        status = OrderStatus.CANCELLED
    }

    fun confirm() {
        if (status != OrderStatus.RESERVED && status != OrderStatus.PENDING) {
            throw BusinessException("예약단계 혹은 Pending 에서만 확정할 수 있습니다.")
        }
        status = OrderStatus.CONFIRMED
    }

    fun pending() {
        if (status != OrderStatus.RESERVED) {
            throw BusinessException("예약단계에서만 pending 으로 전이할 수 있습니다.")
        }
        status = OrderStatus.PENDING
    }

    enum class OrderStatus {
        CREATED,
        RESERVED,
        CANCELLED,
        CONFIRMED,
        PENDING,
        COMPLETED,
    }
}
