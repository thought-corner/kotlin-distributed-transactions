package com.project.monolithic.repository

import com.project.monolithic.domain.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long>
