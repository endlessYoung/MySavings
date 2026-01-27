package com.endlessyoung.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "fund_table")
data class FundEntity(
    @PrimaryKey
    val ownerName: String,
    val totalBalance: BigDecimal,
    val monthlyAmount: BigDecimal,
    val lastUpdateTime: Long
)