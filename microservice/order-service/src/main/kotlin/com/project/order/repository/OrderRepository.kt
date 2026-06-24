package com.project.order.repository

import com.project.order.domain.Order
import com.project.order.domain.Order.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {
    /** 어드민 PENDING 목록 조회용. */
    fun findByStatus(status: OrderStatus): List<Order>

    /** 스케줄러 복구 대상 조회용. updatedAt 이 threshold 이전인(= 충분히 묵은) 주문만 재시도 후보로 가져온다. */
    fun findByStatusAndUpdatedAtBefore(
        status: OrderStatus,
        threshold: LocalDateTime,
    ): List<Order>
}
