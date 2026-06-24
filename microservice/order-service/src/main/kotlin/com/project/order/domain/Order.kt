package com.project.order.domain

import com.project.order.exception.BusinessException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.CREATED
        protected set

    /** Confirm 단계에서 실패해 PENDING 으로 박힌 주문을 스케줄러가 몇 번 재구동했는지. 어드민 알림 임계 판단에 쓰인다. */
    var recoveryAttempts: Int = 0
        protected set

    var createdAt: LocalDateTime? = null
        protected set

    /** 마지막 상태 전이/저장 시각. PENDING 체류 기간과 재시도 간격(grace) 판단에 쓰인다. */
    var updatedAt: LocalDateTime? = null
        protected set

    @PrePersist
    protected fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun complete() {
        status = OrderStatus.COMPLETED
    }

    fun reserve() {
        if (status != OrderStatus.CREATED) {
            throw BusinessException("생성된 단계에서만 예약할 수 있습니다.")
        }
        status = OrderStatus.RESERVED
    }

    fun cancel() {
        if (status != OrderStatus.RESERVED && status != OrderStatus.PENDING) {
            throw BusinessException("예약 혹은 Pending 단계에서만 취소할 수 있습니다.")
        }
        status = OrderStatus.CANCELLED
    }

    fun confirm() {
        if (status != OrderStatus.RESERVED && status != OrderStatus.PENDING) {
            throw BusinessException("예약단계 혹은 Pending 에서만 확정할 수 있습니다.")
        }
        status = OrderStatus.CONFIRMED
    }

    fun pending() {
        if (status != OrderStatus.RESERVED && status != OrderStatus.PENDING) {
            throw BusinessException("예약 혹은 Pending 단계에서만 pending 으로 전이할 수 있습니다.")
        }
        // RESERVED -> PENDING(최초 진입), PENDING -> PENDING(재confirm 실패 시 멱등 재진입) 모두 허용한다.
        status = OrderStatus.PENDING
    }

    /** 스케줄러가 PENDING 주문 재구동을 시도할 때마다 호출해 시도 횟수를 누적한다. */
    fun recordRecoveryAttempt() {
        recoveryAttempts += 1
    }

    enum class OrderStatus {
        CREATED,
        RESERVED,
        CANCELLED,
        CONFIRMED,
        PENDING,
        COMPLETED,
    }
}
