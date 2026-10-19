package com.dosecare.app.data.repository

import com.dosecare.app.model.medication.DoseInstruction
import com.dosecare.app.model.medication.MedicationGroup
import com.dosecare.app.model.medication.MedicationGroupMedication
import com.dosecare.app.model.medication.MedicationGroupSchedule
import com.dosecare.app.model.medication.MedicationGroupScheduleType
import com.dosecare.app.model.medication.Medicine
import com.dosecare.app.model.medication.MedicinePreparation
import com.dosecare.app.model.pharmacology.DrugCatalogService
import com.dosecare.app.model.pharmacology.PatientProfile
import com.dosecare.app.model.pharmacology.RuleEngine
import com.dosecare.app.model.pharmacology.UserDrugForRule
import com.dosecare.app.model.personalization.UserProfile as AppUserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v0.9g merge: bridges the regimen side of the app into the pharmacology rule engine.
 *
 * The rule engine is written against `UserDrugForRule(drugCatalogId, doseMg, frequencyPerDay)`,
 * which is DoseCare's flattened shape — it has no notion of medicine identities, preparation
 * hierarchies, or group schedules. This repository is the only place that translation lives.
 *
 * Two decisions worth knowing about:
 *
 * **Frequency comes from the group schedule, not the medicine.** A medicine has no inherent
 * frequency; "twice daily" belongs to the slot that schedules it. We count the group's daily
 * schedule times for DAILY groups, and estimate from the weekly day set for WEEKLY groups
 * (`daysPerWeek * timesPerDay / 7`). That is an estimate, and it is the number the engine
 * scales AUC folds by, so a weekly-only regimen reads as its daily equivalent — which is what
 * a steady-state interaction calculation wants anyway.
 *
 * **Dangling catalog ids are dropped, never thrown on.** `RuleEngine.evaluate` calls
 * `catalog.getById`, which throws on a miss. A medicine can legitimately point at a catalog id
 * that a future catalog version removed, so every id is checked with `tryGetById` before it
 * leaves this file.
 */
@Singleton
class InteractionAnalysisRepository @Inject constructor(
    private val medicationGroupRepository: MedicationGroupRepository,
    private val medicineRepository: MedicineRepository,
    private val userProfileRepository: UserProfileRepository,
    private val catalog: DrugCatalogService,
    private val ruleEngine: RuleEngine,
) {

    /**
     * The regimen as the rule engine sees it, re-emitting whenever the schedule, the medicine
     * set, or the patient profile changes.
     */
    fun observeUserDrugs(): Flow<List<UserDrugForRule>> =
        combine(
            medicationGroupRepository.observeGroups(),
            userProfileRepository.observeProfile(),
        ) { groups, profile ->
            groups to profile
        }.map { (groups, _) -> groups.orEmpty().toRuleInputs() }

    /**
     * Full evaluation. Returns an empty list when nothing in the regimen is linked to the
     * catalog — an empty result is the correct answer there, not an error.
     */
    suspend fun evaluate(): List<com.dosecare.app.model.pharmacology.Interaction> {
        val inputs = medicationGroupRepository.getGroups().toRuleInputs()
        if (inputs.isEmpty()) return emptyList()
        return ruleEngine.evaluate(inputs, patientProfile())
    }

    /**
     * Convenience for one-shot callers (widget, notification tap). Same shape as [evaluate],
     * and suspending for the same reason: reading the regimen goes through the database.
     */
    suspend fun evaluateNow(): List<com.dosecare.app.model.pharmacology.Interaction> {
        val inputs = medicationGroupRepository.getGroups().toRuleInputs()
        if (inputs.isEmpty()) return emptyList()
        return ruleEngine.evaluate(inputs, patientProfile())
    }

    /**
     * Patient profile for the engine.
     *
     * The engine wants its own `PatientProfile` (CYP genotype, Child-Pugh, eGFR) and the app
     * has its own `UserProfile` (body weight, unit preference). They have no overlapping
     * fields yet, so this starts from the engine's conservative default — every adjustment it
     * applies is then the unmodified population average, which is the right default when the
     * corresponding clinical facts are unknown. When the app grows genotype / hepatic / renal
     * fields, this is the single function to wire them in.
     */
    private fun patientProfile(): PatientProfile = PatientProfile()

    // ============== translation ==============

    private fun List<MedicationGroup>.toRuleInputs(): List<UserDrugForRule> {
        val medicineByUuid = LinkedHashMap<String, Medicine>()
        forEach { group ->
            group.medications.forEach { slot ->
                val medicine = slot.medicine ?: return@forEach
                medicineByUuid[medicine.uuid.toString()] = medicine
            }
        }

        val inputs = mutableListOf<UserDrugForRule>()
        val seen = mutableSetOf<String>()

        forEach { group ->
            val frequency = group.schedule.frequencyPerDayEstimate()
            group.medications.forEach { slot ->
                val medicine = slot.medicine ?: return@forEach
                val catalogId = medicine.drugCatalogId
                if (catalogId.isBlank()) return@forEach
                // RuleEngine.getById throws; check first so a dangling id degrades to "skip".
                if (catalog.tryGetById(catalogId) == null) return@forEach

                val doseMg = slot.doseInstruction.effectiveDoseMg(medicine.preparation)
                if (doseMg <= 0.0) return@forEach

                // One entry per drug: the engine evaluates pairwise, so feeding the same drug
                // twice would make it interact with itself. Take the largest single-administration
                // dose if the user happens to have it in two groups at different strengths.
                val key = catalogId
                val existing = inputs.indexOfFirst { it.drugCatalogId == key }
                if (existing >= 0) {
                    val prior = inputs[existing]
                    if (doseMg > prior.doseMg) {
                        inputs[existing] = prior.copy(
                            doseMg = doseMg,
                            frequencyPerDay = maxOf(prior.frequencyPerDay, frequency),
                        )
                    } else {
                        inputs[existing] = prior.copy(
                            frequencyPerDay = maxOf(prior.frequencyPerDay, frequency),
                        )
                    }
                    seen.add(key)
                } else {
                    inputs += UserDrugForRule(
                        drugCatalogId = catalogId,
                        doseMg = doseMg,
                        frequencyPerDay = frequency,
                    )
                    seen.add(key)
                }
            }
        }
        return inputs
    }

    /**
     * Administrations per day for a group.
     *
     * DAILY groups have an explicit list of times-of-day. WEEKLY groups have a day-of-week set
     * instead, so the daily equivalent is `activeDays * timesPerDay / 7` — the steady-state
     * average the interaction math is defined against.
     */
    private fun MedicationGroupSchedule.frequencyPerDayEstimate(): Double {
        val timesPerDay = times.size.coerceAtLeast(1).toDouble()
        return when (type) {
            MedicationGroupScheduleType.DAILY -> timesPerDay
            MedicationGroupScheduleType.WEEKLY ->
                weeklyDaysOfWeek.size.coerceAtLeast(1).toDouble() * timesPerDay / 7.0
        }
    }

    /**
     * Single-administration dose in mg of the active compound.
     *
     * Multiplies the preparation's per-unit strength by the slot's dose instruction. Returns 0
     * for preparations with no meaningful mg content (the PATCH_OFF sentinel, and gel/patch
     * forms that carry no discrete mg amount), which the caller treats as "not evaluable".
     */
    private fun DoseInstruction.effectiveDoseMg(preparation: MedicinePreparation): Double {
        val perUnit = preparation.perUnitMg() ?: return 0.0
        val fraction = when (this) {
            is DoseInstruction.Noop -> 1.0
            is DoseInstruction.TabletFraction ->
                if (denominator <= 0) 1.0 else numerator.toDouble() / denominator.toDouble()
            is DoseInstruction.WholeUnit -> 1.0
            is DoseInstruction.VolumeMl -> valueMl
            is DoseInstruction.WeightGrams -> valueGrams
        }
        if (fraction <= 0.0) return 0.0
        return perUnit * fraction
    }

    /**
     * Mg of active compound per whole preparation unit, or null when the preparation does not
     * have a single discrete mg amount.
     */
    private fun MedicinePreparation.perUnitMg(): Double? = when (this) {
        is MedicinePreparation.Pill -> strengthMgPerTablet
        is MedicinePreparation.Capsule -> strengthMgPerCapsule
        is MedicinePreparation.InjectionSingleUseVial -> strengthMgPerVial
        is MedicinePreparation.InjectionMultiUseVial -> concentrationMgPerMl * vialVolumeMl
        // Gel and patch deliver a rate, not a discrete unit: concentrationPercent g/g and
        // mcg/day release have no single "mg per unit" without knowing the applied area and
        // duration, which the rule engine's CYP/QTc paths do not consume anyway.
        is MedicinePreparation.GelSachet -> null
        is MedicinePreparation.GelContainer -> null
        is MedicinePreparation.ImportedInjection -> null
        is MedicinePreparation.ImportedGel -> null
        is MedicinePreparation.Patch -> when (specification) {
            is MedicinePreparation.PatchSpecification.TotalMg -> specification.valueMg
            is MedicinePreparation.PatchSpecification.ReleaseRateMcgPerDay -> null
        }
        is MedicinePreparation.PatchOff -> null
    }
}
