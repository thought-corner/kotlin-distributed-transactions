package com.project.monolithic.controller

import com.project.monolithic.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 컨트롤러 전역 예외 처리. 도메인/비즈니스 예외를 적절한 HTTP 상태와 RFC 7807 ProblemDetail 로 매핑한다.
 *
 * - BusinessException: 락 획득 실패·주문 없음 등 비즈니스 규칙 위반 → 409 CONFLICT
 *   (not-found 등은 본래 별도 예외 타입으로 분리하는 것이 더 정확하지만, 현 코드의 단일 예외 타입을 존중)
 * - IllegalArgumentException: 도메인 불변식 위반(재고/포인트 부족, 잘못된 수량) → 400 BAD REQUEST
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "비즈니스 규칙 위반")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "잘못된 요청")
}
