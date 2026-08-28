package pro.drsdgdbye.trackinn.ui.meditation

import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationHistoryScreen(
    onBack: () -> Unit,
    viewModel: MeditationViewModel
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val activityContext = (LocalActivityResultRegistryOwner.current as? Context) ?: context

    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val filteredSessions = sessions.filter { session ->
        val sessionDate = session.startedAt
        val afterStart = startDate == null || sessionDate >= startDate!!
        val beforeEnd = endDate == null || sessionDate <= endDate!!
        afterStart && beforeEnd
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meditation_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.date_from), modifier = Modifier.padding(end = 8.dp))
                    if (startDate != null) {
                        Text(
                            text = dateFormat.format(Date(startDate!!)),
                            modifier = Modifier.clickable {
                                showDatePicker(activityContext, startDate) { startDate = it }
                            }
                        )
                        IconButton(onClick = { startDate = null }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    } else {
                        IconButton(onClick = {
                            showDatePicker(activityContext, null) { startDate = it }
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.task_select_date))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.date_to), modifier = Modifier.padding(end = 8.dp))
                    if (endDate != null) {
                        Text(
                            text = dateFormat.format(Date(endDate!!)),
                            modifier = Modifier.clickable {
                                showDatePicker(activityContext, endDate) { endDate = it }
                            }
                        )
                        IconButton(onClick = { endDate = null }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    } else {
                        IconButton(onClick = {
                            showDatePicker(activityContext, null) { endDate = it }
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.task_select_date))
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (filteredSessions.isEmpty()) {
                Text(
                    stringResource(R.string.no_sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredSessions) { session ->
                        SessionItem(session = session, dateFormat = dateFormat, timeFormat = timeFormat)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItem(
    session: MeditationSessionEntity,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateFormat.format(Date(session.startedAt)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.duration_minutes, session.durationMinutes),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeFormat.format(Date(session.startedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (session.wasCompleted) stringResource(R.string.completed) else stringResource(R.string.interrupted),
                style = MaterialTheme.typography.bodySmall,
                color = if (session.wasCompleted) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

private fun showDatePicker(context: Context, initialDate: Long?, onDateSelected: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    if (initialDate != null) {
        cal.timeInMillis = initialDate
    }
    DatePickerDialog(
        context,
        { _, y, m, d ->
            val newCal = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
            onDateSelected(newCal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
