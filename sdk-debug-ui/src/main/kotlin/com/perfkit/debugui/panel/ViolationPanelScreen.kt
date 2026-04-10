package com.perfkit.debugui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perfkit.api.domain.ViolationCategory
import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.api.domain.ViolationSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViolationPanelScreen(viewModel: ViolationPanelViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.selectedEvent != null) {
        ViolationDetailScreen(
            event = uiState.selectedEvent!!,
            onBack = { viewModel.onSelectEvent(null) },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PerfKit — ${uiState.violations.size} violation${if (uiState.violations.size == 1) "" else "s"}",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (uiState.violations.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearViolations) {
                            Text("Clear", color = Color(0xFFFF6B6B))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Resumo por categoria
            SummaryRow(summaries = uiState.summaries)

            // Filtros por categoria
            CategoryFilterRow(
                selectedCategory = uiState.filter.category,
                onCategorySelected = viewModel::onFilterCategory,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Lista de violações
            if (uiState.violations.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.violations, key = { it.id }) { event ->
                        ViolationEventCard(
                            event = event,
                            onClick = { viewModel.onSelectEvent(event) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(summaries: List<ViolationSummary>) {
    if (summaries.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(summaries, key = { it.category.name }) { summary ->
            SummaryChip(summary)
        }
    }
}

@Composable
private fun SummaryChip(summary: ViolationSummary) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = summary.highestSeverity.color().copy(alpha = 0.15f),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = summary.count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = summary.highestSeverity.color(),
            )
            Text(
                text = summary.category.name.lowercase().replace('_', ' '),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selectedCategory: ViolationCategory?,
    onCategorySelected: (ViolationCategory?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
            )
        }
        items(ViolationCategory.entries, key = { it.name }) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(if (selectedCategory == category) null else category) },
                label = { Text(category.name.take(8)) },
            )
        }
    }
}

@Composable
private fun ViolationEventCard(
    event: ViolationEvent,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Severity indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(event.severity.color()),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = event.category.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = event.severity.color(),
                    )
                    Text(
                        text = event.timestamp.toTimeString(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = event.message.ifBlank { event.className ?: event.policyLabel ?: "?" },
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "thread: ${event.threadName}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViolationDetailScreen(
    event: ViolationEvent,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event.category.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Text(
                        "← Back",
                        modifier = Modifier
                            .padding(12.dp)
                            .clickable(onClick = onBack),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DetailField("Severity", event.severity.name, event.severity.color())
                DetailField("Source", event.policyLabel ?: event.source.name)
                DetailField("Thread", event.threadName)
                DetailField("Class", event.className ?: "—")
                DetailField("Time", event.timestamp.toFullTimeString())
                if (event.message.isNotBlank()) DetailField("Message", event.message)
            }
            if (event.stacktrace != null) {
                item {
                    Text("Stacktrace", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = event.stacktrace.orEmpty(),
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            modifier = Modifier.width(80.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, fontSize = 12.sp, color = valueColor)
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✅", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Nenhuma violação detectada",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Extensions ---

private fun ViolationSeverity.color(): Color = when (this) {
    ViolationSeverity.LOW -> Color(0xFF4CAF50)
    ViolationSeverity.MEDIUM -> Color(0xFFFFC107)
    ViolationSeverity.HIGH -> Color(0xFFFF5722)
    ViolationSeverity.CRITICAL -> Color(0xFFF44336)
}

private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
private val fullFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

private fun Long.toTimeString(): String = timeFormatter.format(Date(this))
private fun Long.toFullTimeString(): String = fullFormatter.format(Date(this))
