package com.project.point.repository

import com.project.point.domain.Point
import org.springframework.data.jpa.repository.JpaRepository

interface PointRepository : JpaRepository<Point, Long> {
    fun findByUserId(userId: Long): Point?
}
