package com.endlessyoung.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "saving")
data class SavingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String,
    val amount: BigDecimal,
    val year: Int,
    val interestRate: BigDecimal,
    val interest: BigDecimal,
    val startTime: Long,
    val endTime: Long
)
