package com.project.order.repository

import com.project.order.domain.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long>
