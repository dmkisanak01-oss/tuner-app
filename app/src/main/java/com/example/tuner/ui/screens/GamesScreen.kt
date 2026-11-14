package com.example.tuner.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.tuner.data.db.GameRow
import com.example.tuner.system.BackendState
import com.example.tuner.system.GameMode
import com.example.tuner.ui.AppIcon
import com.example.tuner.ui.LocalContainer
import com.example.tuner.ui.SkeletonList
import com.example.tuner.ui.StatusPill
import com.example.tuner.ui.labelRes
import com.example.tuner.ui.shortLabelRes
import com.example.tuner.ui.theme.AppShape
import com.example.tuner.ui.theme.AppSpace
import com.example.tuner.ui.theme.AppTheme
import com.example.tuner.ui.theme.AppType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GamesViewModel(private val c: AppContainer) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /** null = masih memuat. Terfilter oleh pencarian (nama atau nama paket). */
    val rows: StateFlow<List<GameRow>?> = combine(c.games.observeRows(), _query) { list, q ->
        val needle = q.trim()
        if (needle.isEmpty()) list
        else list.filter { it.label.contains(needle, ignoreCase = true) || it.packageName.contains(needle, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val backend: StateFlow<BackendState> = c.executor.state

    init {
        viewModelScope.launch { c.games.scan() }
    }

    fun setQuery(value: String) { _query.value = value }

    fun togglePin(row: GameRow) {
        viewModelScope.launch { c.db.gameEntryDao().setPinned(row.packageName, !row.isPinned, System.currentTimeMillis()) }
    }
}

/** S03: daftar game. State: memuat (skeleton), kosong, tidak ada hasil pencarian, terisi, tanpa akses. */
@Composable
fun GamesScreen(onOpenGame: (String) -> Unit, onSetupAccess: () -> Unit, onOpenSettings: () -> Unit) {
    val c = LocalContainer.current
    val vm: GamesViewModel = viewModel(factory = viewModelFactory { initializer { GamesViewModel(c) } })
    val rows by vm.rows.collectAsStateWithLifecycle()
    val backend by vm.backend.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(start = AppSpace.s16, end = AppSpace.s8, top = AppSpace.s16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.games_title),
                    style = AppType.xl,
                    color = AppTheme.colors.text,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.open_settings))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AppSpace.s16, vertical = AppSpace.s8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.s8),
            ) {
                val ready = backend == BackendState.READY
                StatusPill(
                    text = stringResource(if (ready) R.string.games_access_ready else R.string.games_access_not_ready),
                    dot = if (ready) AppTheme.colors.success else AppTheme.colors.warning,
                )
                if (!ready) TextButton(onClick = onSetupAccess) { Text(stringResource(R.string.games_banner_action)) }
            }

            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                singleLine = true,
                shape = AppShape.pill,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpace.s16, vertical = AppSpace.s8),
            )

            val list = rows
            when {
                list == null -> SkeletonList()
                list.isEmpty() -> EmptyMessage(
                    title = stringResource(if (query.isBlank()) R.string.games_empty else R.string.games_no_results),
                    hint = if (query.isBlank()) stringResource(R.string.games_empty_hint) else null,
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.packageName }) { row ->
                        GameItem(
                            row = row,
                            onClick = { onOpenGame(row.packageName) },
                            onTogglePin = { vm.togglePin(row) },
                            onPlay = {
                                context.packageManager.getLaunchIntentForPackage(row.packageName)
                                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ?.let(context::startActivity)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(title: String, hint: String?) {
    Column(Modifier.fillMaxSize().padding(AppSpace.s24), verticalArrangement = Arrangement.Top) {
        Text(title, style = AppType.lg, color = AppTheme.colors.text)
        if (hint != null) Text(hint, style = AppType.sm, color = AppTheme.colors.textMuted)
    }
}

@Composable
private fun summaryOf(row: GameRow): String {
    val parts = mutableListOf<String>()
    if (row.isPinned) parts += stringResource(R.string.games_pinned)
    if (row.tuneEnabled == true && row.gameMode != null) {
        if (row.gameMode == GameMode.CUSTOM) {
            row.graphicsMode?.let { parts += stringResource(it.labelRes()) }
            row.fpsCap?.let { parts += stringResource(R.string.fps_value, it) }
            if (row.graphicsMode == null) parts += stringResource(row.gameMode.shortLabelRes())
        } else {
            parts += stringResource(row.gameMode.shortLabelRes())
        }
    } else {
        parts += stringResource(R.string.games_default)
    }
    return parts.joinToString(" · ")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameItem(row: GameRow, onClick: () -> Unit, onTogglePin: () -> Unit, onPlay: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(horizontal = AppSpace.s16, vertical = AppSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.s12),
        ) {
            AppIcon(row.packageName, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(row.label, style = AppType.base, color = AppTheme.colors.text, maxLines = 1)
                Text(summaryOf(row), style = AppType.sm, color = AppTheme.colors.textMuted, maxLines = 1)
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.games_play, row.label), tint = AppTheme.colors.accent)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (row.isPinned) R.string.games_unpin else R.string.games_pin)) },
                onClick = { menu = false; onTogglePin() },
            )
        }
    }
}
