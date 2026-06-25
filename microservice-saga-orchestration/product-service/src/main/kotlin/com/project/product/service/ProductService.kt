package com.project.product.service

import com.project.product.domain.ProductTransactionHistory
import com.project.product.repository.ProductRepository
import com.project.product.repository.ProductTransactionHistoryRepository
import com.project.product.service.dto.command.ProductBuyCancelCommand
import com.project.product.service.dto.command.ProductBuyCommand
import com.project.product.service.dto.result.ProductBuyCancelResult
import com.project.product.service.dto.result.ProductBuyResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productTransactionHistoryRepository: ProductTransactionHistoryRepository,
) {

    @Transactional
    fun buy(command: ProductBuyCommand): ProductBuyResult {
        val histories = productTransactionHistoryRepository.findAllByRequestIdAndTransactionType(
            command.requestId,
            ProductTransactionHistory.TransactionType.PURCHASE,
        )

        // 같은 requestId 로 이미 구매했다면 멱등하게 기존 결과를 돌려준다.
        if (histories.isNotEmpty()) {
            println("이미 구매한 이력이 있습니다.")
            val totalPrice = histories.sumOf { it.price }
            return ProductBuyResult(totalPrice)
        }

        var totalPrice = 0L
        for (productInfo in command.productInfos) {
            val product = productRepository.findById(productInfo.productId).orElseThrow()

            product.buy(productInfo.quantity)
            val price = product.calculatePrice(productInfo.quantity)
            totalPrice += price

            productTransactionHistoryRepository.save(
                ProductTransactionHistory(
                    command.requestId,
                    productInfo.productId,
                    productInfo.quantity,
                    price,
                    ProductTransactionHistory.TransactionType.PURCHASE,
                ),
            )
        }

        return ProductBuyResult(totalPrice)
    }

    @Transactional
    fun cancel(command: ProductBuyCancelCommand): ProductBuyCancelResult {
        val buyHistories = productTransactionHistoryRepository.findAllByRequestIdAndTransactionType(
            command.requestId,
            ProductTransactionHistory.TransactionType.PURCHASE,
        )

        // 구매 이력이 없으면 보상할 것도 없다.
        if (buyHistories.isEmpty()) {
            return ProductBuyCancelResult(0L)
        }

        val cancelHistories = productTransactionHistoryRepository.findAllByRequestIdAndTransactionType(
            command.requestId,
            ProductTransactionHistory.TransactionType.CANCEL,
        )

        // 이미 취소(보상)했다면 멱등하게 기존 결과를 돌려준다.
        if (cancelHistories.isNotEmpty()) {
            println("이미 취소되었습니다.")
            val totalPrice = cancelHistories.sumOf { it.price }
            return ProductBuyCancelResult(totalPrice)
        }

        var totalPrice = 0L
        for (history in buyHistories) {
            val product = productRepository.findById(history.productId).orElseThrow()

            product.cancel(history.quantity)
            totalPrice += history.price

            productTransactionHistoryRepository.save(
                ProductTransactionHistory(
                    command.requestId,
                    history.productId,
                    history.quantity,
                    history.price,
                    ProductTransactionHistory.TransactionType.CANCEL,
                ),
            )
        }

        return ProductBuyCancelResult(totalPrice)
    }
}
