package com.dosecare.app

import android.app.Application
import android.util.Log
import com.dosecare.app.domain.catalog.DrugCatalogLoader
import com.dosecare.app.domain.catalog.DrugCatalogService
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 类 (v0.7 Hilt + Room 时代)
 *
 * v0.7 升级:
 * - 加 @HiltAndroidApp 启动 Hilt DI
 * - DrugCatalog 由 Room DAO 在 Hilt 注入层异步加载 (见 DatabaseModule)
 * - 此处保留 JSON 资产读取逻辑作为灌库入口 (DataSeeder 在 IO dispatcher 跑)
 *
 * v0.4 之前:
 * - 启动时同步加载 DrugCatalog 到单例
 * - MainActivity 从这里取
 */
@HiltAndroidApp
class DoseCareApp : Application() {

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
