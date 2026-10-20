package com.dosecare.app.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dosecare.app.data.repository.LegacyWidgetAppearance
import com.dosecare.app.data.repository.SettingsRepository
import com.dosecare.app.model.settings.DarkModeOption
import com.dosecare.app.testing.InMemoryPreferencesDataStore
import com.dosecare.app.util.appLanguageLocale
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Locale

// Behavioral coverage of WidgetAppearanceRepository.migrateFromLegacySettingsIfNeeded
// over real Preferences DataStores: the one-time seed must never clobber an existing
// default (e.g. a restored backup), must clear the legacy keys, and must still report
// true when the clear fails so the caller repaints and the next start retries.
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetAppearanceRepositoryMigrationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val widgetContentScaleKey = floatPreferencesKey("widget_content_scale")
    private val widgetBackgroundAlphaKey = floatPreferencesKey("widget_background_alpha")
    private val widgetDarkModeKey = stringPreferencesKey("widget_dark_mode")

    private lateinit var settingsDataStore: DataStore<Preferences>
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var store: WidgetAppearanceStore
    private lateinit var repository: WidgetAppearanceRepository

    @Before
    fun setUp() {
        mockkStatic("com.dosecare.app.util.LocalizationKt")
        every { appLanguageLocale() } returns Locale.ENGLISH
        val context: Context = mockk()

        settingsDataStore = InMemoryPreferencesDataStore()
        settingsRepository = SettingsRepository(context, settingsDataStore)

        // 内存版存储: Windows 上文件版 DataStore 会被常驻读句柄锁住, 写不进去。
        val widgetStorage = InMemoryPreferencesDataStore()
        store = WidgetAppearanceStore(widgetContext, widgetStorage)
        repository = WidgetAppearanceRepository(store, settingsRepository, mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        unmockkStatic("com.dosecare.app.util.LocalizationKt")
    }

    @Test
    fun `legacy keys seed the empty store default, get cleared, and report true`() =
        runTest(testDispatcher) {
            settingsDataStore.edit {
                it[widgetContentScaleKey] = 1.3f
                it[widgetBackgroundAlphaKey] = 0.8f
                it[widgetDarkModeKey] = "DARK"
            }

            assertTrue(repository.migrateFromLegacySettingsIfNeeded())

            assertEquals(
                legacyWidgetAppearance(1.3f, 0.8f, DarkModeOption.DARK),
                store.observeEntry(null).first(),
            )
            assertNull(settingsRepository.readLegacyWidgetAppearance())
        }

    @Test
    fun `existing store default is not clobbered but legacy keys are still cleared`() =
        runTest(testDispatcher) {
            // E.g. a backup restore already wrote a default before migration ran.
            val restored = WidgetAppearance.Default.copy(seedHue = 200f)
            store.setEntry(null, restored)
            settingsDataStore.edit { it[widgetDarkModeKey] = "DARK" }

            assertTrue(repository.migrateFromLegacySettingsIfNeeded())

            assertEquals(restored, store.observeEntry(null).first())
            assertNull(settingsRepository.readLegacyWidgetAppearance())
        }

    @Test
    fun `no legacy keys reports false and leaves the store untouched`() =
        runTest(testDispatcher) {
            assertFalse(repository.migrateFromLegacySettingsIfNeeded())

            assertNull(store.observeEntry(null).first())
        }

    @Test
    fun `failed legacy clear still reports true, keys survive, next start retries`() =
        runTest(testDispatcher) {
            settingsDataStore.edit { it[widgetDarkModeKey] = "DARK" }
            val failingSettings = spyk(settingsRepository)
            coEvery {
                failingSettings.clearLegacyWidgetAppearanceKeys()
            } throws IOException("injected clear failure")
            val firstStart =
                WidgetAppearanceRepository(store, failingSettings, mockk(relaxed = true))

            // The seed landed, so the caller must still repaint (true) even though
            // the clear failed; the surviving keys make the next start retry.
            assertTrue(firstStart.migrateFromLegacySettingsIfNeeded())
            val seeded = store.observeEntry(null).first()
            assertEquals(legacyWidgetAppearance(1.0f, 1.0f, DarkModeOption.DARK), seeded)
            assertEquals(
                LegacyWidgetAppearance(1.0f, 1.0f, DarkModeOption.DARK),
                settingsRepository.readLegacyWidgetAppearance(),
            )

            // Second start: seed no-ops, clear succeeds, migration goes quiet.
            assertTrue(repository.migrateFromLegacySettingsIfNeeded())
            assertEquals(seeded, store.observeEntry(null).first())
            assertNull(settingsRepository.readLegacyWidgetAppearance())
            assertFalse(repository.migrateFromLegacySettingsIfNeeded())
        }

    private companion object {
        val widgetContext: Context = mockk<Context>().also {
            every { it.applicationContext } returns it
        }
    }
}
