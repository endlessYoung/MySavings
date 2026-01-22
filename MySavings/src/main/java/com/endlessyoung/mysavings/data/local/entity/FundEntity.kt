package com.endlessyoung.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "provident_fund")
data class FundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerName: String,
    val totalBalance: BigDecimal,
    val monthlyAmount: BigDecimal,
    val lastUpdateTime: Long
)