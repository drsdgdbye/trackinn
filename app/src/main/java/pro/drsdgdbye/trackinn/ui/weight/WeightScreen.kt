package pro.drsdgdbye.trackinn.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.WeightEntryEntity
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    onHistoryClick: () -> Unit = {},
    viewModel: WeightViewModel
) {
    val entries by viewModel.entries.collectAsState()
    val target by viewModel.weightTarget.collectAsState()
    val weighInDay by viewModel.weightWeighInDay.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val parsedWeight = inputText.replace(",", ".").toDoubleOrNull()
    val isValid = parsedWeight != null && parsedWeight in 20.0..300.0

    val today = remember { LocalDate.now() }
    val isWeighInToday = WeightStats.isWeighInToday(weighInDay, today)
    val nextWeighIn = WeightStats.nextWeighInDate(weighInDay, today)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight)) },
                actions = {
                    IconButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.testTag("weight.history")
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.weight_history))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isWeighInToday) {
                        stringResource(R.string.weight_remind_today)
                    } else {
                        stringResource(R.string.weight_remind_next, dayName(nextWeighIn.dayOfWeek))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text(stringResource(R.string.weight_input_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weight.input")
                    )
                    Button(
                        onClick = {
                            if (isValid && parsedWeight != null) {
                                viewModel.addEntry(parsedWeight)
                                inputText = ""
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.testTag("weight.add")
                    ) {
                        Text(stringResource(R.string.weight_add))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (entries.isNotEmpty()) {
                itemsIndexed(entries) { index, entry ->
                    val previous = entries.getOrNull(index + 1)
                    WeightEntryItem(
                        entry = entry,
                        previousWeight = previous?.weightKg,
                        targetWeight = target.toDouble()
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.weight_no_entries),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
internal fun dayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.day_monday)
    DayOfWeek.TUESDAY -> stringResource(R.string.day_tuesday)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wednesday)
    DayOfWeek.THURSDAY -> stringResource(R.string.day_thursday)
    DayOfWeek.FRIDAY -> stringResource(R.string.day_friday)
    DayOfWeek.SATURDAY -> stringResource(R.string.day_saturday)
    DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday)
}

@Composable
private fun WeightEntryItem(
    entry: WeightEntryEntity,
    previousWeight: Double?,
    targetWeight: Double
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
    val delta = WeightStats.computeDelta(entry.weightKg, previousWeight)
    val remaining = WeightStats.computeRemaining(entry.weightKg, targetWeight)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = dateFormat.format(Date(entry.recordedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatWeightKg(entry.weightKg),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (delta != null) {
                    val deltaColor = if (delta < 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Text(
                        text = formatDelta(delta),
                        style = MaterialTheme.typography.bodyMedium,
                        color = deltaColor
                    )
                }
            }
            if (remaining != null) {
                val remainingText = if (remaining > 0) {
                    formatWeightKg(remaining)
                } else {
                    stringResource(R.string.weight_target_reached)
                }
                Text(
                    text = stringResource(R.string.weight_remaining, remainingText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun formatWeightKg(value: Double): String {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    return stringResource(R.string.weight_value_kg, String.format(locale, "%.1f", value))
}

@Composable
private fun formatDelta(delta: Double): String {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val sign = if (delta > 0) "+" else ""
    return String.format(locale, "%s%.1f", sign, delta)
}
