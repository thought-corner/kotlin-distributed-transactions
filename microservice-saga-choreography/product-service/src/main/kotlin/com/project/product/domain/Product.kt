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
    quantity: Long = 0,
    price: Long = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    var quantity: Long = quantity
        protected set

    var price: Long = price
        protected set

    @Version
    var version: Long? = null
        protected set

    fun calculatePrice(quantity: Long): Long = price * quantity

    fun buy(quantity: Long) {
        if (this.quantity < quantity) {
            throw BusinessException("재고가 부족합니다.")
        }
        this.quantity -= quantity
    }

    fun cancel(quantity: Long) {
        this.quantity += quantity
    }
}
