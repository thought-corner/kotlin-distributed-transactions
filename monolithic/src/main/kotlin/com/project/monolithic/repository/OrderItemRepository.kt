package com.project.monolithic.repository

import com.project.monolithic.domain.OrderItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findAllByOrderId(orderId: Long): List<OrderItem>
}