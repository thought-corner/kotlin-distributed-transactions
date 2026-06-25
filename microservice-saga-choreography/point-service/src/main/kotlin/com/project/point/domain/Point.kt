package com.project.point.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import com.project.point.exception.BusinessException
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "points")
class Point(
    userId: Long = 0,
    amount: Long = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    var userId: Long = userId
        protected set

    var amount: Long = amount
        protected set

    @Version
    var version: Long? = null
        protected set

    fun use(amount: Long) {
        if (this.amount < amount) {
            throw BusinessException("잔액이 부족합니다.")
        }
        this.amount -= amount
    }

    fun cancel(amount: Long) {
        this.amount += amount
    }
}
