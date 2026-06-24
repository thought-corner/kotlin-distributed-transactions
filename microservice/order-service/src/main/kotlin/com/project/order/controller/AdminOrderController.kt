package com.project.order.controller

import com.project.order.controller.dto.response.PendingOrderResponse
import com.project.order.facade.OrderCoordinator
import com.project.order.service.OrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * PENDING(미결) 주문에 대한 어드민 수동 제어 엔드포인트.
 * 자동 복구 스케줄러가 풀지 못한 주문을 사람이 강제 확정/취소한다.
 *
 * ## 설계 근거: 왜 수동 제어 창구를 따로 두는가
 *
 * in-doubt 상태의 최종 처리(confirm 인가 cancel 인가)는 본질적으로 비즈니스 판단이 필요한 결정이다.
 * 자동 복구가 상한까지 실패했다는 것은 단순 재시도로는 해소되지 않는 원인(영속적 장애·데이터 불일치)이
 * 남아 있다는 신호이며, 이때 시스템이 임의로 한 방향을 강행하면 금전/재고에 비가역적 오결정을 남길 수 있다.
 * 따라서 마지막 판단은 (1) 참여자들의 실제 상태를 조회해 확인하고(/pending), (2) 사람이 명시적으로
 * confirm 또는 cancel 을 지시하는 멱등 연산으로 분리한다. 이는 자동화가 안전하게 끝낼 수 있는 범위와
 * 사람의 판단이 필요한 범위의 경계를 코드 상에서 분명히 드러내기 위함이다.
 */
@RestController
@RequestMapping("/admin/orders")
class AdminOrderController(
    private val orderService: OrderService,
    private val orderCoordinator: OrderCoordinator,
) {
    /** PENDING 주문 목록 조회(수동 제어 대상 확인용). */
    @GetMapping("/pending")
    fun pendingOrders(): List<PendingOrderResponse> = orderService.findPendingOrders().map(PendingOrderResponse::from)

    /** 강제 확정: 참여자 confirm 을 멱등 재구동하고 주문을 CONFIRMED 로 종료한다. */
    @PostMapping("/{orderId}/confirm")
    fun forceConfirm(
        @PathVariable orderId: Long,
    ) {
        orderCoordinator.confirm(orderId)
    }

    /** 강제 취소: 참여자 cancel(보상)을 멱등 재구동하고 주문을 CANCELLED 로 종료한다. */
    @PostMapping("/{orderId}/cancel")
    fun forceCancel(
        @PathVariable orderId: Long,
    ) {
        orderCoordinator.cancel(orderId)
    }
}
