package com.project.monolithic.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null,
    private val price: Long = 0,
    private var stock: Long = 0,
) {
    fun decreaseStock(quantity: Long) {
        require(quantity > 0) { "구매 수량은 1 이상이어야 합니다: quantity=$quantity" }
        require(stock >= quantity) { "재고가 부족합니다: stock=$stock, quantity=$quantity" }
        stock -= quantity
    }

    fun calculateTotalPrice(quantity: Long): Long = quantity * price
}
