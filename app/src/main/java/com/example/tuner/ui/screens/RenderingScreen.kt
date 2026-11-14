package com.example.tuner.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.tuner.domain.ApplyOutcome
import com.example.tuner.system.BackendState
import com.example.tuner.system.Renderer
import com.example.tuner.ui.LocalContainer
import com.example.tuner.ui.RadioRow
import com.example.tuner.ui.SectionCard
import com.example.tuner.ui.StatusPill
import com.example.tuner.ui.theme.AppSpace
import com.example.tuner.ui.theme.AppTheme
import com.example.tuner.ui.theme.AppType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@StringRes
private fun Renderer.labelRes(): Int = when (this) {
    Renderer.DEFAULT -> R.string.rendering_default
    Renderer.SKIAGL -> R.string.rendering_skiagl
    Renderer.SKIAVK -> R.string.rendering_skiavk
}

class RenderingViewModel(private val c: AppContainer) : ViewModel() {
    private val _current = MutableStateFlow<Renderer?>(null)
    private val _pipeline = MutableStateFlow<String?>(null)
    private val _picked = MutableStateFlow<Renderer?>(null)
    private val _applying = MutableStateFlow(false)
    private val _message = MutableStateFlow<Pair<Int, String?>?>(null)

    val current: StateFlow<Renderer?> = _current
    val pipeline: StateFlow<String?> = _pipeline
    val picked: StateFlow<Renderer?> = _picked
    val applying: StateFlow<Boolean> = _applying
    val message: StateFlow<Pair<Int, String?>?> = _message
    val backend: StateFlow<BackendState> = c.executor.state

    init {
        viewModelScope.launch {
            c.executor.state.collect { if (it == BackendState.READY) refresh() }
        }
    }

    private suspend fun refresh() {
        _current.value = c.tuning.readRenderer()
        _pipeline.value = c.tuning.readPipeline(c.packageName)
    }

    fun pick(r: Renderer) { _picked.value = r }

    fun apply(target: Renderer) {
        if (_applying.value) return
        viewModelScope.launch {
            _applying.value = true
            _message.value = when (val o = c.tuning.applyRenderer(target)) {
                ApplyOutcome.Ok -> R.string.msg_rendering_ok to null
                is ApplyOutcome.Partial -> R.string.msg_partial to o.detail
                is ApplyOutcome.Failed -> R.string.msg_failed to o.detail
                ApplyOutcome.NoPrivilege -> R.string.detail_need_access to null
                ApplyOutcome.InvalidPackage -> R.string.msg_invalid to null
            }
            refresh()
            _applying.value = false
        }
    }
}

/** S05: renderer HWUI global. Peringatan wajib dan konfirmasi sebelum diterapkan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderingScreen(onBack: () -> Unit, onSetupAccess: () -> Unit) {
    val c = LocalContainer.current
    val vm: RenderingViewModel = viewModel(factory = viewModelFactory { initializer { RenderingViewModel(c) } })
    val current by vm.current.collectAsStateWithLifecycle()
    val pipeline by vm.pipeline.collectAsStateWithLifecycle()
    val picked by vm.picked.collectAsStateWithLifecycle()
    val applying by vm.applying.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val backend by vm.backend.collectAsStateWithLifecycle()
    val ready = backend == BackendState.READY
    val selected = picked ?: current ?: Renderer.DEFAULT
    var confirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rendering_title)) },
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
                    onClick = { vm.pick(Renderer.DEFAULT); vm.apply(Renderer.DEFAULT) },
                    enabled = ready && !applying,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.detail_reset_short)) }
                Button(
                    onClick = { confirm = true },
                    enabled = ready && !applying,
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
            SectionCard {
                Text(stringResource(R.string.rendering_warning_title), style = AppType.lg, color = AppTheme.colors.warning)
                Text(stringResource(R.string.rendering_warning_body), style = AppType.sm, color = AppTheme.colors.text)
            }

            if (!ready) {
                SectionCard {
                    Text(stringResource(R.string.detail_need_access), style = AppType.base, color = AppTheme.colors.text)
                    TextButton(onClick = onSetupAccess) { Text(stringResource(R.string.games_banner_action)) }
                }
            }

            current?.let {
                StatusPill(
                    text = stringResource(R.string.rendering_current, stringResource(it.labelRes())),
                    dot = if (it == Renderer.DEFAULT) AppTheme.colors.textMuted else AppTheme.colors.success,
                )
            }
            pipeline?.let { Text(stringResource(R.string.rendering_pipeline, it), style = AppType.sm, color = AppTheme.colors.textMuted) }

            SectionCard {
                Column(Modifier.selectableGroup()) {
                    Renderer.entries.forEach { r ->
                        RadioRow(
                            selected = r == selected,
                            enabled = !applying,
                            title = stringResource(r.labelRes()),
                            onClick = { vm.pick(r) },
                        )
                    }
                }
            }

            message?.let { (res, detail) ->
                SectionCard {
                    Text(stringResource(res), style = AppType.base, color = AppTheme.colors.text)
                    detail?.let { Text(it, style = AppType.mono, color = AppTheme.colors.textMuted) }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.rendering_confirm_title)) },
            text = { Text(stringResource(R.string.rendering_confirm_body)) },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
            confirmButton = {
                TextButton(onClick = { confirm = false; vm.apply(selected) }) { Text(stringResource(R.string.action_confirm)) }
            },
        )
    }
}
