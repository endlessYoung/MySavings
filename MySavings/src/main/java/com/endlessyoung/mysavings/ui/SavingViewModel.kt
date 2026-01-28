package com.endlessyoung.mysavings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.endlessyoung.mysavings.data.local.db.AppDatabase
import com.endlessyoung.mysavings.data.local.entity.FundEntity
import com.endlessyoung.mysavings.data.local.entity.PlanEntity
import com.endlessyoung.mysavings.data.repository.FundRepository
import com.endlessyoung.mysavings.data.repository.PlanRepository
import com.endlessyoung.mysavings.data.repository.SavingRepository
import com.endlessyoung.mysavings.domain.model.SavingItem
import com.endlessyoung.mysavings.domain.model.toDomain
import com.endlessyoung.mysavings.domain.model.toEntity
import com.endlessyoung.mysavings.domain.usecase.GetGroupedSavingsUseCase
import com.endlessyoung.mysavings.domain.usecase.GetTotalAssetsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

enum class SavingSortMode {
    DEFAULT,       // 按开始时间倒序
    AMOUNT_DESC,   // 按金额倒序
    RATE_DESC      // 按利率倒序
}

class SavingViewModel(app: Application) : AndroidViewModel(app) {
    private val database = AppDatabase.get(app)
    private val savingRepo = SavingRepository(database.savingDao())
    private val fundRepo = FundRepository(database.fundDao())
    private val planRepo = PlanRepository(database.planDao())

    private val getTotalAssetsWorthUseCase = GetTotalAssetsUseCase(savingRepo, fundRepo)
    private val getGroupedSavingsUseCase = GetGroupedSavingsUseCase(savingRepo)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortMode = MutableStateFlow(SavingSortMode.DEFAULT)
    val sortMode: StateFlow<SavingSortMode> = _sortMode

    private val sourceSavings: Flow<List<SavingItem>> = savingRepo.allSavings
        .map { list -> list.map { it.toDomain() } }

    val savings: StateFlow<List<SavingItem>> = sourceSavings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSavings: StateFlow<List<SavingItem>> =
        combine(sourceSavings, _searchQuery, _sortMode) { list, query, mode ->
            var result = list

            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                result = result.filter { item ->
                    item.bankName.lowercase().contains(q) ||
                            item.amount.toPlainString().contains(q) ||
                            item.year.toString().contains(q)
                }
            }

            result = when (mode) {
                SavingSortMode.DEFAULT -> result.sortedByDescending { it.startTime }
                SavingSortMode.AMOUNT_DESC -> result.sortedByDescending { it.amount }
                SavingSortMode.RATE_DESC -> result.sortedByDescending { it.interestRate }
            }

            result
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAssets: StateFlow<BigDecimal> = getTotalAssetsWorthUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BigDecimal.ZERO
        )

    val totalFundAmount: StateFlow<BigDecimal> = fundRepo.getTotalFundAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    val totalSavingAmount: StateFlow<BigDecimal> = savingRepo.getTotalAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    val bankDistribution: Flow<Map<String, BigDecimal>> = savingRepo.allSavings.map { list ->
        list.groupBy { it.bankName }
            .mapValues { entry ->
                entry.value.fold(BigDecimal.ZERO) { acc, entity ->
                    acc.add(entity.amount).add(entity.interest)
                }
            }
    }

    val upcomingPlan = planRepo.observeNextUpcoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPlans = planRepo.allPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val yearlySavings: Flow<Map<Int, BigDecimal>> = savingRepo.allSavings.map { list ->
        val calendar = Calendar.getInstance()
        list.groupBy {
            calendar.timeInMillis = it.startTime
            calendar.get(Calendar.YEAR)
        }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }.toSortedMap()
    }

    private val calendar = Calendar.getInstance()

    val availableYears: StateFlow<List<Int>> = savingRepo.allSavings
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

    fun setSearchQuery(keyword: String) {
        _searchQuery.value = keyword
    }

    fun setSortMode(mode: SavingSortMode) {
        _sortMode.value = mode
    }

    val monthlySavingsByYear: Flow<Map<Int, BigDecimal>> =
        combine(savingRepo.allSavings, _filterYear) { list, year ->
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

    fun getGroupedSavings(year: Int, month: Int) = getGroupedSavingsUseCase(year, month)

    // 存款操作
    fun insertSaving(item: SavingItem) = viewModelScope.launch { savingRepo.insert(item.toEntity()) }
    fun deleteSaving(item: SavingItem) = viewModelScope.launch { savingRepo.delete(item.toEntity()) }
    fun updateSaving(item: SavingItem) = viewModelScope.launch { savingRepo.update(item.toEntity()) }

    // 公积金操作
    fun updateFund(entity: FundEntity) = viewModelScope.launch { fundRepo.upsert(entity) }
    fun deleteFund(name: String) = viewModelScope.launch { fundRepo.deleteByName(name) }

    // 计划操作
    fun insertPlan(entity: PlanEntity) = viewModelScope.launch { planRepo.insert(entity) }
    fun deletePlan(id: Long) = viewModelScope.launch { planRepo.deleteById(id) }

}
