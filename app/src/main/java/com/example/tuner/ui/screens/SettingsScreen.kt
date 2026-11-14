package com.example.tuner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tuner.R
import com.example.tuner.ui.LocalContainer
import com.example.tuner.ui.SectionCard
import com.example.tuner.ui.theme.AppSpace
import com.example.tuner.ui.theme.AppTheme
import com.example.tuner.ui.theme.AppType
import kotlinx.coroutines.launch

/** S10 + S11: pintu ke akses dan rendering, reset semua, tentang. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onAccess: () -> Unit, onRendering: () -> Unit) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(AppSpace.s16),
            verticalArrangement = Arrangement.spacedBy(AppSpace.s16),
        ) {
            SectionCard {
                SettingsRow(stringResource(R.string.settings_access), stringResource(R.string.settings_access_desc), onAccess)
                SettingsRow(stringResource(R.string.settings_rendering), stringResource(R.string.settings_rendering_desc), onRendering)
                SettingsRow(
                    stringResource(R.string.settings_reset_all),
                    stringResource(R.string.settings_reset_all_desc),
                    { confirmReset = true },
                    danger = true,
                )
            }
            resultText?.let { Text(stringResource(it), style = AppType.sm, color = AppTheme.colors.text) }
            SectionCard {
                Text(stringResource(R.string.settings_about), style = AppType.lg, color = AppTheme.colors.text)
                Text(stringResource(R.string.settings_about_body), style = AppType.sm, color = AppTheme.colors.textMuted)
                Text(stringResource(R.string.settings_version, "0.2.0"), style = AppType.xs, color = AppTheme.colors.textMuted)
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        resultText = when (c.tuning.resetAll()) {
                            com.example.tuner.domain.ApplyOutcome.Ok -> R.string.msg_reset_ok
                            com.example.tuner.domain.ApplyOutcome.NoPrivilege -> R.string.detail_need_access
                            else -> R.string.msg_partial
                        }
                    }
                }) { Text(stringResource(R.string.action_confirm), color = AppTheme.colors.danger) }
            },
        )
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit, danger: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = AppSpace.s8),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = AppType.base, color = if (danger) AppTheme.colors.danger else AppTheme.colors.text)
        Text(subtitle, style = AppType.sm, color = AppTheme.colors.textMuted)
    }
}
