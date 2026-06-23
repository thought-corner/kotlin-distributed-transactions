package com.project.monolithic.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "points")
class Point(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null,
    private val userId: Long = 0,
    private var amount: Long = 0,
) {
    fun usePoint(amount: Long) {
        require(amount > 0) { "사용 포인트는 1 이상이어야 합니다: amount=$amount" }
        require(this.amount >= amount) { "포인트가 부족합니다: balance=${this.amount}, amount=$amount" }
        this.amount -= amount
    }
}
