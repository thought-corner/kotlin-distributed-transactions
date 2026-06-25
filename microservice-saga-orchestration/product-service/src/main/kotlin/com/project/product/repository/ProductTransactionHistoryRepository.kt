package com.project.product.repository

import com.project.product.domain.ProductTransactionHistory
import org.springframework.data.jpa.repository.JpaRepository

interface ProductTransactionHistoryRepository : JpaRepository<ProductTransactionHistory, Long> {
    fun findAllByRequestIdAndTransactionType(
        requestId: String,
        transactionType: ProductTransactionHistory.TransactionType,
    ): List<ProductTransactionHistory>
}
