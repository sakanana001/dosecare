package com.dosecare.app.di

import android.content.Context
import androidx.room.Room
import com.dosecare.app.data.crypto.DbKeyProvider
import com.dosecare.app.data.db.AppDatabase
import com.dosecare.app.data.db.DataSeeder
import com.dosecare.app.data.repository.DiaryRepository
import com.dosecare.app.data.repository.DrugCatalogRepository
import com.dosecare.app.domain.catalog.DrugCatalogService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * Hilt DatabaseModule (v0.7 + v0.8a)
 *
 * v0.7 Phase A: 提供 AppDatabase (空 schema, SQLCipher 加密)
 *               + DrugCatalogService 仍然从 JSON 加载
 * v0.7 Phase B: 灌库 (DataSeeder) + DrugCatalogService 改从 DAO 拼回
 * v0.7 Phase C: 加用户表 Dao 注入
 * v0.8a Phase F: 加 diary_entry 表 + DiaryRepository
 *
 * Phase B 关键: DrugCatalogService 在 Hilt 单例构造时同步灌库 + 加载
 *   - 首次启动: ~500ms 灌 1500 行
 *   - 后续启动: ~50ms 跳 (metadata version 匹配)
 *   - 阻塞主线程可接受 (Application.onCreate 阶段, UI 还没起)
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keyProvider: DbKeyProvider
    ): AppDatabase {
        // 加载 SQLCipher native libs (必须在第一次 openHelper 之前)
        System.loadLibrary("sqlcipher")
        val passphrase = keyProvider.getOrCreatePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(AppDatabase.MIGRATION_4_5)  // v0.9g: prescribed_drug 加 times + target_date
            .build()
    }

    /**
     * v0.7 Phase B: DrugCatalogService 走 Room
     * - 首次启动: 同步灌库 (DataSeeder.seedIfNeeded) + 加载 → 拼成 DrugCatalogService
     * - 后续启动: 跳过灌库, 直接加载
     */
    @Provides
    @Singleton
    fun provideDrugCatalogService(
        @ApplicationContext context: Context,
        db: AppDatabase,
        repository: DrugCatalogRepository
    ): DrugCatalogService {
        return runBlocking {
            // 灌库 (如果需要)
            DataSeeder.seedIfNeeded(context, db)
            // 迁移老 SharedPreferences 处方 (Phase C)
            com.dosecare.app.data.migration.PrescriptionMigrator.migrateIfNeeded(
                context,
                db.prescriptionGroupDao(),
                db.prescribedDrugDao(),
                db.doseTakenDao(),
                db.userPreferenceDao()
            )
            // 加载 (从 DB 拼回 Drug 对象)
            repository.loadFromDb()
        }
    }

    /**
     * v0.8a Phase F: 日记表注入
     */
    @Provides
    @Singleton
    fun provideDiaryRepository(db: AppDatabase): DiaryRepository =
        DiaryRepository(db.diaryDao())
}
