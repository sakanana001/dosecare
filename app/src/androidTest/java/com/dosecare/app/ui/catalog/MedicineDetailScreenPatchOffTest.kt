package com.dosecare.app.ui.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.dosecare.app.R
import com.dosecare.app.data.repository.MedicationGroupRepository
import com.dosecare.app.data.repository.MedicineRepository
import com.dosecare.app.data.repository.MedicineStockRepository
import com.dosecare.app.data.repository.SettingsRepository
import com.dosecare.app.model.medication.MedicationCategory
import com.dosecare.app.model.medication.Medicine
import com.dosecare.app.model.medication.MedicineDisplayDoseUnit
import com.dosecare.app.model.medication.MedicineIdentityKey
import com.dosecare.app.model.medication.MedicinePreparation
import com.dosecare.app.model.medication.MedicineSelection
import com.dosecare.app.model.medication.MedicineStock
import com.dosecare.app.model.pharmacology.DrugCatalogService
import com.dosecare.app.model.settings.SettingsState
import com.dosecare.app.ui.theme.HrtTrackerTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

class MedicineDetailScreenPatchOffTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun patchOffSummaryMovesFromPreparationSectionToHeaderTooltip() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val patchOff = patchOffMedicine()
        val medicineRepository: MedicineRepository = mockk()
        val medicationGroupRepository: MedicationGroupRepository = mockk()
        val stockRepository: MedicineStockRepository = mockk()
        val settingsRepository: SettingsRepository = mockk()
        // v0.9g merge: the view model resolves medicine.drugCatalogId through the
        // catalog. This test does not exercise the dossier, so an empty catalog is
        // enough — tryGetById then returns null ("no dossier linked").
        val drugCatalog = DrugCatalogService(emptyList())
        every { medicineRepository.getCachedActiveMedicine(patchOff.uuid) } returns patchOff
        every { medicineRepository.observeByUuid(patchOff.uuid) } returns flowOf(patchOff)
        every { medicationGroupRepository.getCachedGroups() } returns emptyList()
        every { medicationGroupRepository.observeGroups() } returns flowOf(emptyList())
        every { stockRepository.getCachedProjection(patchOff.uuid) } returns null
        every { stockRepository.observeProjections() } returns flowOf(emptyList())
        every { settingsRepository.settingsState } returns MutableStateFlow(SettingsState())
        coEvery { medicineRepository.isLocked(patchOff.uuid) } returns false

        // Construct the view model outside setContent: building one inside a
        // composable trips the ViewModelConstructorInComposable lint check.
        val viewModel = MedicineDetailViewModel(
            medicineRepository = medicineRepository,
            medicationGroupRepository = medicationGroupRepository,
            stockRepository = stockRepository,
            settingsRepository = settingsRepository,
            drugCatalog = drugCatalog,
            savedStateHandle = SavedStateHandle(
                mapOf(MedicineDetailViewModel.MEDICINE_ID_ARG to patchOff.uuid.toString()),
            ),
        )

        composeRule.setContent {
            HrtTrackerTheme(dynamicColor = false) {
                MedicineDetailScreen(
                    onNavigateBack = { },
                    onArchiveExit = { true },
                    onGroupClick = { },
                    viewModel = viewModel,
                )
            }
        }

        val patchOffName = context.getString(R.string.medicine_patch_off_name)
        val preparationHeading = context.getString(R.string.medicine_preparation)
        val summary = context.getString(R.string.medicine_patch_off_detail_summary)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(patchOffName, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithText(preparationHeading, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText(summary, useUnmergedTree = true)
            .assertDoesNotExist()

        composeRule.onNodeWithContentDescription(summary, useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithText(summary, useUnmergedTree = true)
            .assertIsDisplayed()
    }
}

private fun patchOffMedicine(): Medicine {
    return Medicine(
        uuid = UUID.fromString("dddddddd-0000-0000-0000-000000000000"),
        selection = MedicineSelection.PatchOff,
        category = MedicationCategory.ESTRADIOL,
        preparation = MedicinePreparation.PatchOff,
        displayName = null,
        identityKey = MedicineIdentityKey.patchOff(),
        displayDoseUnit = MedicineDisplayDoseUnit.MG,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        archivedAt = null,
        stock = MedicineStock(),
    )
}
