package com.project.monolithic.domain

import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.EnumType
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

    fun isCompleted(): Boolean = status == OrderStatus.COMPLETED

    fun complete() {
        status = OrderStatus.COMPLETED
    }

    enum class OrderStatus {
        CREATED,
        COMPLETED,
    }
}