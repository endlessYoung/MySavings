package com.endlessyoung.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "expenditure_plan")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val planDate: Long,
    val isRecurring: Boolean,
    val isCompleted: Boolean = false
)