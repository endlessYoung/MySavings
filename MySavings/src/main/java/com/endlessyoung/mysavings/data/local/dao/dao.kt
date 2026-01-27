package com.endlessyoung.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.endlessyoung.mysavings.data.local.entity.FundEntity
import com.endlessyoung.mysavings.data.local.entity.PlanEntity
import com.endlessyoung.mysavings.data.local.entity.SavingEntity
import kotlinx.coroutines.flow.Flow

interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T)

    @Update
    suspend fun delete(entity: T)

    @Delete
    suspend fun update(entity: T)

}

@Dao
interface SavingDao: BaseDao<SavingEntity> {
    @Query("SELECT * FROM saving ORDER BY endTime ASC")
    fun observeAll(): Flow<List<SavingEntity>>

    @Query("DELETE FROM saving WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FundDao: BaseDao<FundEntity> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(fund: FundEntity)

    @Query("SELECT * FROM fund_table")
    fun observeAll(): Flow<List<FundEntity>>

    // 注意：如果你把 ownerName 改为主键，这里的删除逻辑也要对应修改
    @Query("DELETE FROM fund_table WHERE ownerName = :name")
    suspend fun deleteByName(name: String)
}

@Dao
interface PlanDao: BaseDao<PlanEntity> {
    @Query("SELECT * FROM expenditure_plan ORDER BY planDate ASC")
    fun observeAll(): Flow<List<PlanEntity>>

    @Query("DELETE FROM expenditure_plan WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenditure_plan WHERE planDate >= :now AND isCompleted = 0 LIMIT 1")
    fun observeNextUpcoming(now: Long = System.currentTimeMillis()): Flow<PlanEntity?>
}