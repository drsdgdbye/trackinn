package pro.drsdgdbye.trackinn.ui.weight

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.WeightEntryEntity
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightHistoryScreen(
    onBack: () -> Unit,
    viewModel: WeightViewModel
) {
    val entries by viewModel.entries.collectAsState()
    val target by viewModel.weightTarget.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight_history)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("weight.history.back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                if (entries.size >= 2) {
                    WeightLineChart(entries = entries, targetWeight = target.toDouble())
                } else {
                    Text(
                        text = stringResource(R.string.weight_chart_min_entries),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(entries) { entry ->
                val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                Text(
                    text = "${dateFormat.format(Date(entry.recordedAt))} — ${formatWeightKg(entry.weightKg)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WeightLineChart(
    entries: List<WeightEntryEntity>,
    targetWeight: Double
) {
    val zone = ZoneId.systemDefault()
    val configuration = LocalConfiguration.current
    val chartLocale = configuration.locales[0]
    val sorted = remember(entries) { entries.sortedBy { it.recordedAt } }

    val labels = remember(sorted, zone, chartLocale) {
        sorted.map { entry ->
            val date = Instant.ofEpochMilli(entry.recordedAt).atZone(zone).toLocalDate()
            "${date.month.getDisplayName(java.time.format.TextStyle.SHORT, chartLocale)} ${date.year % 100}"
        }
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(sorted) {
        modelProducer.runTransaction {
            lineModel {
                series(sorted.map { it.weightKg })
            }
        }
    }

    val valueFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            if (index in labels.indices) labels[index] else " "
        }
    }

    val seriesColor = Color(0xFFF44336)
    val point = rememberShapeComponent(
        fill = Fill(seriesColor),
        shape = CircleShape
    )
    val lineLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(Fill(seriesColor)),
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(component = point, size = 8.dp)
                )
            )
        ),
        rangeProvider = remember(targetWeight) { WeightRangeProvider(targetWeight) }
    )

    val axisLabelColor = MaterialTheme.colorScheme.onSurface
    val axisLabelStyle = androidx.compose.ui.text.TextStyle(color = axisLabelColor)

    val decorations: List<Decoration> = if (targetWeight > 0) {
        val targetLabel = stringResource(R.string.weight_target_label)
        listOf(
            HorizontalLine(
                y = { targetWeight },
                line = rememberLineComponent(
                    fill = Fill(Color(0xFF4CAF50)),
                    thickness = 2.dp
                ),
                labelComponent = rememberTextComponent(
                    style = axisLabelStyle,
                ),
                label = { targetLabel }
            )
        )
    } else {
        emptyList()
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            lineLayer,
            startAxis = VerticalAxis.rememberStart(
                label = rememberTextComponent(
                    style = axisLabelStyle,
                ),
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = valueFormatter,
                label = rememberTextComponent(
                    style = axisLabelStyle,
                ),
            ),
            decorations = decorations
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxSize()
            .height(260.dp)
    )
}

private class WeightRangeProvider(
    private val targetWeight: Double
) : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val span = (maxY - minY).coerceAtLeast(1.0)
        val padding = span * 0.1
        val dataMin = minY - padding
        return if (targetWeight > 0) minOf(dataMin, targetWeight - padding) else dataMin
    }

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val span = (maxY - minY).coerceAtLeast(1.0)
        val padding = span * 0.1
        val dataMax = maxY + padding
        return if (targetWeight > 0) maxOf(dataMax, targetWeight + padding) else dataMax
    }
}
