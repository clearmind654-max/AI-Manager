package com.aimanager.feature.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aimanager.core.model.ModelScore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overview cards
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Today", "${uiState.totalTokensToday} tokens", "$${"%.3f".format(uiState.totalCostToday)}", Modifier.weight(1f))
                    StatCard("This Week", "${uiState.totalTokensWeek} tokens", "$${"%.2f".format(uiState.totalCostWeek)}", Modifier.weight(1f))
                }
            }

            // Provider usage breakdown
            if (uiState.usageByProvider.isNotEmpty()) {
                item { Text("Usage by Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(uiState.usageByProvider.entries.toList()) { (provider, tokens) ->
                    ProviderUsageRow(provider, tokens, uiState.totalTokensWeek.coerceAtLeast(1))
                }
            }

            // Model scores
            if (uiState.modelScores.isNotEmpty()) {
                item { Text("Model Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(uiState.modelScores.take(10)) { score ->
                    ModelScoreCard(score)
                }
            }

            // Recent usage
            if (uiState.recentUsage.isNotEmpty()) {
                item { Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(uiState.recentUsage.take(20)) { record ->
                    UsageRow(record.provider, record.model, record.tokensIn + record.tokensOut, record.latencyMs)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ProviderUsageRow(provider: String, tokens: Long, total: Long) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(provider, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(100.dp))
            LinearProgressIndicator(
                progress = { (tokens.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(8.dp),
            )
            Text("${tokens}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModelScoreCard(score: ModelScore) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(score.model, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(score.taskType.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${(score.acceptanceRate * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = when { score.acceptanceRate > 0.8f -> MaterialTheme.colorScheme.primary; score.acceptanceRate > 0.5f -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.error })
                Text("${score.totalAttempts} attempts", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun UsageRow(provider: String, model: String, tokens: Int, latencyMs: Long) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(provider.take(6), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(60.dp))
        Text(model.take(15), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text("${tokens}t", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
        Text("${latencyMs}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
