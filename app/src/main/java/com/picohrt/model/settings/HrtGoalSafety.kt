package com.picohrt.model.settings

import androidx.annotation.StringRes
import com.picohrt.R
import com.picohrt.model.medication.MedicationCategory
import com.picohrt.model.medication.MedicationKey

/**
 * F-PR4: per-(goal, medication) clinical safety warning. Surfaced in
 * the medication editor (and any picker that lands on a
 * goal-mismatched medication) so a user picking up the wrong regimen
 * gets a soft prompt before they commit a dose. Warnings are
 * informational, never blocking — the user can dismiss and save if
 * the regimen is genuinely prescribed.
 */
data class HrtGoalSafetyWarning(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val severity: HrtGoalSafetySeverity,
)

enum class HrtGoalSafetySeverity {
    /** Confirmed contraindication: a hard "this is the wrong hormone" prompt. */
    STRONG,
    /** Off-label but possible (low-dose T in MtF, E2 in FtM): a softer nudge. */
    SOFT,
}

/**
 * F-PR4: returns the safety warning set for a (goal, medication)
 * pair. Empty list means the medication fits the goal cleanly.
 *
 * The matrix covers the clinically meaningful mismatches:
 * - FTM + E2 family: E2 is the primary MtF hormone, off-label for
 *   FtM. SOFT (some FtM users do take low-dose E2 for specific
 *   reasons — soft prompt, not a block).
 * - FTM + CPA: CPA is an MtF antiandrogen. STRONG (it is the
 *   antithesis of FtM).
 * - MTF + T family: T is the primary FtM hormone, off-label for
 *   MtF. SOFT.
 * - All other combinations: empty (no warning).
 */
fun hrtGoalSafetyWarningsFor(
    goal: HrtGoal,
    medicationKey: MedicationKey?,
    category: MedicationCategory?,
): List<HrtGoalSafetyWarning> {
    if (goal == HrtGoal.UNSET || goal == HrtGoal.NON_BINARY) {
        // No goal picked (or the user opted out of goal-based
        // gating) — no warning. F-PR5 documents that NON_BINARY
        // intentionally skips goal-based safety prompts.
        return emptyList()
    }
    if (medicationKey == null || category == null) {
        // Custom medication without a catalog key — we can't
        // route to a specific warning. The user named the
        // medication themselves; trust the name.
        return emptyList()
    }
    val isEstradiolFamily = category == MedicationCategory.ESTRADIOL ||
            medicationKey.name.startsWith("ESTRADIOL_")
    val isTestosteroneFamily = category == MedicationCategory.TESTOSTERONE ||
            medicationKey.name.startsWith("TESTOSTERONE_")
    val isCpa = medicationKey == MedicationKey.CYPROTERONE_ACETATE
    return when (goal) {
        HrtGoal.FTM -> when {
            isCpa -> listOf(
                HrtGoalSafetyWarning(
                    titleRes = R.string.safety_hrt_goal_mismatch_cpa_ftm_title,
                    bodyRes = R.string.safety_hrt_goal_mismatch_cpa_ftm_body,
                    severity = HrtGoalSafetySeverity.STRONG,
                ),
            )
            isEstradiolFamily -> listOf(
                HrtGoalSafetyWarning(
                    titleRes = R.string.safety_hrt_goal_mismatch_e2_ftm_title,
                    bodyRes = R.string.safety_hrt_goal_mismatch_e2_ftm_body,
                    severity = HrtGoalSafetySeverity.SOFT,
                ),
            )
            else -> emptyList()
        }
        HrtGoal.MTF -> when {
            isTestosteroneFamily -> listOf(
                HrtGoalSafetyWarning(
                    titleRes = R.string.safety_hrt_goal_mismatch_t_mtf_title,
                    bodyRes = R.string.safety_hrt_goal_mismatch_t_mtf_body,
                    severity = HrtGoalSafetySeverity.SOFT,
                ),
            )
            else -> emptyList()
        }
        HrtGoal.UNSET, HrtGoal.NON_BINARY -> emptyList()
    }
}

/**
 * F-PR4: convenience wrapper that maps each [HrtGoalSafetyWarning]
 * to the corresponding [com.picohrt.ui.components.MedicalDisclaimerKind]
 * so the medication editor can pass them into the existing
 * MedicalDisclaimerText composable without a new UI component.
 */
fun hrtGoalSafetyWarningKindsFor(
    goal: HrtGoal,
    medicationKey: MedicationKey?,
    category: MedicationCategory?,
): List<com.picohrt.ui.components.MedicalDisclaimerKind> {
    return hrtGoalSafetyWarningsFor(goal, medicationKey, category).map { warning ->
        when {
            warning.titleRes == R.string.safety_hrt_goal_mismatch_cpa_ftm_title ->
                com.picohrt.ui.components.MedicalDisclaimerKind.HRT_GOAL_MISMATCH_CPA_FTM
            warning.titleRes == R.string.safety_hrt_goal_mismatch_e2_ftm_title ->
                com.picohrt.ui.components.MedicalDisclaimerKind.HRT_GOAL_MISMATCH_E2_FTM
            warning.titleRes == R.string.safety_hrt_goal_mismatch_t_mtf_title ->
                com.picohrt.ui.components.MedicalDisclaimerKind.HRT_GOAL_MISMATCH_T_MTF
            else ->
                com.picohrt.ui.components.MedicalDisclaimerKind.PRESET_DOSES
        }
    }
}
