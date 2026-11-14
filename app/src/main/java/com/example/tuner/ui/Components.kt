package com.example.tuner.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.tuner.ui.theme.AppShape
import com.example.tuner.ui.theme.AppSpace
import com.example.tuner.ui.theme.AppTheme
import com.example.tuner.ui.theme.AppType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ikon app dari PackageManager (dimuat di thread IO). Kotak abu-abu selama memuat atau kalau gagal. */
@Composable
fun AppIcon(pkg: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap() }.getOrNull()
        }
    }
    Box(modifier.size(size).clip(AppShape.md).background(AppTheme.colors.surface)) {
        bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

/** Kartu pengelompok isi layar. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(AppShape.lg)
            .background(AppTheme.colors.surface)
            .border(1.dp, AppTheme.colors.border, AppShape.lg)
            .padding(AppSpace.s16),
        verticalArrangement = Arrangement.spacedBy(AppSpace.s8),
        content = content,
    )
}

/** Chip status: titik berwarna + teks (status tidak hanya lewat warna). */
@Composable
fun StatusPill(text: String, dot: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(AppShape.pill)
            .background(AppTheme.colors.surface)
            .border(1.dp, AppTheme.colors.border, AppShape.pill)
            .padding(horizontal = AppSpace.s12, vertical = AppSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(AppSpace.s8))
        Text(text, style = AppType.xs, color = AppTheme.colors.text)
    }
}

/** Baris pilihan tunggal. Target sentuh minimal 48dp, seluruh baris bisa diketuk. */
@Composable
fun RadioRow(
    selected: Boolean,
    enabled: Boolean,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
            selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f).padding(start = AppSpace.s12)) {
            Text(title, style = AppType.base, color = if (enabled) AppTheme.colors.text else AppTheme.colors.textMuted)
            if (subtitle != null) Text(subtitle, style = AppType.xs, color = AppTheme.colors.textMuted)
        }
        if (trailing != null) Text(trailing, style = AppType.sm, color = AppTheme.colors.textMuted)
    }
}

/** Placeholder berdenyut untuk daftar yang sedang dimuat (bukan spinner layar penuh). */
@Composable
fun SkeletonList(count: Int = 6) {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Column(Modifier.fillMaxWidth().alpha(alpha)) {
        repeat(count) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AppSpace.s16, vertical = AppSpace.s12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.s12),
            ) {
                Box(Modifier.size(48.dp).clip(AppShape.md).background(AppTheme.colors.surface))
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.s8)) {
                    Box(Modifier.width(160.dp).size(width = 160.dp, height = 14.dp).clip(AppShape.sm).background(AppTheme.colors.surface))
                    Box(Modifier.size(width = 100.dp, height = 10.dp).clip(AppShape.sm).background(AppTheme.colors.surface))
                }
            }
        }
    }
}
