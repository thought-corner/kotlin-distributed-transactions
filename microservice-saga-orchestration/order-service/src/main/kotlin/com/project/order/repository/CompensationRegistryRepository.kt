package com.project.order.repository

import com.project.order.domain.CompensationRegistry
import org.springframework.data.jpa.repository.JpaRepository

interface CompensationRegistryRepository : JpaRepository<CompensationRegistry, Long>
