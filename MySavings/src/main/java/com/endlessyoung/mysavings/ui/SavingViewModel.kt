package com.endlessyoung.mysavings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.endlessyoung.mysavings.data.local.db.AppDatabase
import com.endlessyoung.mysavings.data.local.entity.SavingEntity
import com.endlessyoung.mysavings.data.repository.SavingRepository
import com.endlessyoung.mysavings.domain.model.SavingItem
import com.endlessyoung.mysavings.domain.model.toDomain
import com.endlessyoung.mysavings.domain.model.toEntity
import com.endlessyoung.mysavings.log.MySavingsLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

class SavingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SavingRepository(AppDatabase.get(app).savingDao())

    val savings: StateFlow<List<SavingItem>> = repo.allSavings
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAmount: StateFlow<BigDecimal> = repo.getTotalAmount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BigDecimal.ZERO
        )

    val bankDistribution: Flow<Map<String, BigDecimal>> = repo.allSavings.map { list ->
        list.groupBy { it.bankName }
            .mapValues { entry ->
                entry.value.fold(BigDecimal.ZERO) { acc, entity ->
                    acc.add(entity.amount).add(entity.interest)
                }
            }
    }

    val yearlySavings: Flow<Map<Int, BigDecimal>> = repo.allSavings.map { list ->
        val calendar = Calendar.getInstance()
        list.groupBy {
            calendar.timeInMillis = it.startTime
            calendar.get(Calendar.YEAR)
        }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }.toSortedMap()
    }

    private val calendar = Calendar.getInstance()

    val availableYears: StateFlow<List<Int>> = repo.allSavings
        .map { list ->
            list.map {
                calendar.timeInMillis = it.startTime
                calendar.get(Calendar.YEAR)
            }.distinct().sortedDescending()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val filterYear: StateFlow<Int> = _filterYear

    fun setFilterYear(year: Int) {
        _filterYear.value = year
    }

    val monthlySavingsByYear: Flow<Map<Int, BigDecimal>> =
        combine(repo.allSavings, _filterYear) { list, year ->
            val fullYearMap = (1..12).associateWith { BigDecimal.ZERO }.toMutableMap()

            val localCalendar = Calendar.getInstance()

            val actualData = list.filter { record ->
                localCalendar.timeInMillis = record.startTime
                localCalendar.get(Calendar.YEAR) == year
            }.groupBy { record ->
                localCalendar.timeInMillis = record.startTime
                localCalendar.get(Calendar.MONTH) + 1
            }.mapValues { entry ->
                entry.value.fold(BigDecimal.ZERO) { acc, record -> acc.add(record.amount) }
            }

            fullYearMap.putAll(actualData)
            fullYearMap.toSortedMap()
        }

    fun getGroupedSavings(year: Int, month: Int): Flow<Map<String, List<SavingItem>>> {
        return repo.allSavings.map { entities ->
            val cal = Calendar.getInstance()
            entities.map { entity ->
                entity.toDomain()
            }.filter { item ->
                cal.timeInMillis = item.startTime
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                if (month == -1) y == year else (y == year && m == month)
            }
                .sortedByDescending { it.startTime }
                .groupBy { item ->
                    cal.timeInMillis = item.startTime
                    if (month == -1) {
                        "${cal.get(Calendar.MONTH) + 1}月"
                    } else {
                        "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
                    }
                }
        }
    }

    fun insert(item: SavingItem) = viewModelScope.launch { repo.insert(item.toEntity()) }
    fun delete(item: SavingItem) = viewModelScope.launch { repo.delete(item.toEntity()) }
    fun update(item: SavingItem) = viewModelScope.launch { repo.update(item.toEntity()) }

}
