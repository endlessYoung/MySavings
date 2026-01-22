package com.endlessyoung.mysavings.domain.model

import com.endlessyoung.mysavings.data.local.entity.FundEntity
import com.endlessyoung.mysavings.data.local.entity.PlanEntity
import java.math.BigDecimal

data class ExpenditurePlan(
    val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val planDate: Long,
    val isRecurring: Boolean,
    val isCompleted: Boolean = false
)

fun ExpenditurePlan.toEntity() = PlanEntity(
    id = id,
    title = title,
    amount = amount,
    planDate = planDate,
    isRecurring = isRecurring,
    isCompleted = isCompleted
)

fun PlanEntity.toDomain() = ExpenditurePlan(
    id = id,
    title = title,
    amount = amount,
    planDate = planDate,
    isRecurring = isRecurring,
    isCompleted = isCompleted
)
