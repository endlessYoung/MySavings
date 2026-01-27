package com.endlessyoung.mysavings.data.repository

import com.endlessyoung.mysavings.data.local.dao.BaseDao
import com.endlessyoung.mysavings.data.local.dao.FundDao
import com.endlessyoung.mysavings.data.local.dao.PlanDao
import com.endlessyoung.mysavings.data.local.dao.SavingDao
import com.endlessyoung.mysavings.data.local.entity.FundEntity
import com.endlessyoung.mysavings.data.local.entity.PlanEntity
import com.endlessyoung.mysavings.data.local.entity.SavingEntity
import com.endlessyoung.mysavings.log.MySavingsLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

abstract class BaseRepository<Entity>(
    private val dao: BaseDao<Entity>
) {
    open suspend fun insert(entity: Entity) = dao.insert(entity)
    open suspend fun update(entity: Entity) = dao.update(entity)
    open suspend fun delete(entity: Entity) = dao.delete(entity)
}

class SavingRepository(private val dao: SavingDao) : BaseRepository<SavingEntity>(
    dao = dao
) {
    val allSavings: Flow<List<SavingEntity>>
        get() = dao.observeAll()

    override suspend fun insert(entity: SavingEntity) {
        dao.insert(entity)
    }

    override suspend fun delete(entity: SavingEntity) {
        dao.deleteById(entity.id)
    }

    override suspend fun update(entity: SavingEntity) {
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

class FundRepository(private val dao: FundDao) : BaseRepository<FundEntity>(dao) {
    val allFunds: Flow<List<FundEntity>>
        get() = dao.observeAll()

    suspend fun deleteByName(name: String) {
        dao.deleteByName(name)
    }

    suspend fun upsert(fund: FundEntity) {
        dao.insertOrUpdate(fund)
    }

    fun getTotalFundAmount(): Flow<BigDecimal> {
        return allFunds.map { list ->
            list.fold(BigDecimal.ZERO) { acc, entity ->
                acc.add(entity.totalBalance)
            }
        }
    }
}

class PlanRepository(private val dao: PlanDao) : BaseRepository<PlanEntity>(dao) {
    val allPlans: Flow<List<PlanEntity>>
        get() = dao.observeAll()

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    fun observeNextUpcoming(now: Long = System.currentTimeMillis()): Flow<PlanEntity?> {
        return dao.observeNextUpcoming(now)
    }
}