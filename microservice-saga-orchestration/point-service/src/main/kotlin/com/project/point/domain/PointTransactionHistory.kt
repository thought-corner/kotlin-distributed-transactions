package com.project.point.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * requestId 기준 멱등성/보상 판단의 근거가 되는 포인트 거래 이력.
 * USE 이력이 있는데 CANCEL 이력이 없으면 보상(포인트 복원) 대상이다.
 */
@Entity
@Table(name = "point_transaction_histories")
class PointTransactionHistory(
    val requestId: String = "",
    val pointId: Long = 0,
    val amount: Long = 0,
    @Enumerated(EnumType.STRING)
    val transactionType: TransactionType = TransactionType.USE,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    enum class TransactionType {
        USE,
        CANCEL,
    }
}
