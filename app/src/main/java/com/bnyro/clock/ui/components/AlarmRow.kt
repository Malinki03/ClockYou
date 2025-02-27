package com.bnyro.clock.ui.components

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bnyro.clock.R
import com.bnyro.clock.obj.Alarm
import com.bnyro.clock.ui.model.AlarmModel
import com.bnyro.clock.util.AlarmHelper
import com.bnyro.clock.util.TimeHelper
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmRow(alarm: Alarm, alarmModel: AlarmModel) {
    val context = LocalContext.current
    ElevatedCard(
        onClick = {
            alarmModel.selectedAlarm = alarm
        },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val relativeTimeString = DateUtils.getRelativeTimeSpanString(
                    AlarmHelper.getAlarmTime(alarm),
                )
                alarm.label?.let {
                    Row(
                        modifier = Modifier
                            .padding(start = 5.dp, end = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Label, null)
                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = it,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = TimeHelper.millisToFormatted(alarm.time),
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 36.sp
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = "$relativeTimeString."
                )
            }

            Row(Modifier.padding(horizontal = 8.dp)) {
                when {
                    !alarm.repeat -> {
                        Text(text = stringResource(R.string.one_time))
                    }

                    alarm.isRepeatEveryday -> {
                        Text(text = stringResource(R.string.repeating))
                    }

                    alarm.isWeekends -> {
                        Text(text = stringResource(R.string.weekends))
                    }

                    alarm.isWeekdays -> {
                        Text(text = stringResource(R.string.weekdays))
                    }

                    else -> {
                        val daysOfWeek = remember {
                            AlarmHelper.getDaysOfWeekByLocale()
                        }
                        daysOfWeek.forEach { (day, index) ->
                            val enabled = alarm.days.contains(index)
                            Text(
                                text = day,
                                color = if (enabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.5f
                                    )
                                },
                                fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            var isEnabled by remember {
                mutableStateOf(alarm.enabled)
            }
            LaunchedEffect(alarm) {
                isEnabled = alarm.enabled
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { newValue ->
                    alarm.enabled = newValue
                    isEnabled = newValue
                    if (isEnabled) {
                        val millisRemainingForAlarm: Long = (AlarmHelper.getAlarmTime(alarm) - System.currentTimeMillis())
                        Toast.makeText(
                            context,
                            "${context.resources.getString(R.string.alarm_will_play)} ${TimeHelper.durationToFormatted(context, millisRemainingForAlarm.milliseconds)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    alarmModel.updateAlarm(context, alarm)
                }
            )
        }
    }
}
