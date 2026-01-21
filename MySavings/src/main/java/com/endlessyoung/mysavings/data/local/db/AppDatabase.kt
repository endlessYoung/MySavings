package com.endlessyoung.mysavings.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.endlessyoung.mysavings.data.local.Converters
import com.endlessyoung.mysavings.data.local.dao.SavingDao
import com.endlessyoung.mysavings.data.local.entity.SavingEntity

@Database(
    entities = [SavingEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun savingDao(): SavingDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saving.db"
                ).fallbackToDestructiveMigration()
                 .build().also { INSTANCE = it }
            }
    }
}