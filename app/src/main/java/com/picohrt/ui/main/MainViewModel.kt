package com.picohrt.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.picohrt.data.repository.BloodTestRepository
import com.picohrt.data.repository.HomeInputSource
import com.picohrt.data.repository.HomeInputs
import com.picohrt.data.repository.HomeRepository
import com.picohrt.data.repository.MedicationLogRepository
import com.picohrt.data.repository.SettingsRepository
import com.picohrt.di.DefaultDispatcher
import com.picohrt.model.bloodtest.AllowedAnalyteUnit
import com.picohrt.model.bloodtest.BloodAnalyteKey
import com.picohrt.model.bloodtest.BloodUnitKey
import com.picohrt.model.home.HomeCardType
import com.picohrt.model.medication.MedicationCategory
import com.picohrt.model.medication.MedicationKey
import com.picohrt.model.medication.MedicationLogEntry
import com.picohrt.model.medication.MedicineSelection
import com.picohrt.model.medication.MedicineStockProjection
import com.picohrt.model.medication.MedicineStockState
import com.picohrt.model.medication.lowStockSeverityRank
import com.picohrt.model.medication.visibleMedicationEntries
import com.picohrt.model.pk.HomeE2ChartWindowOption
import com.picohrt.model.pk.PkHormone
import com.picohrt.model.pk.PkMedicationSimulation
import com.picohrt.model.pk.PkTUserOverride
import com.picohrt.model.settings.HrtGoal
import com.picohrt.ui.journal.toAnchorRowUiState
import com.picohrt.util.AppDiagnosticsLogger
import com.picohrt.util.AppTimeSnapshot
import com.picohrt.util.AppTimeSource
import com.picohrt.util.TimeZoneChangeNotice
import com.picohrt.util.TimeZoneChangeNoticeController
import com.picohrt.util.tickWhileSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val settingsRepository: SettingsRepository,
    private val bloodTestRepository: BloodTestRepository,
    private val timeZoneChangeNoticeController: TimeZoneChangeNoticeController,
    private val diagnosticsLogger: AppDiagnosticsLogger = AppDiagnosticsLogger(),
    appTimeSource: AppTimeSource,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val currentSnapshot = appTimeSource.currentSnapshot
    private var lastHomeSnapshotRefreshKey: HomeSnapshotRefreshKey? = null

    // PR3-UI: the home chart now has a per-hormone selector. ESTRADIOL
    // keeps the existing default; TESTOSTERONE flips the chart and
    // hero to the T engine path. The selector is in-memory only for
    // v0.6 (no persistence) because chart-hormone is a view state
    // the user picks per session, not a stable preference.
    //
    // F-PR3: when the user picks an HRT goal in settings the chart
    // re-picks its default via the auto-picker coroutine below; a
    // session override is preserved as long as it remains in the
    // goal's available set. The available set is also exposed for
    // the chip row (MainChartHormoneChipRow).
    private val _selectedChartHormone = MutableStateFlow(PkHormone.ESTRADIOL)
    val selectedChartHormone: StateFlow<PkHormone> = _selectedChartHormone.asStateFlow()

    // F-PR3: latest persisted HRT goal. Cached so the home chart
    // can auto-re-pick a default when the user changes goals in
    // settings without re-running the full combine.
    private val _currentHrtGoal = MutableStateFlow(HrtGoal.UNSET)
    val currentHrtGoal: StateFlow<HrtGoal> = _currentHrtGoal.asStateFlow()

    private val _uiState = MutableStateFlow(
        MainUiState(
            homeDataReady = false,
            now = currentSnapshot.value.minute,
        )
    )

    // The ui-state combine below stays collected for the ViewModel's whole
    // (activity-scoped) lifetime: a stop-timeout would let data mutated while
    // away (e.g. a backup restore) flash one stale frame on re-entry, so data
    // emissions must keep rebuilding the retained value in the background.
    // The time inputs, however, are gated on this state's own subscriber
    // count: a live minute tick would otherwise rebuild the ui state — PK
    // simulation fallbacks included — every minute for the activity's whole
    // lifetime, including backgrounded. While unsubscribed only date/zone
    // changes pass through, keeping the midnight/timezone re-anchor.
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val gatedSnapshot: StateFlow<AppTimeSnapshot> =
        currentSnapshot.tickWhileSubscribed(_uiState.subscriptionCount) { snapshot ->
            snapshot.minute.toLocalDate() to snapshot.zone
        }
    private val gatedMinute: StateFlow<LocalDateTime> =
        appTimeSource.currentMinute.tickWhileSubscribed(_uiState.subscriptionCount) { minute ->
            minute.toLocalDate()
        }

    init {
        viewModelScope.launch {
            buildUiStateFlow().collect { state -> _uiState.value = state }
        }
        // F-PR3: react to HRT-goal changes by re-picking the home
        // chart's default hormone. The settingsState flow is hot
        // (StateFlow + combine upstream of buildUiStateFlow), so
        // distinctUntilChanged fires once per actual goal change.
        // We keep the in-session override (a user who explicitly
        // tapped a different chip) by only flipping when the current
        // selection is no longer in the goal's available set OR
        // the goal itself just changed.
        viewModelScope.launch {
            settingsRepository.settingsState
                .map { it.hrtGoal }
                .distinctUntilChanged()
                .collect { goal ->
                    _currentHrtGoal.value = goal
                    val available = availableChartHormonesFor(goal)
                    val current = _selectedChartHormone.value
                    if (current !in available) {
                        _selectedChartHormone.value = defaultChartHormoneFor(goal)
                    } else if (current != defaultChartHormoneFor(goal) && _lastGoalSnapshot == null) {
                        // Goal change on a fresh VM: re-pick the
                        // default so a FTM user sees T after they
                        // pick FTM, even if the prior session had
                        // ESTRADIOL selected.
                        _selectedChartHormone.value = defaultChartHormoneFor(goal)
                    }
                    _lastGoalSnapshot = goal
                }
        }
    }

    // F-PR3: tracks the most recent goal seen by the auto-picker
    // so the initial pre-F-PR3 (UNSET → ESTRADIOL) doesn't get
    // re-applied on every UNSET replay.
    private var _lastGoalSnapshot: HrtGoal? = null

    // Re-subscribe the home inputs flow only on local date or zone changes.
    // Room query windows are date+zone-derived, while per-minute and explicit
    // wall-clock refreshes are fed to HomeRepository as a combine input so
    // now-sensitive projections re-anchor without tearing down the Room flows.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildUiStateFlow(): Flow<MainUiState> {
        val keyedInputsFlow = gatedSnapshot
            .map { snapshot ->
                HomeTimeKey(
                    now = snapshot.minute,
                    date = snapshot.minute.toLocalDate(),
                    zoneId = snapshot.zone,
                )
            }
            .distinctUntilChangedBy { key -> key.date to key.zoneId }
            .flatMapLatest { key ->
                refreshHomeSnapshotForDateIfNeeded(key.now, key.zoneId)
                homeRepository.observeHomeInputs(
                    date = key.date,
                    nowFlow = gatedMinute,
                    zoneId = key.zoneId,
                )
                    .map { inputs -> KeyedHomeInputs(key = key, inputs = inputs) }
            }

        val homeStateFlow = combine(
            keyedInputsFlow,
            gatedSnapshot,
            timeZoneChangeNoticeController.notice,
            settingsRepository.homeLowStockSectionExpandedFlow,
            settingsRepository.homeLowStockAcknowledgedWarningStatesFlow,
            // PR3-UI: per-hormone medication log flow for the T chart path.
            // The flow stays cold (no Room query) until the user picks
            // TESTOSTERONE; the combine is keyed off the hormone so an E2
            // user never subscribes to the T data stream.
            medicationLogRepository.observeEntries().map { entries ->
                entries.orEmpty().filter { it.category == MedicationCategory.TESTOSTERONE }
            }.distinctUntilChanged(),
            // PR4: same pattern for CPA. Both hormone-specific flows share
            // the same upstream Room query; the only overhead vs a single
            // combined flow is one extra .filter().orEmpty() per upstream
            // emission, which is negligible next to the PK simulation cost.
            medicationLogRepository.observeEntries().map { entries ->
                entries.orEmpty().filter { it.category == MedicationCategory.ANTIANDROGEN }
            }.distinctUntilChanged(),
            _selectedChartHormone,
            // F-PR3: the auto-picker writes here on goal change, and
            // joining the combine means a goal flip in settings
            // (settingsRepository.settingsState) triggers a MainUiState
            // rebuild on the next downstream tick. Without this input
            // the chip filter (MainChartHormoneChipRow) would stay
            // stale until the next HomeInputs emission.
            _currentHrtGoal,
            // v2 血检追踪 (BT-PR2): blood test panels feed the home
            // "Recent Bloodwork" card. The flow stays cold (no Room
            // query) until the home screen subscribes; for users
            // without blood test data the card builder returns null
            // and the home screen skips rendering it entirely.
            bloodTestRepository.observePanels().distinctUntilChanged(),
        ) { values ->
            // The 10-arg combine is implemented as the vararg overload
            // returning a `Flow<Array<Any?>>`; destructure back to the
            // typed tuple. Order matches the flow list above.
            val keyedInputs = values[0] as KeyedHomeInputs
            val timeSnapshot = values[1] as AppTimeSnapshot
            val timeZoneNotice = values[2] as TimeZoneChangeNotice?
            val storedLowStockSectionExpanded = values[3] as Boolean
            val acknowledgedWarningStates = values[4] as Map<String, MedicineStockState>
            @Suppress("UNCHECKED_CAST")
            val testosteroneEntries = values[5] as List<MedicationLogEntry>
            @Suppress("UNCHECKED_CAST")
            val cpaEntries = values[6] as List<MedicationLogEntry>
            val activeHormone = values[7] as PkHormone
            val currentHrtGoal = values[8] as HrtGoal
            @Suppress("UNCHECKED_CAST")
            val bloodTestPanels = values[9] as List<com.picohrt.model.bloodtest.BloodTestPanel>
            if (!keyedInputs.key.matches(timeSnapshot)) {
                return@combine null
            }
            val inputs = keyedInputs.inputs
            if (
                inputs.source == HomeInputSource.ROOM &&
                inputs.stockWarnings.isEmpty() &&
                acknowledgedWarningStates.isNotEmpty()
            ) {
                // Swallow DataStore failures: letting them propagate would tear
                // down this combine -> stateIn home flow. A dropped clear is
                // self-correcting on the next empty-ROOM emission.
                try {
                    settingsRepository.clearHomeLowStockAcknowledgedWarningStates()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Best-effort cleanup; see comment above.
                }
            }
            withContext(defaultDispatcher) {
                buildHomeUiState(
                    inputs = inputs,
                    now = timeSnapshot.minute,
                    zoneId = timeSnapshot.zone,
                    timeZoneNotice = timeZoneNotice,
                    lowStockSectionExpanded = shouldExpandLowStockSection(
                        storedExpanded = storedLowStockSectionExpanded,
                        stockWarnings = inputs.stockWarnings,
                        acknowledgedWarningStates = acknowledgedWarningStates,
                    ),
                    activeHormone = activeHormone,
                    testosteroneEntries = testosteroneEntries,
                    cpaEntries = cpaEntries,
                    currentHrtGoal = currentHrtGoal,
                    bloodTestPanels = bloodTestPanels,
                )
            }
        }
            .filterNotNull()

        return combine(
            homeStateFlow,
            settingsRepository.homeCardLayoutFlow,
        ) { state, layout ->
            state.copy(homeCardLayout = layout)
        }
    }

    private val _highlightRequest = MutableStateFlow<DoseRowHighlightRequest?>(null)
    val highlightRequest: StateFlow<DoseRowHighlightRequest?> = _highlightRequest.asStateFlow()

    private val _homeDeepLinkSignal = MutableStateFlow(0)
    val homeDeepLinkSignal: StateFlow<Int> = _homeDeepLinkSignal.asStateFlow()

    private val _milestonesDeepLinkSignal = MutableStateFlow(0)
    val milestonesDeepLinkSignal: StateFlow<Int> = _milestonesDeepLinkSignal.asStateFlow()

    // Dedup marker for the milestones deep link. Deliberately a ViewModel field
    // rather than NavHost rememberSaveable state so it shares the signal's exact
    // lifetime: both reset to 0 on process death, both survive a config-change
    // recreation together. A marker saved to instance state would outlive the
    // signal — a process-death re-tap would restore lastHandled ahead of the
    // fresh 0-based signal and silently drop the deep link, and a font-scale (or
    // any non-configChanges) recreation would replay the retained intent.
    private var lastHandledMilestonesSignal = 0

    // True once an onCreate has parsed its launch intent. ViewModel-lifetime on purpose
    // (same rationale as lastHandledMilestonesSignal): survives a config recreation, so
    // the retained intent's widget extras are not replayed and cannot yank the user back
    // to a deep link they navigated away from — but resets on process death, so a widget
    // or shortcut tap that recreates a killed task (which lands in onCreate with a
    // non-null savedInstanceState, because a dead instance can never receive onNewIntent)
    // still parses the fresh intent and navigates.
    var launchIntentParsed = false

    fun requestMilestonesDeepLink() {
        _milestonesDeepLinkSignal.update { it + 1 }
    }

    // Fires the milestones navigation once per request: returns true for the
    // first observer of each new signal value and marks it handled; later calls
    // for the same (or an already-superseded) signal return false.
    fun consumeMilestonesDeepLink(): Boolean {
        val signal = _milestonesDeepLinkSignal.value
        if (signal <= lastHandledMilestonesSignal) {
            return false
        }
        lastHandledMilestonesSignal = signal
        return true
    }

    fun requestDoseRowHighlight(keys: List<DoseRowHighlightKey>) {
        if (keys.isEmpty()) {
            return
        }
        _highlightRequest.value = DoseRowHighlightRequest(keys)
        _homeDeepLinkSignal.update { it + 1 }
    }

    fun consumeHighlightRequest(request: DoseRowHighlightRequest) {
        // Clear only if this is still the active request. A cancellation-time
        // consume (tab switch) must not clobber a newer request that arrived
        // from a fresh deep link.
        _highlightRequest.compareAndSet(request, null)
    }

    // Once-per-process view concern owned here (this ViewModel is activity-
    // scoped in a single-activity app) rather than as a file-level static: a
    // static is invisible to tests, replays only on process death anyway, and
    // is racy across simultaneous Home compositions — the loser would read the
    // pre-claim value but never consume it, sticking on the intro render path
    // forever.
    private var homeE2ChartIntroAnimationClaimed = false

    /**
     * Claims the once-per-ViewModel-lifetime E2 chart intro animation.
     * Returns true for exactly one caller; that composition plays the intro,
     * every other (and later) composition renders the synchronous-model path
     * directly.
     */
    fun claimHomeE2ChartIntroAnimation(): Boolean {
        if (homeE2ChartIntroAnimationClaimed) {
            return false
        }
        homeE2ChartIntroAnimationClaimed = true
        return true
    }

    fun dismissTimeZoneChangeNotice() {
        timeZoneChangeNoticeController.dismiss()
    }

    fun setHomeE2DisplayUnit(unit: BloodUnitKey) {
        viewModelScope.launch {
            settingsRepository.setHomeE2DisplayUnit(AllowedAnalyteUnit.of(BloodAnalyteKey.E2, unit))
        }
    }

    fun setHomeE2ChartWindowOption(option: HomeE2ChartWindowOption) {
        viewModelScope.launch {
            settingsRepository.setHomeE2ChartWindowOption(option)
        }
    }

    fun setHomeChartHormone(hormone: PkHormone) {
        _selectedChartHormone.value = hormone
    }

    fun setHomeCardLayout(order: List<HomeCardType>, hidden: Set<HomeCardType>) {
        viewModelScope.launch {
            settingsRepository.setHomeCardLayout(order, hidden)
        }
    }

    fun setLowStockSectionExpanded(expanded: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHomeLowStockSectionFoldState(
                expanded = expanded,
                acknowledgedWarningStates = if (expanded) {
                    emptyMap()
                } else {
                    uiState.value.stockWarnings.toLowStockWarningStateMap()
                },
            )
        }
    }

    private fun refreshHomeSnapshotForDateIfNeeded(
        now: LocalDateTime,
        zoneId: ZoneId,
    ) {
        val key = HomeSnapshotRefreshKey(
            date = now.toLocalDate(),
            zoneId = zoneId.id,
        )
        if (lastHomeSnapshotRefreshKey == key) {
            return
        }
        lastHomeSnapshotRefreshKey = key
        homeRepository.refreshHomeSnapshotAsync(now = now, force = false, zoneId = zoneId)
    }

    private fun buildHomeUiState(
        inputs: HomeInputs,
        now: LocalDateTime,
        zoneId: ZoneId,
        timeZoneNotice: TimeZoneChangeNotice?,
        lowStockSectionExpanded: Boolean,
        activeHormone: PkHormone,
        testosteroneEntries: List<MedicationLogEntry>,
        cpaEntries: List<MedicationLogEntry>,
        // F-PR3: threaded from the combine (which is fed by the
        // _currentHrtGoal auto-picker). Passed in so the chip filter
        // and FTM antiandrogen exclusion reflect the latest goal
        // without a HomeSnapshot refresh.
        currentHrtGoal: HrtGoal,
        // v2 血检追踪 (BT-PR2): blood test panels from the live
        // Room flow. The card builder returns null when the user
        // has no panels, which makes the home screen hide the card
        // entirely (no empty container rendered).
        bloodTestPanels: List<com.picohrt.model.bloodtest.BloodTestPanel>,
    ): MainUiState {
        val homeE2DisplayUnit = inputs.settings.homeE2DisplayUnit
        val chartWindowOption = inputs.settings.homeE2ChartWindowOption
        val homeEntries = (inputs.scheduleEntries + inputs.antiandrogenHistoryEntries)
            .distinctBy { entry -> entry.uuid }
            .sortedByDescending { entry -> entry.appliedAt }
        val allGroups = inputs.activeGroups + inputs.archivedGroups
        val visibleEntries = visibleMedicationEntries(
            homeEntries,
            allGroups,
            inputs.settings.showArchivedGroupRecords,
        )
        val scheduleGroups =
            if (inputs.settings.showArchivedGroupRecords) allGroups else inputs.activeGroups
        // HomeInputs was constructed against the date-scoped anchor `now` at
        // flow subscription time. The live `now` ticks per minute and may
        // cross a planned-slot expiry without a fresh upstream emission, so
        // re-validate against the live `now` here:
        //   - pkProjection: drop if the cached expiry is on or before now.
        //   - estradiolPkPlannedEntries: drop synthetic doses whose slot has
        //     already passed (the user didn't take them — don't keep them on
        //     the curve as if they did).
        val nowInstant = now.atZone(zoneId).toInstant()
        val freshProjection = inputs.pkProjection?.takeIf {
            inputs.pkProjectionExpiresAt?.isAfter(nowInstant) ?: true
        }
        val freshPlannedEntries = inputs.estradiolPkPlannedEntries.filter { entry ->
            entry.scheduledFor?.isAfter(now) ?: false
        }
        // F-PR-snapshot-upgrade: prefer the snapshot's cached T /
        // CPA entries when source == SNAPSHOT, so a user who flips
        // the chip to T / CPA on cold start renders a non-empty
        // chart on the first frame. The live
        // [testosteroneEntries] / [cpaEntries] still arrive on
        // the next tick and will overwrite once the Room flow
        // re-emits; we only fall back to the live flow when the
        // snapshot's pool is empty (older v23 cache or a user
        // with no T / CPA doses).
        val testosteroneEntriesForTrend = if (inputs.source == HomeInputSource.SNAPSHOT) {
            inputs.testosteronePkEntries.takeIf { it.isNotEmpty() } ?: testosteroneEntries
        } else {
            testosteroneEntries
        }
        val cpaEntriesForTrend = if (inputs.source == HomeInputSource.SNAPSHOT) {
            inputs.cpaPkEntries.takeIf { it.isNotEmpty() } ?: cpaEntries
        } else {
            cpaEntries
        }
        // PR3-UI: per-hormone simulation routing. E2 keeps the snapshot-
        // backed projection + Room-fed entries path. T / CPA now use the
        // snapshot-cached entries (F-PR-snapshot-upgrade) on the first
        // frame and fall through to the live flow on subsequent ticks.
        val trendResult = when (activeHormone) {
            PkHormone.ESTRADIOL -> freshProjection?.toMainEstradiolTrend(
                now = now,
                zoneId = zoneId,
                option = chartWindowOption,
            ) ?: PkMedicationSimulation.simulateMainEstradiolTrend(
                entries = inputs.estradiolPkEntries,
                plannedEntries = freshPlannedEntries,
                bodyWeightKg = inputs.profile.weightKg,
                now = now,
                zoneId = zoneId,
                option = chartWindowOption,
            )
            PkHormone.TESTOSTERONE -> PkMedicationSimulation.simulateMainTestosteroneTrend(
                entries = testosteroneEntriesForTrend,
                bodyWeightKg = inputs.profile.weightKg,
                now = now,
                zoneId = zoneId,
                plannedEntries = emptyList(),
                option = chartWindowOption,
                // PR-user-override: forward the user's T PK
                // parameter overrides (kClearInjection for central
                // clearance, threeCompK31 for deep peripheral
                // release). Null fields fall through to the engine
                // catalog defaults. Only the 3-comp path
                // (TU 3-comp) consumes these today; other T
                // esters and the gel path ignore the override.
                userOverride = PkTUserOverride(
                    kClearInjection = inputs.settings.pkClearInjectionOverride,
                    threeCompK31 = inputs.settings.pkK31Override,
                ),
            )
            // PR4: CPA branch — toCpaPkDoseEvent drops non-CPA antiandrogens
            // (spironolactone, bicalutamide, finasteride, dutasteride) so
            // passing the wider ANTIANDROGEN filter through is safe.
            PkHormone.CYPROTERONE_ACETATE -> PkMedicationSimulation.simulateMainCpaTrend(
                entries = cpaEntriesForTrend,
                bodyWeightKg = inputs.profile.weightKg,
                now = now,
                zoneId = zoneId,
                plannedEntries = emptyList(),
                option = chartWindowOption,
            )
        }

        // When the cached projection is invalid the trend falls back to
        // simulating over `estradiolPkEntries`. The SNAPSHOT path now embeds
        // those entries, so it can recompute a real curve without waiting for
        // Room; mark it ready whenever a usable projection or PK entries are
        // present. Only a SNAPSHOT with neither (no dose history) stays gated
        // on the ROOM emission. The T path is "ready" whenever the live
        // T log has at least one entry (snapshot cache ships in F-PR*).
        val e2TrendReady = when (activeHormone) {
            PkHormone.ESTRADIOL -> freshProjection != null ||
                    inputs.estradiolPkEntries.isNotEmpty() ||
                    inputs.source == HomeInputSource.ROOM
            PkHormone.TESTOSTERONE -> testosteroneEntriesForTrend.isNotEmpty() ||
                    inputs.source == HomeInputSource.ROOM
            // PR4: CPA "ready" mirrors T's — at least one ANTIANDROGEN log
            // entry in the live stream (toCpaPkDoseEvent narrows to CPA),
            // or a Room source so the home screen shows the loading
            // skeleton instead of jumping to "no data" before the first
            // emission lands.
            PkHormone.CYPROTERONE_ACETATE -> cpaEntriesForTrend.isNotEmpty() ||
                    inputs.source == HomeInputSource.ROOM
        }

        // Local re-simulation only happens when no usable cached projection
        // remains but embedded PK entries do — log just that case, not every
        // per-minute tick.
        if (freshProjection == null && inputs.estradiolPkEntries.isNotEmpty()) {
            diagnosticsLogger.info(
                TAG,
                "home_pk_trend_resimulated source=${inputs.source} " +
                        "projectionExpired=${inputs.pkProjection != null} " +
                        "pkEntries=${inputs.estradiolPkEntries.size} " +
                        "plannedEntries=${freshPlannedEntries.size}",
            )
        }

        return MainUiState(
            homeDataReady = true,
            e2TrendReady = e2TrendReady,
            homeSource = inputs.source,
            now = now,
            homeE2DisplayUnit = homeE2DisplayUnit,
            homeE2ChartWindowOption = chartWindowOption,
            hideReferenceRanges = inputs.settings.hideReferenceRanges,
            stockWarnings = inputs.stockWarnings,
            lowStockSectionExpanded = lowStockSectionExpanded,
            homeAnchor = inputs.homeAnchor?.toAnchorRowUiState(now.toLocalDate()),
            // PR3-UI: pass the selected hormone into the hero/chart
            // builders so the unit, target range, and title text swap in
            // the right places.
            chartHormone = activeHormone,
            // F-PR3: HRT goal + per-goal chart hormone whitelist feed
            // the chip row filter (MainChartHormoneChipRow drops the
            // CPA chip for FTM, hides E2 / CPA for MTF, etc.).
            // We source the goal from _currentHrtGoal (the auto-picker
            // StateFlow fed by settingsRepository.settingsState) rather
            // than inputs.settings.hrtGoal, so a goal change takes
            // effect on the next uiState rebuild without waiting for a
            // HomeSnapshot refresh.
            hrtGoal = currentHrtGoal,
            availableChartHormones = availableChartHormonesFor(
                currentHrtGoal
            ),
            e2Hero = buildMainE2Hero(
                // The single-row `latestEstradiolEntry` query is bounded at the
                // frozen subscription `now`, so on its own it misses a dose the
                // user logs later in the same session. Feed the schedule window
                // (which carries today's logged doses) alongside it, mirroring
                // the antiandrogen card; buildMainE2Hero filters to estradiol
                // entries at or before the live `now` and picks the latest.
                entries = when (activeHormone) {
                    PkHormone.TESTOSTERONE -> testosteroneEntriesForTrend
                    PkHormone.CYPROTERONE_ACETATE -> cpaEntriesForTrend
                    else -> homeEntries + listOfNotNull(inputs.latestEstradiolEntry)
                },
                trendResult = trendResult,
                displayUnit = homeE2DisplayUnit,
                zoneId = zoneId,
                hormone = activeHormone,
                now = now,
            ),
            e2Chart = buildMainChart(
                trendResult = trendResult,
                displayUnit = homeE2DisplayUnit,
                chartWindowOption = chartWindowOption,
                hormone = activeHormone,
            ),
            antiandrogenGroupSections = listOf(
                MedicationCategory.ANTIANDROGEN,
                MedicationCategory.SERM,
                MedicationCategory.GNRH_AGONIST,
            ).mapNotNull { category ->
                // F-PR3: cyproterone acetate is MtF-only. For FtM users
                // we drop any group whose medications are all CPA so
                // the antiandrogen home card stays clinically relevant
                // (spironolactone / bicalutamide / finasteride /
                // dutasteride still show). Sourced from _currentHrtGoal
                // (the auto-picker StateFlow) for the same reason as
                // the chip filter above — a goal change takes effect
                // immediately without waiting for a HomeSnapshot refresh.
                val groupsForUi = if (currentHrtGoal == HrtGoal.FTM) {
                    inputs.activeGroups.filterNot { group ->
                        group.medications.isNotEmpty() &&
                                group.medications.all { gm ->
                                    (gm.medicine?.selection as? MedicineSelection.Catalog)
                                        ?.medicationKey == MedicationKey.CYPROTERONE_ACETATE
                                }
                    }
                } else {
                    inputs.activeGroups
                }
                val cards = buildMainAntiandrogenCards(
                    groups = groupsForUi,
                    entries = homeEntries,
                    now = now,
                    zoneId = zoneId,
                    category = category,
                )
                cards.takeIf { it.isNotEmpty() }
                    ?.let { MainMedicationCategorySection(category = category, cards = it) }
            },
            todaySection = buildMainTodaySection(
                groups = scheduleGroups,
                entries = visibleEntries,
                now = now,
                zoneId = zoneId,
                includeUnloggedArchivedSlots = false,
                unloggedArchivedSlotCutoff = now,
            ),
            lastNightSection = buildMainLastNightSection(
                groups = scheduleGroups,
                entries = visibleEntries,
                now = now,
                zoneId = zoneId,
                includeUnloggedArchivedSlots = false,
                unloggedArchivedSlotCutoff = now,
            ),
            comingUpSection = buildMainComingUpSection(
                groups = scheduleGroups,
                entries = visibleEntries,
                now = now,
                zoneId = zoneId,
                includeUnloggedArchivedSlots = false,
                unloggedArchivedSlotCutoff = now,
            ),
            upcomingSection = buildMainUpcomingSection(
                groups = inputs.activeGroups,
                entries = homeEntries,
                now = now,
                zoneId = zoneId,
            ),
            timeZoneChangeNotice = timeZoneNotice,
            // v2 血检追踪 (BT-PR2): the "Recent Bloodwork" card. The
            // builder returns null when the user has no blood test
            // panels, in which case the home screen skips rendering
            // the card entirely.
            bloodTestCard = buildMainBloodTestCard(
                panels = bloodTestPanels,
                goal = currentHrtGoal,
                now = now.atZone(zoneId).toInstant(),
            ),
        )
    }

    private fun shouldExpandLowStockSection(
        storedExpanded: Boolean,
        stockWarnings: List<MedicineStockProjection>,
        acknowledgedWarningStates: Map<String, MedicineStockState>,
    ): Boolean {
        if (storedExpanded) {
            return true
        }
        val currentWarningStates = stockWarnings.toLowStockWarningStateMap()
        return currentWarningStates.any { (uuid, currentState) ->
            val acknowledgedState = acknowledgedWarningStates[uuid]
            acknowledgedState == null ||
                    acknowledgedState.lowStockSeverityRank() < currentState.lowStockSeverityRank()
        }
    }

    private fun List<MedicineStockProjection>.toLowStockWarningStateMap(): Map<String, MedicineStockState> {
        return mapNotNull { projection ->
            projection.state
                .takeIf { state -> state.lowStockSeverityRank() > 0 }
                ?.let { state -> projection.medicine.uuid.toString() to state }
        }.toMap()
    }

    private data class HomeTimeKey(
        val now: LocalDateTime,
        val date: LocalDate,
        val zoneId: ZoneId,
    )

    private data class HomeSnapshotRefreshKey(
        val date: LocalDate,
        val zoneId: String,
    )

    private data class KeyedHomeInputs(val key: HomeTimeKey, val inputs: HomeInputs)

    private fun HomeTimeKey.matches(snapshot: AppTimeSnapshot): Boolean {
        return date == snapshot.minute.toLocalDate() &&
                zoneId == snapshot.zone
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
