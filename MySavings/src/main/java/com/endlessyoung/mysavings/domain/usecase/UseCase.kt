package com.endlessyoung.mysavings.domain.usecase

import com.endlessyoung.mysavings.data.repository.FundRepository
import com.endlessyoung.mysavings.data.repository.SavingRepository
import com.endlessyoung.mysavings.domain.model.SavingItem
import com.endlessyoung.mysavings.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.Calendar

class GetTotalAssetsUseCase(
    private val savingRepo: SavingRepository,
    private val fundRepo: FundRepository
) {
    operator fun invoke(): Flow<BigDecimal> {
        return combine(
            savingRepo.getTotalAmount(),
            fundRepo.getTotalFundAmount()
        ) { savings, funds -> savings.add(funds) }
    }
}

class GetGroupedSavingsUseCase(private val savingRepo: SavingRepository) {
    operator fun invoke(year: Int, month: Int): Flow<Map<String, List<SavingItem>>> {
        return savingRepo.allSavings.map { entities ->
            val cal = Calendar.getInstance()
            entities.map { it.toDomain() }
                .filter { item ->
                    cal.timeInMillis = item.startTime
                    val y = cal.get(Calendar.YEAR)
                    val m = cal.get(Calendar.MONTH) + 1
                    if (month == -1) y == year else (y == year && m == month)
                }
                .sortedByDescending { it.startTime }
                .groupBy { item ->
                    cal.timeInMillis = item.startTime
                    if (month == -1) "${cal.get(Calendar.MONTH) + 1}月"
                    else "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
                }
        }
    }
}