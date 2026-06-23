package com.project.monolithic.repository

import com.project.monolithic.domain.Point
import org.springframework.data.jpa.repository.JpaRepository

interface PointRepository : JpaRepository<Point, Long> {
    fun findByUserId(userId: Long): Point?
}
