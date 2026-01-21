package com.endlessyoung.mysavings.data.repository

import com.endlessyoung.mysavings.data.local.dao.SavingDao
import com.endlessyoung.mysavings.data.local.entity.SavingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class SavingRepository(private val dao: SavingDao) {
    val allSavings: Flow<List<SavingEntity>>
        get() = dao.observeAll()

    suspend fun insert(entity: SavingEntity) {
        dao.insert(entity)
    }

    suspend fun delete(entity: SavingEntity) {
        dao.deleteById(entity.id)
    }

    suspend fun update(entity: SavingEntity) {
        dao.update(entity)
    }

    fun getTotalAmount(): Flow<BigDecimal> {
        return allSavings.map { list ->
            list.fold(BigDecimal.ZERO) { acc, entity ->
                acc.add(entity.amount).add(entity.interest)
            }
        }
    }
}