package com.project.monolithic.repository

import com.project.monolithic.domain.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long>