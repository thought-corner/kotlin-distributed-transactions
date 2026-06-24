package com.project.product.domain

import com.project.product.exception.BusinessException
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    private var quantity: Long = 0,
    private val price: Long = 0,
    private var reservedQuantity: Long = 0,
) {
    @Version
    private var version: Long? = null

    fun reserve(requestedQuantity: Long): Long {
        val reservableQuantity = quantity - reservedQuantity

        if (reservableQuantity < requestedQuantity) {
            throw BusinessException("예약할 수 있는 수량이 부족합니다.")
        }

        reservedQuantity += requestedQuantity

        return price * requestedQuantity
    }

    fun cancel(requestedQuantity: Long) {
        if (reservedQuantity < requestedQuantity) {
            throw BusinessException("예약된 수량이 부족합니다.")
        }

        reservedQuantity -= requestedQuantity
    }

    fun confirm(requestedQuantity: Long) {
        if (quantity < requestedQuantity) {
            throw BusinessException("재고가 부족합니다.")
        }

        if (reservedQuantity < requestedQuantity) {
            throw BusinessException("예약된 수량이 부족합니다.")
        }

        quantity -= requestedQuantity
        reservedQuantity -= requestedQuantity
    }

    fun calculatePrice(quantity: Long): Long = price * quantity

    fun buy(quantity: Long) {
        if (this.quantity < quantity) {
            throw BusinessException("재고가 부족합니다.")
        }

        this.quantity -= quantity
    }
}
