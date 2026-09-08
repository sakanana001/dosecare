package com.dosecare.app.domain.prescription

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 我的处方 - SharedPreferences 持久化
 *
 * v0.6 加 migration: v0.5 存的 dosesTaken 是 List<Long>, v0.6 改成 List<DoseTaken>
 * 加载时检测 → 自动转换
 */
class PrescriptionRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dosecare_prescription", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun loadAll(): List<PrescriptionGroup> {
        val raw = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        return runCatching {
            val arr = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
            arr.map { el -> migrateGroup(el) }
        }.getOrDefault(emptyList())
    }

    /**
     * v0.5 → v0.6 migration: dosesTaken 从 [long, long, ...] 转 [{timestamp:long, doseMg:100, note:null}, ...]
     */
    private fun migrateGroup(el: JsonElement): PrescriptionGroup {
        val obj = el as? JsonObject ?: return runCatching {
            json.decodeFromJsonElement(PrescriptionGroup.serializer(), el)
        }.getOrNull() ?: return PrescriptionGroup(name = "?", colorIndex = 0)
        val newObj = JsonObject(obj.toMutableMap().apply {
            (this["drugs"] as? JsonArray)?.let { drugsArr ->
                val newDrugsEl = kotlinx.serialization.json.buildJsonArray {
                    drugsArr.forEach { drugEl ->
                        val dObj = drugEl as? JsonObject
                        if (dObj == null) {
                            add(drugEl)
                            return@forEach
                        }
                        val dosesTaken = dObj["dosesTaken"] as? JsonArray
                        if (dosesTaken == null || dosesTaken.isEmpty()) {
                            add(drugEl)
                            return@forEach
                        }
                        val first = dosesTaken.first()
                        if (first is JsonPrimitive && first.isString.not() && first.content.toLongOrNull() != null) {
                            // v0.5 格式: 纯 Long 列表 → 转 DoseTaken 列表
                            val migrated = kotlinx.serialization.json.buildJsonArray {
                                dosesTaken.forEach { ts ->
                                    add(kotlinx.serialization.json.buildJsonObject {
                                        put("timestamp", ts)
                                        put("doseMg", JsonPrimitive(100.0))
                                        put("note", JsonNull)
                                    })
                                }
                            }
                            add(JsonObject(dObj.toMutableMap().apply { put("dosesTaken", migrated) }))
                        } else {
                            add(drugEl)
                        }
                    }
                }
                put("drugs", newDrugsEl)
            }
        })
        return json.decodeFromJsonElement(PrescriptionGroup.serializer(), newObj)
    }

    fun saveAll(groups: List<PrescriptionGroup>) {
        val raw = json.encodeToString(ListSerializer(PrescriptionGroup.serializer()), groups)
        prefs.edit().putString(KEY_GROUPS, raw).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_GROUPS).apply()
    }

    companion object {
        private const val KEY_GROUPS = "groups_json"
    }
}
