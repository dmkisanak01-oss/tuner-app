package com.example.tuner.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.example.tuner.data.db.GameRow
import com.example.tuner.system.BackendState
import com.example.tuner.ui.LocalContainer
import com.example.tuner.ui.labelRes
import com.example.tuner.ui.theme.AppSpace
import com.example.tuner.ui.theme.AppTheme
import com.example.tuner.ui.theme.AppType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GamesViewModel(c: AppContainer) : ViewModel() {
    /** null = masih memuat. */
    val rows: StateFlow<List<GameRow>?> =
        c.games.observeRows().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val backend: StateFlow<BackendState> = c.executor.state

    init {
        viewModelScope.launch { c.games.scan() }
    }
}

/** S03: daftar game, dengan state loading, kosong, dan terisi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(onOpenGame: (String) -> Unit, onSetupAccess: () -> Unit) {
    val c = LocalContainer.current
    val vm: GamesViewModel = viewModel(factory = viewModelFactory { initializer { GamesViewModel(c) } })
    val rows by vm.rows.collectAsStateWithLifecycle()
    val backend by vm.backend.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.games_title)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (backend != BackendState.READY) {
                AccessBanner(backend = backend, onSetupAccess = onSetupAccess)
            }
            val list = rows
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                list.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(AppSpace.s24),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.games_empty), style = AppType.lg, color = AppTheme.colors.text)
                    Text(stringResource(R.string.games_empty_hint), style = AppType.sm, color = AppTheme.colors.textMuted)
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.packageName }) { row -> GameItem(row) { onOpenGame(row.packageName) } }
                }
            }
        }
    }
}

@Composable
private fun AccessBanner(backend: BackendState, onSetupAccess: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(AppSpace.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s12),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.games_banner_none), style = AppType.sm, color = AppTheme.colors.text)
            Text(stringResource(backend.labelRes()), style = AppType.xs, color = AppTheme.colors.textMuted)
        }
        TextButton(onClick = onSetupAccess) { Text(stringResource(R.string.games_banner_action)) }
    }
}

@Composable
private fun GameItem(row: GameRow, onClick: () -> Unit) {
    val summary = if (row.tuneEnabled == true) stringResource(R.string.games_tuned) else stringResource(R.string.games_default)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onClick).padding(horizontal = AppSpace.s16, vertical = AppSpace.s12),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(row.label, style = AppType.base, color = AppTheme.colors.text)
        Text("$summary · ${row.packageName}", style = AppType.sm, color = AppTheme.colors.textMuted)
    }
}
