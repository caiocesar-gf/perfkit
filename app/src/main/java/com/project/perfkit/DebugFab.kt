package com.project.perfkit

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perfkit.core.PerfKit

/**
 * Persistent debug entry point for the PerfKit demo app.
 *
 * - Tap: opens the PerfKit violation panel.
 * - Long press: opens the quick actions sheet.
 * - Renders only when [BuildConfig.DEBUG] is true.
 *
 * Intended to be placed at the root level of the app scaffold so it stays
 * visible across all demo screens.
 */
@Composable
fun DebugFab(
    modifier: Modifier = Modifier,
    onOpenPanel: () -> Unit,
    onTriggerDiskRead: () -> Unit,
    onTriggerDiskWrite: () -> Unit,
    onTriggerSlowCall: () -> Unit,
) {
    if (!BuildConfig.DEBUG) return

    var showQuickActions by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF1A1A2E))
            .combinedClickable(
                onClick = onOpenPanel,
                onLongClick = { showQuickActions = true },
            )
            .semantics {
                contentDescription = "Open PerfKit debug panel. Long press for quick actions."
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }

    if (showQuickActions) {
        QuickActionsSheet(
            onDismiss = { showQuickActions = false },
            onOpenPanel = {
                showQuickActions = false
                onOpenPanel()
            },
            onTriggerDiskRead = {
                onTriggerDiskRead()
                showQuickActions = false
            },
            onTriggerDiskWrite = {
                onTriggerDiskWrite()
                showQuickActions = false
            },
            onTriggerSlowCall = {
                onTriggerSlowCall()
                showQuickActions = false
            },
            onClear = {
                PerfKit.clearViolations()
                showQuickActions = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSheet(
    onDismiss: () -> Unit,
    onOpenPanel: () -> Unit,
    onTriggerDiskRead: () -> Unit,
    onTriggerDiskWrite: () -> Unit,
    onTriggerSlowCall: () -> Unit,
    onClear: () -> Unit,
) {
    val apiLevel = Build.VERSION.SDK_INT
    val isFullCapture = apiLevel >= 28

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("PerfKit", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "StrictMode Debug Observability",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge("Monitoring Active", Color(0xFF4CAF50))
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge("Debug Only", Color(0xFF2196F3))
                StatusBadge(
                    text = if (isFullCapture) "API $apiLevel · Full Capture" else "API $apiLevel · Logcat",
                    color = if (isFullCapture) Color(0xFF4CAF50) else Color(0xFFFFC107),
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Text(
                "Quick Actions",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            QuickActionRow(
                icon = Icons.Default.BugReport,
                label = "Open PerfKit Panel",
                onClick = onOpenPanel,
            )
            QuickActionRow(
                icon = Icons.Default.Storage,
                label = "Trigger Disk Read",
                onClick = onTriggerDiskRead,
            )
            QuickActionRow(
                icon = Icons.Default.Storage,
                label = "Trigger Disk Write",
                onClick = onTriggerDiskWrite,
            )
            QuickActionRow(
                icon = Icons.Default.Speed,
                label = "Trigger Slow Call",
                onClick = onTriggerSlowCall,
            )
            QuickActionRow(
                icon = Icons.Default.Delete,
                label = "Clear Captured Events",
                onClick = onClear,
                tint = Color(0xFFFF6B6B),
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(label, fontSize = 14.sp, color = tint)
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
