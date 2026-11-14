package com.example.tuner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.tuner.R
import com.example.tuner.ui.SectionCard
import com.example.tuner.ui.theme.AppSpace
import com.example.tuner.ui.theme.AppTheme
import com.example.tuner.ui.theme.AppType

/** S01: peringatan risiko, wajib dicentang sebelum lanjut. */
@Composable
fun OnboardingScreen(onAccept: () -> Unit) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(AppSpace.s16),
        verticalArrangement = Arrangement.spacedBy(AppSpace.s16),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpace.s16),
        ) {
            Text(stringResource(R.string.onboarding_title), style = AppType.xl, color = AppTheme.colors.text)
            SectionCard {
                Text(stringResource(R.string.onboarding_p1), style = AppType.base, color = AppTheme.colors.text)
                Text(stringResource(R.string.onboarding_p2), style = AppType.base, color = AppTheme.colors.text)
                Text(stringResource(R.string.onboarding_p3), style = AppType.base, color = AppTheme.colors.text)
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = agreed, role = Role.Checkbox, onValueChange = { agreed = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = agreed, onCheckedChange = null)
            Text(stringResource(R.string.onboarding_check), style = AppType.base, color = AppTheme.colors.text, modifier = Modifier.padding(start = AppSpace.s12))
        }
        Button(onClick = onAccept, enabled = agreed, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.privilege_continue))
        }
    }
}
