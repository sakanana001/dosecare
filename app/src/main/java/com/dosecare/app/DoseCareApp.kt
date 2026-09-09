package com.dosecare.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dosecare.app.domain.catalog.DrugCatalogLoader
import com.dosecare.app.domain.catalog.DrugCatalogService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application 类 (v0.7 Hilt + Room 时代)
 *
 * v0.7 升级:
 * - 加 @HiltAndroidApp 启动 Hilt DI
 * - DrugCatalog 由 Room DAO 在 Hilt 注入层异步加载 (见 DatabaseModule)
 * - 此处保留 JSON 资产读取逻辑作为灌库入口 (DataSeeder 在 IO dispatcher 跑)
 *
 * v0.9f 升级:
 * - 实现 Configuration.Provider 接口, 让 WorkManager 使用 HiltWorkerFactory
 * - 这样 @HiltWorker 注解的 Worker 可以注入 Repository 等依赖
 */
@HiltAndroidApp
class DoseCareApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        val start = System.currentTimeMillis()
        Log.i(TAG, "DoseCare started in ${System.currentTimeMillis() - start}ms (catalog loading deferred to Hilt module)")
    }

    companion object {
        private const val TAG = "DoseCareApp"
        const val DRUG_CATALOG_ASSET = "drugs/v0.6.json"
    }
}
