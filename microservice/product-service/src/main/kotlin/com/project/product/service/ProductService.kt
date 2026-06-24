package com.project.product.service

import com.project.product.domain.ProductReservation
import com.project.product.exception.BusinessException
import com.project.product.repository.ProductRepository
import com.project.product.repository.ProductReservationRepository
import com.project.product.service.dto.command.ProductReserveCancelCommand
import com.project.product.service.dto.command.ProductReserveCommand
import com.project.product.service.dto.command.ProductReserveConfirmCommand
import com.project.product.service.dto.result.ProductReserveResult
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productReservationRepository: ProductReservationRepository,
) {
    @Transactional
    fun tryReserve(command: ProductReserveCommand): ProductReserveResult {
        val exists = productReservationRepository.findAllByRequestId(command.requestId)

        if (exists.isNotEmpty()) {
            // 이미 예약된 요청 — 멱등 처리: 기존 예약 금액을 그대로 반환
            val totalPrice = exists.sumOf { it.reservedPrice }
            return ProductReserveResult(totalPrice)
        }

        var totalPrice = 0L
        for (item in command.items) {
            val product =
                productRepository.findByIdOrNull(item.productId)
                    ?: throw BusinessException("상품을 찾을 수 없습니다: id=${item.productId}")

            val price = product.reserve(item.reserveQuantity)
            totalPrice += price

            productRepository.save(product)
            productReservationRepository.save(
                ProductReservation(
                    requestId = command.requestId,
                    productId = item.productId,
                    reservedQuantity = item.reserveQuantity,
                    reservedPrice = price,
                ),
            )
        }

        return ProductReserveResult(totalPrice)
    }

    @Transactional
    fun confirmReserve(command: ProductReserveConfirmCommand) {
        val reservations = productReservationRepository.findAllByRequestId(command.requestId)

        if (reservations.isEmpty()) {
            throw BusinessException("예약된 정보가 없습니다.")
        }

        val alreadyConfirmed =
            reservations.any {
                it.status == ProductReservation.ProductReservationStatus.CONFIRMED
            }

        if (alreadyConfirmed) {
            // 이미 확정됨 — 멱등 처리
            return
        }

        for (reservation in reservations) {
            val product =
                productRepository.findByIdOrNull(reservation.productId)
                    ?: throw BusinessException("상품을 찾을 수 없습니다: id=${reservation.productId}")

            product.confirm(reservation.reservedQuantity)
            reservation.confirm()

            productRepository.save(product)
            productReservationRepository.save(reservation)
        }
    }

    @Transactional
    fun cancelReserve(command: ProductReserveCancelCommand) {
        val reservations = productReservationRepository.findAllByRequestId(command.requestId)

        if (reservations.isEmpty()) {
            throw BusinessException("예약된 정보가 존재하지 않습니다.")
        }

        val alreadyCancelled =
            reservations.any {
                it.status == ProductReservation.ProductReservationStatus.CANCELLED
            }

        if (alreadyCancelled) {
            // 이미 취소됨 — 멱등 처리
            return
        }

        for (reservation in reservations) {
            val product =
                productRepository.findByIdOrNull(reservation.productId)
                    ?: throw BusinessException("상품을 찾을 수 없습니다: id=${reservation.productId}")

            product.cancel(reservation.reservedQuantity)
            reservation.cancel()

            productRepository.save(product)
            productReservationRepository.save(reservation)
        }
    }
}
