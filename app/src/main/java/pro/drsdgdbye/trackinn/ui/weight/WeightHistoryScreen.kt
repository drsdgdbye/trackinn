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
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
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
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
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
                    IconButton(onClick = onBack) {
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
            "${date.month.getDisplayName(TextStyle.SHORT, chartLocale)} ${date.year % 100}"
        }
    }

    val labelIndices = remember(sorted, zone) {
        val indices = mutableSetOf<Int>()
        var lastMonth: YearMonth? = null
        for ((index, entry) in sorted.withIndex()) {
            val date = Instant.ofEpochMilli(entry.recordedAt).atZone(zone).toLocalDate()
            val month = YearMonth.from(date)
            if (month != lastMonth) {
                indices.add(index)
                lastMonth = month
            }
        }
        indices
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
        )
    )

    val decorations: List<Decoration> = if (targetWeight > 0) {
        val targetLabel = stringResource(R.string.weight_target_label)
        listOf(
            HorizontalLine(
                y = { targetWeight },
                line = rememberLineComponent(
                    fill = Fill(Color(0xFF4CAF50)),
                    thickness = 2.dp
                ),
                labelComponent = rememberTextComponent(),
                label = { targetLabel }
            )
        )
    } else {
        emptyList()
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            lineLayer,
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = valueFormatter,
                itemPlacer = remember(labelIndices) { MonthStartItemPlacer(labelIndices) }
            ),
            decorations = decorations
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxSize()
            .height(260.dp)
    )
}

/** Подписывает на оси X только первую точку каждого месяца. */
private class MonthStartItemPlacer(
    private val labelIndices: Set<Int>
) : HorizontalAxis.ItemPlacer {
    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = labelIndices
        .map { it.toDouble() }
        .filter { it >= visibleXRange.start - 1.0 && it <= visibleXRange.endInclusive + 1.0 }

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
    ): List<Double> = labelIndices.map { it.toDouble() }

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = labelIndices.map { it.toDouble() }

    override fun getStartLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = 0f

    override fun getEndLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = 0f
}
