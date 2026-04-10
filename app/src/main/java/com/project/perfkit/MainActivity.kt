package com.project.perfkit

import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perfkit.core.PerfKit
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HomeScreen(
                    onTriggerDiskRead = ::triggerDiskRead,
                    onTriggerDiskWrite = ::triggerDiskWrite,
                    onTriggerSlowCall = ::triggerSlowCall,
                    onTriggerLeakedResource = ::triggerLeakedResource,
                    onOpenPanel = { PerfKit.openDebugPanel(this) },
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Violation triggers — propositais para demonstração
    // ------------------------------------------------------------------

    /** Leitura síncrona na main thread → DiskReadViolation */
    private fun triggerDiskRead() {
        runCatching { File(filesDir, "perfkit_demo.txt").readText() }
    }

    /** Escrita síncrona na main thread → DiskWriteViolation */
    private fun triggerDiskWrite() {
        runCatching {
            File(filesDir, "perfkit_demo.txt")
                .writeText("written-at-${System.currentTimeMillis()}")
        }
    }

    /**
     * [StrictMode.noteSlowCall] marca explicitamente uma operação lenta.
     * Mais confiável para demo do que simular latência real.
     * → SlowCallViolation
     */
    private fun triggerSlowCall() {
        StrictMode.noteSlowCall("perfkit-demo-slow-call")
    }

    /**
     * Cria um [java.io.Closeable] sem fechá-lo.
     * StrictMode detecta no próximo GC → LeakedClosableViolation.
     *
     * Nota: a violação não é imediata — depende do GC.
     * O log "LeakedResource trigger fired" confirmará que o código rodou.
     */
    @Suppress("unused")
    private fun triggerLeakedResource() {
        val leaking = File(filesDir, "perfkit_demo.txt").inputStream()
        // Intencionalmente não fechamos — o GC eventualmente detectará
        android.util.Log.d("PerfKit/Demo", "LeakedResource trigger fired — violation appears after GC")
    }
}

// ------------------------------------------------------------------
// Composables
// ------------------------------------------------------------------

@Composable
private fun HomeScreen(
    onTriggerDiskRead: () -> Unit,
    onTriggerDiskWrite: () -> Unit,
    onTriggerSlowCall: () -> Unit,
    onTriggerLeakedResource: () -> Unit,
    onOpenPanel: () -> Unit,
) {
    var totalTriggered by remember { mutableIntStateOf(0) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // Header
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "PerfKit",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "StrictMode Debug Observability",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Status card
            StatusCard(totalTriggered = totalTriggered)

            Spacer(Modifier.height(4.dp))

            Text(
                "Trigger Violations",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Trigger buttons
            TriggerButton(
                label = "Disk Read",
                description = "Síncrono na main thread",
                severity = "MEDIUM",
                severityColor = Color(0xFFFFC107),
                onClick = { totalTriggered++; onTriggerDiskRead() },
            )

            TriggerButton(
                label = "Disk Write",
                description = "Síncrono na main thread",
                severity = "MEDIUM",
                severityColor = Color(0xFFFF9800),
                onClick = { totalTriggered++; onTriggerDiskWrite() },
            )

            TriggerButton(
                label = "Slow Call",
                description = "StrictMode.noteSlowCall()",
                severity = "MEDIUM",
                severityColor = Color(0xFF9C27B0),
                onClick = { totalTriggered++; onTriggerSlowCall() },
            )

            TriggerButton(
                label = "Leaked Resource",
                description = "Detectado no próximo GC",
                severity = "HIGH",
                severityColor = Color(0xFFFF5722),
                onClick = { totalTriggered++; onTriggerLeakedResource() },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onOpenPanel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E)),
            ) {
                Text(
                    "Open Violations Panel  →",
                    modifier = Modifier.padding(vertical = 6.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(totalTriggered: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (totalTriggered > 0) Color(0xFFF3E5F5) else Color(0xFFE8F5E9),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("SDK ativo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    if (totalTriggered == 0) "Nenhuma violação disparada ainda"
                    else "$totalTriggered trigger${if (totalTriggered > 1) "s" else ""} enviado${if (totalTriggered > 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TriggerButton(
    label: String,
    description: String,
    severity: String,
    severityColor: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                severity,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = severityColor,
                modifier = Modifier
                    .background(severityColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHomeScreen() {
    MaterialTheme {
        HomeScreen({}, {}, {}, {}, {})
    }
}
