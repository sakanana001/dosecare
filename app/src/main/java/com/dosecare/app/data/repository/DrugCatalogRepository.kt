package com.dosecare.app.data.repository

import com.dosecare.app.data.db.AppDatabase
import com.dosecare.app.data.db.DrugEntityMapper
import com.dosecare.app.domain.catalog.Metabolite
import com.dosecare.app.domain.catalog.OverdoseInfo
import com.dosecare.app.domain.catalog.OverdoseSeverity
import com.dosecare.app.domain.catalog.DrugCatalogService
import com.dosecare.app.domain.pk.PkModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DrugCatalogRepository (v0.7 Phase B)
 *
 * 把 Room DB 的 11 张表拼回 DrugCatalogService (内存 API, UI 不用改)
 *
 * 灌库入口在 DatabaseModule: 调用 DataSeeder.seedIfNeeded(context, db) 同步跑
 * 然后 repository.loadFromDb() 读 DB 拼 Drug
 *
 * v0.7 同步灌库 (runBlocking in Hilt @Singleton init)
 * v0.8 计划: 异步 + 启动屏, 加 Flow<DrugCatalogService> 响应式订阅
 */
@Singleton
class DrugCatalogRepository @Inject constructor(
    private val db: AppDatabase
) {
    /**
     * 从 DB 加载 → DrugCatalogService
     * 调用方: Hilt Provider (DatabaseModule.provideDrugCatalogService)
     */
    suspend fun loadFromDb(): DrugCatalogService {
        val drugEntities = db.drugDao().getAll()
        val drugs = drugEntities.map { d ->
            DrugEntityMapper.toDomain(
                drug = d,
                brands = db.drugBrandDao().getByDrugId(d.id),
                indications = db.drugIndicationDao().getByDrugId(d.id),
                metabolites = db.drugActiveMetaboliteDao().getByDrugId(d.id).map {
                    Metabolite(
                        id = it.metaboliteId,
                        name = it.name,
                        activityRatio = it.activityRatio,
                        note = it.note
                    )
                },
                pk = db.drugPkDao().getByDrugId(d.id),
                window = db.drugTherapeuticWindowDao().getByDrugId(d.id)?.let { w ->
                    PkModel.TherapeuticWindow(
                        low = w.low, high = w.high, unit = w.unit, guidelineSource = w.guidelineSource
                    )
                },
                overdose = db.drugOverdoseDao().getByDrugId(d.id)?.let { o ->
                    OverdoseInfo(
                        symptoms = o.symptoms,
                        severity = OverdoseSeverity.valueOf(o.severity),
                        toxicDoseEstimateMg = o.toxicDoseMg,
                        fatalDoseEstimateMg = o.fatalDoseMg,
                        management = o.management,
                        antidote = o.antidote,
                        dataSource = o.dataSource
                    )
                },
                clinical = db.drugClinicalDao().getByDrugId(d.id)
                    ?: error("Drug ${d.id} missing clinical row"),
                cypRoles = db.cypRoleDao().getByDrugId(d.id),
                criticalInteractions = db.drugCriticalInteractionDao().getByDrugId(d.id)
            )
        }
        return DrugCatalogService(drugs)
    }
}
