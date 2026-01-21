package com.endlessyoung.mysavings.data.local.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.endlessyoung.mysavings.data.local.entity.SavingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingDao {
    companion object {
        private const val TAG = "SavingDao"
    }

    @Query("SELECT * FROM saving ORDER BY endTime ASC")
    fun observeAll(): Flow<List<SavingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavingEntity)

    @Delete
    suspend fun delete(entity: SavingEntity)

    @Query("DELETE FROM saving WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(entity: SavingEntity)
}