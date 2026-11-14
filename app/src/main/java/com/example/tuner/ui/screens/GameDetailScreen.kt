package com.example.tuner.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.tuner.ui.AppIcon
import com.example.tuner.ui.RadioRow
import com.example.tuner.ui.SectionCard
import com.example.tuner.ui.StatusPill
import com.example.tuner.ui.percentLabel
import com.example.tuner.ui.shortLabelRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tuner.AppContainer
import com.example.tuner.R
import com.example.tuner.data.db.ApplyLogEntity
import com.example.tuner.data.db.GraphicsMode
import com.example.tuner.domain.ApplyOutcome
import com.example.tuner.system.BackendState
import com.example.tuner.system.GameMode
import com.example.tuner.system.ModeInfo
import com.example.tuner.ui.LocalContainer
import com.example.tuner.ui.labelRes
import com.example.tuner.ui.theme.AppSpace
import com.example.tuner.ui.theme.AppTheme
import com.example.tuner.ui.theme.AppType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiMessage(@StringRes val res: Int, val detail: String? = null, val isError: Boolean = false)

private val FPS_CHOICES = listOf(0, 30, 60, 90)

class GameDetailViewModel(private val c: AppContainer, private val pkg: String) : ViewModel() {
    private val pickedMode = MutableStateFlow<GameMode?>(null)
    private val pickedGraphics = MutableStateFlow<GraphicsMode?>(null)
    private val pickedFps = MutableStateFlow<Int?>(null)
    private val _applying = MutableStateFlow(false)
    private val _message = MutableStateFlow<UiMessage?>(null)
    private val _diagnosis = MutableStateFlow<String?>(null)
    private val _modes = MutableStateFlow<ModeInfo?>(null)
    private val profile = c.db.gameProfileDao().observe(pkg)

    val label: StateFlow<String?> =
        c.db.gameEntryDao().observeLabel(pkg).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val backend: StateFlow<BackendState> = c.executor.state
    val applying: StateFlow<Boolean> = _applying
    val message: StateFlow<UiMessage?> = _message
    val diagnosis: StateFlow<String?> = _diagnosis
    /** Mode yang diumumkan game. null = belum terbaca (semua opsi tetap boleh dicoba). */
    val modes: StateFlow<ModeInfo?> = _modes
    val lastLog: StateFlow<ApplyLogEntity?> =
        c.db.applyLogDao().observeLast(pkg).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedMode: StateFlow<GameMode> = combine(pickedMode, profile, _modes) { p, prof, info ->
        p ?: prof?.gameMode ?: info?.current ?: GameMode.STANDARD
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameMode.STANDARD)

    val selectedGraphics: StateFlow<GraphicsMode> = combine(pickedGraphics, profile) { p, prof ->
        p ?: prof?.graphicsMode ?: GraphicsMode.HIGH
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphicsMode.HIGH)

    val selectedFps: StateFlow<Int> = combine(pickedFps, profile) { p, prof ->
        p ?: prof?.fpsCap ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            c.executor.state.collect { if (it == BackendState.READY) refreshModes() }
        }
    }

    private suspend fun refreshModes() {
        _modes.value = c.tuning.readModes(pkg)
    }

    fun selectMode(mode: GameMode) { pickedMode.value = mode }
    fun selectGraphics(mode: GraphicsMode) { pickedGraphics.value = mode }
    fun selectFps(fps: Int) { pickedFps.value = fps }

    fun applySelected() = execute {
        when (val mode = selectedMode.value) {
            GameMode.CUSTOM -> c.tuning.applyCustom(pkg, selectedGraphics.value, selectedFps.value)
            else -> c.tuning.applyGameMode(pkg, mode)
        }
    }

    fun resetToDefault() = execute {
        val outcome = c.tuning.resetGame(pkg)
        pickedMode.value = null
        pickedGraphics.value = null
        pickedFps.value = null
        outcome
    }

    fun diagnose() {
        if (_applying.value) return
        viewModelScope.launch {
            _applying.value = true
            _diagnosis.value = c.tuning.diagnose(pkg)
            _applying.value = false
        }
    }

    private fun execute(block: suspend () -> ApplyOutcome) {
        if (_applying.value) return // cegah tap ganda
        viewModelScope.launch {
            _applying.value = true
            _message.value = null
            val outcome = block()
            _message.value = when (outcome) {
                ApplyOutcome.Ok -> UiMessage(R.string.msg_ok)
                is ApplyOutcome.Partial -> UiMessage(R.string.msg_partial, outcome.detail)
                is ApplyOutcome.Failed ->
                    if (outcome.detail.contains("not supported", ignoreCase = true))
                        UiMessage(R.string.msg_unsupported, outcome.detail, isError = true)
                    else UiMessage(R.string.msg_failed, outcome.detail, isError = true)
                ApplyOutcome.NoPrivilege -> UiMessage(R.string.detail_need_access, isError = true)
                ApplyOutcome.InvalidPackage -> UiMessage(R.string.msg_invalid, isError = true)
            }
            refreshModes()
            _applying.value = false
        }
    }
}

/** S04: header game, mode yang didukung, opsi Kustom (resolusi + FPS), tombol aksi tetap di bawah. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(pkg: String, onBack: () -> Unit, onSetupAccess: () -> Unit) {
    val c = LocalContainer.current
    val vm: GameDetailViewModel = viewModel(key = pkg, factory = viewModelFactory { initializer { GameDetailViewModel(c, pkg) } })
    val label by vm.label.collectAsStateWithLifecycle()
    val backend by vm.backend.collectAsStateWithLifecycle()
    val applying by vm.applying.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val diagnosis by vm.diagnosis.collectAsStateWithLifecycle()
    val selected by vm.selectedMode.collectAsStateWithLifecycle()
    val graphics by vm.selectedGraphics.collectAsStateWithLifecycle()
    val fps by vm.selectedFps.collectAsStateWithLifecycle()
    val modes by vm.modes.collectAsStateWithLifecycle()
    val lastLog by vm.lastLog.collectAsStateWithLifecycle()
    val ready = backend == BackendState.READY
    val selectedSupported = modes?.available?.contains(selected) ?: true
    var advanced by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(AppTheme.colors.bg).navigationBarsPadding().padding(AppSpace.s16),
                horizontalArrangement = Arrangement.spacedBy(AppSpace.s12),
            ) {
                OutlinedButton(
                    onClick = vm::resetToDefault,
                    enabled = ready && !applying,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.detail_reset_short)) }
                Button(
                    onClick = vm::applySelected,
                    enabled = ready && !applying && selectedSupported,
                    modifier = Modifier.weight(1.4f).heightIn(min = 48.dp),
                ) {
                    if (applying) CircularProgressIndicator(Modifier.size(20.dp).padding(end = AppSpace.s8), strokeWidth = 2.dp)
                    Text(stringResource(R.string.detail_apply))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(AppSpace.s16),
            verticalArrangement = Arrangement.spacedBy(AppSpace.s16),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.s16)) {
                AppIcon(pkg, 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.s4)) {
                    Text(label ?: pkg, style = AppType.lg, color = AppTheme.colors.text, maxLines = 2)
                    Text(pkg, style = AppType.xs, color = AppTheme.colors.textMuted, maxLines = 1)
                }
            }
            modes?.current?.let { cur ->
                StatusPill(
                    text = stringResource(R.string.detail_active_mode, stringResource(cur.shortLabelRes())),
                    dot = if (cur == GameMode.STANDARD) AppTheme.colors.textMuted else AppTheme.colors.success,
                )
            }

            if (!ready) {
                SectionCard {
                    Text(stringResource(R.string.detail_need_access), style = AppType.base, color = AppTheme.colors.text)
                    TextButton(onClick = onSetupAccess) { Text(stringResource(R.string.games_banner_action)) }
                }
            }

            SectionCard {
                Text(stringResource(R.string.detail_game_mode), style = AppType.lg, color = AppTheme.colors.text)
                Column(Modifier.selectableGroup()) {
                    GameMode.entries.forEach { mode ->
                        val supported = modes?.available?.contains(mode) ?: true
                        RadioRow(
                            selected = mode == selected,
                            enabled = supported && !applying,
                            title = stringResource(mode.labelRes()),
                            subtitle = if (supported) null else stringResource(R.string.mode_unsupported),
                            onClick = { vm.selectMode(mode) },
                        )
                    }
                }
            }

            if (selected == GameMode.CUSTOM) {
                SectionCard {
                    Text(stringResource(R.string.graphics_title), style = AppType.lg, color = AppTheme.colors.text)
                    Text(stringResource(R.string.custom_hint), style = AppType.sm, color = AppTheme.colors.textMuted)
                    Column(Modifier.selectableGroup()) {
                        GraphicsMode.entries.forEach { g ->
                            RadioRow(
                                selected = g == graphics,
                                enabled = !applying,
                                title = stringResource(g.labelRes()),
                                trailing = g.percentLabel(),
                                onClick = { vm.selectGraphics(g) },
                            )
                        }
                    }
                }
                SectionCard {
                    Text(stringResource(R.string.fps_title), style = AppType.lg, color = AppTheme.colors.text)
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.s8)) {
                        FPS_CHOICES.forEach { value ->
                            FilterChip(
                                selected = value == fps,
                                onClick = { vm.selectFps(value) },
                                enabled = !applying,
                                label = { Text(if (value == 0) stringResource(R.string.fps_default) else value.toString()) },
                            )
                        }
                    }
                }
            }

            message?.let { m ->
                SectionCard {
                    Text(
                        stringResource(m.res),
                        style = AppType.base,
                        color = if (m.isError) AppTheme.colors.danger else AppTheme.colors.success,
                    )
                    m.detail?.let { Text(it, style = AppType.mono, color = AppTheme.colors.textMuted) }
                }
            }

            TextButton(onClick = { advanced = !advanced }) {
                Text(stringResource(if (advanced) R.string.detail_advanced_hide else R.string.detail_advanced_show))
            }
            if (advanced) {
                OutlinedButton(onClick = vm::diagnose, enabled = ready && !applying, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.detail_diagnose))
                }
                diagnosis?.let {
                    SelectionContainer {
                        Text(it, style = AppType.mono, color = AppTheme.colors.text, modifier = Modifier.fillMaxWidth())
                    }
                }
                Text(stringResource(R.string.detail_last_log), style = AppType.lg, color = AppTheme.colors.text)
                val log = lastLog
                SelectionContainer {
                    Text(
                        text = if (log == null) stringResource(R.string.detail_no_log)
                        else "$ ${log.command}\nexit ${log.exitCode}\n${log.output}",
                        style = AppType.mono,
                        color = AppTheme.colors.text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
