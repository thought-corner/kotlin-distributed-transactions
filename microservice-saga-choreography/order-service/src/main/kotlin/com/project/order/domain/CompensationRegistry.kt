package com.project.order.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 보상 트랜잭션(rollback)마저 실패한 주문을 적재해 두는 테이블.
 * 별도 배치/운영자가 PENDING 건을 재처리하는 출발점이 된다.
 */
@Entity
@Table(name = "compensation_registries")
class CompensationRegistry(
    val orderId: Long = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    var status: CompensationRegistryStatus = CompensationRegistryStatus.PENDING
        protected set

    enum class CompensationRegistryStatus {
        PENDING,
        COMPLETE,
    }
}
