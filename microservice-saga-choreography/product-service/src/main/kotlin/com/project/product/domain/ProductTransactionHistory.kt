package com.project.product.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * requestId 기준 멱등성/보상 판단의 근거가 되는 거래 이력.
 * PURCHASE 이력이 있는데 CANCEL 이력이 없으면 보상(재고 복원) 대상이다.
 */
@Entity
@Table(name = "product_transaction_histories")
class ProductTransactionHistory(
    val requestId: String = "",
    val productId: Long = 0,
    val quantity: Long = 0,
    val price: Long = 0,
    @Enumerated(EnumType.STRING)
    val transactionType: TransactionType = TransactionType.PURCHASE,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    enum class TransactionType {
        PURCHASE,
        CANCEL,
    }
}
