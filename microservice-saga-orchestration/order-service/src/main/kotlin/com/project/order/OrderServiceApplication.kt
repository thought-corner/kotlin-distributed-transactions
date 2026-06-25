package com.project.order

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@EnableRetry
@EnableScheduling
@SpringBootApplication
class OrderServiceApplication

fun main(args: Array<String>) {
    runApplication<OrderServiceApplication>(*args)
}
