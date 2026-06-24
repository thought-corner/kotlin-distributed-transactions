package com.project.point.domain

import com.project.point.exception.BusinessException
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "points")
class Point(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    private val userId: Long = 0,
    private var amount: Long = 0,
    private var reservedAmount: Long = 0,
) {
    @Version
    private var version: Long? = null

    fun reserve(reserveAmount: Long) {
        val reservableAmount = this.amount - this.reservedAmount

        if (reservableAmount < reserveAmount) {
            throw BusinessException("금액이 부족합니다.")
        }

        reservedAmount += reserveAmount
    }

    fun cancel(reserveAmount: Long) {
        if (reservedAmount < reserveAmount) {
            throw BusinessException("예약된 금액이 부족합니다.")
        }

        this.reservedAmount -= reserveAmount
    }

    fun use(amount: Long) {
        if (this.amount < amount) {
            throw BusinessException("잔액이 부족합니다.")
        }

        this.amount -= amount
    }

    fun confirm(reserveAmount: Long) {
        if (this.amount < reserveAmount) {
            throw BusinessException("포인트가 부족합니다.")
        }

        if (this.reservedAmount < reserveAmount) {
            throw BusinessException("예약된 금액이 부족합니다.")
        }

        this.amount -= reserveAmount
        this.reservedAmount -= reserveAmount
    }
}
