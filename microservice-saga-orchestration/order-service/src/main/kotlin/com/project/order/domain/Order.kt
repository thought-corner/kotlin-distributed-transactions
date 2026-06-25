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
class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.CREATED
        protected set

    fun request() {
        if (status != OrderStatus.CREATED) {
            throw BusinessException("잘못된 요청입니다.")
        }
        status = OrderStatus.REQUESTED
    }

    fun complete() {
        status = OrderStatus.COMPLETED
    }

    fun fail() {
        if (status != OrderStatus.REQUESTED) {
            throw BusinessException("잘못된 요청입니다.")
        }
        status = OrderStatus.FAILED
    }

    enum class OrderStatus {
        CREATED,
        REQUESTED,
        COMPLETED,
        FAILED,
    }
}
