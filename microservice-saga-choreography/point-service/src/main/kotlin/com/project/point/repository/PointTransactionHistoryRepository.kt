package com.project.point.repository

import com.project.point.domain.PointTransactionHistory
import org.springframework.data.jpa.repository.JpaRepository

interface PointTransactionHistoryRepository : JpaRepository<PointTransactionHistory, Long> {
    fun findByRequestIdAndTransactionType(
        requestId: String,
        transactionType: PointTransactionHistory.TransactionType,
    ): PointTransactionHistory?
}
