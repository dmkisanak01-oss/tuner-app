package com.example.tuner.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.example.tuner.domain.ApplyOutcome
import com.example.tuner.system.BackendState
import com.example.tuner.system.GameMode
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

class GameDetailViewModel(private val c: AppContainer, private val pkg: String) : ViewModel() {
    private val picked = MutableStateFlow<GameMode?>(null)
    private val _applying = MutableStateFlow(false)
    private val _message = MutableStateFlow<UiMessage?>(null)

    val label: StateFlow<String?> =
        c.db.gameEntryDao().observeLabel(pkg).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val backend: StateFlow<BackendState> = c.executor.state
    val applying: StateFlow<Boolean> = _applying
    val message: StateFlow<UiMessage?> = _message
    val lastLog: StateFlow<ApplyLogEntity?> =
        c.db.applyLogDao().observeLast(pkg).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Mode terpilih: pilihan user kalau ada, kalau tidak mode yang tersimpan di profil, kalau tidak Standar. */
    val selectedMode: StateFlow<GameMode> = combine(picked, c.db.gameProfileDao().observe(pkg)) { p, profile ->
        p ?: profile?.gameMode ?: GameMode.STANDARD
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameMode.STANDARD)

    fun select(mode: GameMode) { picked.value = mode }

    fun applySelected() = execute { c.tuning.applyGameMode(pkg, selectedMode.value) }

    fun resetToDefault() = execute {
        val outcome = c.tuning.resetGame(pkg)
        picked.value = null
        outcome
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
                is ApplyOutcome.Failed -> UiMessage(R.string.msg_failed, outcome.detail, isError = true)
                ApplyOutcome.NoPrivilege -> UiMessage(R.string.detail_need_access, isError = true)
                ApplyOutcome.InvalidPackage -> UiMessage(R.string.msg_invalid, isError = true)
            }
            _applying.value = false
        }
    }
}

/** S04 (M1: hanya Game Mode). State: default, loading, berhasil, gagal, tanpa akses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(pkg: String, onBack: () -> Unit, onSetupAccess: () -> Unit) {
    val c = LocalContainer.current
    val vm: GameDetailViewModel = viewModel(key = pkg, factory = viewModelFactory { initializer { GameDetailViewModel(c, pkg) } })
    val label by vm.label.collectAsStateWithLifecycle()
    val backend by vm.backend.collectAsStateWithLifecycle()
    val applying by vm.applying.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val selected by vm.selectedMode.collectAsStateWithLifecycle()
    val lastLog by vm.lastLog.collectAsStateWithLifecycle()
    val ready = backend == BackendState.READY

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(label ?: pkg, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(AppSpace.s16).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpace.s16),
        ) {
            Text(stringResource(R.string.detail_game_mode), style = AppType.lg, color = AppTheme.colors.text)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                GameMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { vm.select(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, GameMode.entries.size),
                        enabled = !applying,
                    ) { Text(stringResource(mode.labelRes())) }
                }
            }

            if (!ready) {
                Text(stringResource(R.string.detail_need_access), style = AppType.sm, color = AppTheme.colors.text)
                TextButton(onClick = onSetupAccess) { Text(stringResource(R.string.games_banner_action)) }
            }

            Button(onClick = vm::applySelected, enabled = ready && !applying, modifier = Modifier.fillMaxWidth()) {
                if (applying) CircularProgressIndicator(Modifier.size(20.dp).padding(end = AppSpace.s8), strokeWidth = 2.dp)
                Text(stringResource(R.string.detail_apply))
            }
            OutlinedButton(onClick = vm::resetToDefault, enabled = ready && !applying, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.detail_reset))
            }

            message?.let { m ->
                Text(
                    stringResource(m.res) + (m.detail?.let { "\n$it" } ?: ""),
                    style = AppType.sm,
                    color = if (m.isError) AppTheme.colors.danger else AppTheme.colors.text,
                )
            }

            Text(stringResource(R.string.detail_last_log), style = AppType.lg, color = AppTheme.colors.text)
            val log = lastLog
            Text(
                text = if (log == null) stringResource(R.string.detail_no_log)
                else "$ ${log.command}\nexit ${log.exitCode}\n${log.output}",
                style = AppType.mono,
                color = AppTheme.colors.text,
                modifier = Modifier.fillMaxWidth().padding(AppSpace.s12),
            )
        }
    }
}
