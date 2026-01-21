package com.endlessyoung.mysavings.domain.model

import com.endlessyoung.mysavings.data.local.entity.SavingEntity
import java.math.BigDecimal

data class SavingItem(
    val id: Long = 0L,
    val bankName: String,
    val amount: BigDecimal,
    val year: Int,
    val interestRate: BigDecimal,
    val interest: BigDecimal,
    val startTime: Long,
    val endTime: Long
)

fun SavingItem.toEntity(): SavingEntity =
    SavingEntity(
        id = id,
        bankName = bankName,
        amount = amount,
        interestRate = interestRate,
        interest = interest,
        year = year,
        startTime = startTime,
        endTime = endTime
    )

fun SavingEntity.toDomain(): SavingItem =
    SavingItem(
        id = id,
        bankName = bankName,
        amount = amount,
        interestRate = interestRate,
        interest = interest,
        startTime = startTime,
        endTime = endTime,
        year = year
    )
