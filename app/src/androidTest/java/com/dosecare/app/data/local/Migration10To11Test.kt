package com.dosecare.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

/**
 * v0.9g merge: `medicines.drugCatalogId` arrives as v10 -> v11.
 *
 * The load-bearing property is the default. Every medicine row that existed before the merge
 * has no pharmacological dossier, so the migration must leave it reading back as "" rather
 * than NULL or a failure — Room's NOT NULL column would reject a null default, and a
 * non-empty default would silently claim a dossier link that was never made.
 */
@RunWith(AndroidJUnit4::class)
class Migration10To11Test {
    @get:Rule val testName: TestName = TestName()
    private val testDb get() = "migration-test-db-${testName.methodName}"

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(testDb)
    }

    @Test
    fun migrate10To11_addsDrugCatalogIdDefaultingToEmpty() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(testDb)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(testDb)
                .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val db = helper.writableDatabase
        try {
            // Minimal `medicines` table matching the v10 shape (no drugCatalogId yet).
            db.execSQL(
                """
                CREATE TABLE medicines (
                    uuid TEXT NOT NULL PRIMARY KEY,
                    selectionKind TEXT NOT NULL,
                    identityKey TEXT NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT INTO medicines (uuid, selectionKind, identityKey, createdAtEpochMillis) " +
                    "VALUES ('m1', 'CUSTOM', 'X|estradiol|PILL', 1000)"
            )
            db.execSQL(
                "INSERT INTO medicines (uuid, selectionKind, identityKey, createdAtEpochMillis) " +
                    "VALUES ('m2', 'CATALOG', 'C|ESTRADIOL|PATCH', 2000)"
            )
            db.version = 10

            MIGRATION_10_11.migrate(db)
            db.version = 11

            assertTrue(
                "drugCatalogId column should exist after migration",
                columnExists(db, "medicines", "drugCatalogId"),
            )

            // Pre-existing rows must read back as "no dossier", not null and not a stray id.
            db.query("SELECT uuid, drugCatalogId FROM medicines ORDER BY uuid").use { cursor ->
                assertEquals(2, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("m1", cursor.getString(0))
                assertEquals("", cursor.getString(1))
                assertTrue(cursor.isNull(1).not())
                assertTrue(cursor.moveToNext())
                assertEquals("m2", cursor.getString(0))
                assertEquals("", cursor.getString(1))
            }

            // And the column must be writable, so the phase-2 picker can populate it.
            db.execSQL("UPDATE medicines SET drugCatalogId = 'estradiol_valerate' WHERE uuid = 'm1'")
            db.query("SELECT drugCatalogId FROM medicines WHERE uuid = 'm1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("estradiol_valerate", cursor.getString(0))
            }
        } finally {
            db.close()
            helper.close()
        }
    }

    private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }
}
