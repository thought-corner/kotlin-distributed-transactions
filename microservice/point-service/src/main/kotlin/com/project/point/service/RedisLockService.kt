package com.project.point.service

import com.project.point.exception.BusinessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class RedisLockService(
    private val stringRedisTemplate: StringRedisTemplate,
) {
    /**
     * 분산락 획득 → 작업 실행 → 해제까지의 생명주기를 이 클래스가 단독으로 책임진다.
     * 호출부(파사드/컨트롤러)는 "무엇을 잠그고 무엇을 실행할지"만 넘긴다.
     */
    fun <T> withLock(key: String, value: String, block: () -> T): T {
        val acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, value) ?: false
        if (!acquired) {
            throw BusinessException("락 획득에 실패하였습니다.")
        }

        try {
            return block()
        } finally {
            stringRedisTemplate.delete(key)
        }
    }
}
