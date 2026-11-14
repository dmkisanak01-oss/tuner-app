package com.example.tuner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tuner.R
import com.example.tuner.data.prefs.PrivilegeMode
import com.example.tuner.system.BackendState
import com.example.tuner.ui.LocalContainer
import com.example.tuner.ui.labelRes
import com.example.tuner.ui.theme.AppSpace
import com.example.tuner.ui.theme.AppTheme
import com.example.tuner.ui.theme.AppType
import kotlinx.coroutines.launch

/** S02: pilih Shizuku atau Root, lihat statusnya. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivilegeScreen(onDone: () -> Unit) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val mode by c.settings.privilegeMode.collectAsStateWithLifecycle(initialValue = PrivilegeMode.NONE)
    val shizukuState by c.shizuku.state.collectAsStateWithLifecycle()
    val rootState by c.root.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.privilege_title)) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(AppSpace.s16),
            verticalArrangement = Arrangement.spacedBy(AppSpace.s16),
        ) {
            Text(stringResource(R.string.privilege_intro), style = AppType.base, color = AppTheme.colors.text)

            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AppSpace.s12)) {
                OptionCard(
                    title = stringResource(R.string.privilege_shizuku),
                    description = stringResource(R.string.privilege_shizuku_desc),
                    status = stringResource(shizukuState.labelRes()),
                    selected = mode == PrivilegeMode.SHIZUKU,
                    onSelect = {
                        scope.launch {
                            c.settings.setPrivilegeMode(PrivilegeMode.SHIZUKU)
                            c.shizuku.refresh()
                        }
                    },
                )
                OptionCard(
                    title = stringResource(R.string.privilege_root),
                    description = stringResource(R.string.privilege_root_desc),
                    status = stringResource(rootState.labelRes()),
                    selected = mode == PrivilegeMode.ROOT,
                    onSelect = {
                        scope.launch {
                            c.settings.setPrivilegeMode(PrivilegeMode.ROOT)
                            c.root.refresh()
                        }
                    },
                )
            }

            if (mode == PrivilegeMode.SHIZUKU && shizukuState == BackendState.NEEDS_PERMISSION) {
                Button(onClick = { c.shizuku.requestPermission() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.privilege_grant))
                }
            }

            Spacer(Modifier.height(AppSpace.s8))
            val ready = (mode == PrivilegeMode.SHIZUKU && shizukuState == BackendState.READY) ||
                (mode == PrivilegeMode.ROOT && rootState == BackendState.READY)
            Button(onClick = onDone, enabled = ready, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privilege_continue))
            }
        }
    }
}

@Composable
private fun OptionCard(title: String, description: String, status: String, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, AppTheme.colors.accent)
        else androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
    ) {
        Column(Modifier.padding(AppSpace.s16), verticalArrangement = Arrangement.spacedBy(AppSpace.s4)) {
            Text(title, style = AppType.lg, color = AppTheme.colors.text)
            Text(description, style = AppType.sm, color = AppTheme.colors.textMuted)
            Text(status, style = AppType.xs, color = AppTheme.colors.text)
        }
    }
}
