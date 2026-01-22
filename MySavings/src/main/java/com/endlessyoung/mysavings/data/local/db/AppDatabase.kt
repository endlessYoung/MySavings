package com.endlessyoung.mysavings.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.endlessyoung.mysavings.data.local.Converters
import com.endlessyoung.mysavings.data.local.dao.FundDao
import com.endlessyoung.mysavings.data.local.dao.PlanDao
import com.endlessyoung.mysavings.data.local.dao.SavingDao
import com.endlessyoung.mysavings.data.local.entity.FundEntity
import com.endlessyoung.mysavings.data.local.entity.PlanEntity
import com.endlessyoung.mysavings.data.local.entity.SavingEntity

@Database(
    // 补全所有实体类，否则 DAO 无法绑定
    entities = [
        SavingEntity::class,
        FundEntity::class,
        PlanEntity::class
    ],
    version = 1, // 保持版本 1，需手动卸载旧 App 再安装
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savingDao(): SavingDao
    abstract fun fundDao(): FundDao
    abstract fun planDao(): PlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_saving.db"
                ).build().also { INSTANCE = it }
            }
    }
}