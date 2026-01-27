package com.endlessyoung.mysavings.domain.model

import com.endlessyoung.mysavings.data.local.entity.FundEntity
import java.math.BigDecimal

data class ProvidentFund(
    val ownerName: String,
    val totalBalance: BigDecimal,
    val monthlyAmount: BigDecimal,
    val lastUpdateTime: Long
)

fun FundEntity.toDomain() = ProvidentFund(
    ownerName = ownerName,
    totalBalance = totalBalance,
    monthlyAmount = monthlyAmount,
    lastUpdateTime = lastUpdateTime
)

fun ProvidentFund.toEntity() = FundEntity(
    ownerName = ownerName,
    totalBalance = totalBalance,
    monthlyAmount = monthlyAmount,
    lastUpdateTime = lastUpdateTime
)


