package com.bnyro.clock.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.bnyro.clock.obj.Alarm
import com.bnyro.clock.receivers.AlarmReceiver
import com.bnyro.clock.ui.MainActivity
import java.util.Calendar
import java.util.GregorianCalendar

object AlarmHelper {
    const val EXTRA_ID = "alarm_id"
    val availableDays = listOf("S", "M", "T", "W", "T", "F", "S")

    fun enqueue(context: Context, alarm: Alarm) {
        cancel(context, alarm)
        if (!alarm.enabled) {
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmInfo = AlarmManager.AlarmClockInfo(
            getAlarmScheduleTime(alarm),
            getOpenAppIntent(context, alarm)
        )
        alarmManager.setAlarmClock(alarmInfo, getPendingIntent(context, alarm))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun hasPermission(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun cancel(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(getPendingIntent(context, alarm))
    }

    private fun getPendingIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context.applicationContext, AlarmReceiver::class.java)
            .putExtra(EXTRA_ID, alarm.id)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOpenAppIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context.applicationContext, MainActivity::class.java)
            .putExtra(EXTRA_ID, alarm.id)
        return PendingIntent.getActivity(
            context.applicationContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Calculate the epoch time for scheduling an alarm
     */
    private fun getAlarmScheduleTime(alarm: Alarm): Long {
        val calendar = GregorianCalendar()
        calendar.time = TimeHelper.currentTime

        // reset the calendar time to the start of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // add the milliseconds from the new alarm
        calendar.add(Calendar.MILLISECOND, alarm.time.toInt())

        // if the event has already passed for the day, schedule for the next day
        if (calendar.time.time < TimeHelper.currentTime.time) {
            calendar.add(Calendar.HOUR_OF_DAY, 24)
        }
        return calendar.timeInMillis
    }

    fun getAlarmTime(alarm: Alarm): Long {
        val calendar = GregorianCalendar()
        calendar.time = TimeHelper.currentTime

        // reset the calendar time to the start of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // add the milliseconds from the new alarm
        calendar.add(Calendar.MILLISECOND, alarm.time.toInt())

        val eventPassed = calendar.time.time < TimeHelper.currentTime.time

        val postponeDays = when {
            alarm.repeat && alarm.days.isNotEmpty() -> {
                val today = calendar.get(Calendar.DAY_OF_WEEK) - 1
                val eventDay = when {
                    alarm.days.last() >= today -> {
                        //Get the next alarm
                        val day = alarm.days.first { it >= today }
                        when {
                            //If the alarm is not set up for today or is setup for today and it hasn't ringed yet, do nothing
                            day > today || (day == today && !eventPassed) -> day
                            //If there was an alarm today but it already ringed and there is more in the weekend, skip to the next one.
                            day == today && eventPassed && alarm.days.last() > today -> alarm.days.first{ it > today }
                            else -> {
                                alarm.days.first()
                            }
                        }
                    }
                    //If there is no more alarms this week, skip to the next
                    else -> {
                        alarm.days.first()
                    }
                }
                var dayDiff = eventDay - today
                //If an alarm is set on repeat but only set up for one day, check if has already played and add reset the days accordingly
                if (dayDiff < 0 || (eventPassed && dayDiff == 0)) dayDiff += 7
                dayDiff
            }

            eventPassed -> 1

            else -> 0
        }
        calendar.add(Calendar.DATE, postponeDays)
        return calendar.timeInMillis
    }

    fun snooze(context: Context, oldAlarm: Alarm) {
        val snoozeMinutes = oldAlarm.snoozeMinutes
        val calendar = GregorianCalendar()
        val nowEpoch = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayEpoch = calendar.timeInMillis
        val snoozeTime = nowEpoch - todayEpoch + 1000 * 60 * snoozeMinutes
        enqueue(context, oldAlarm.copy(time = snoozeTime))
    }

    /**
     * @return the days of the week mapped to an index 0-Sunday, 1-Monday, ..., 6-Saturday.
     * The list order will match the user preferred days of the week order.
     */
    fun getDaysOfWeekByLocale(): List<Pair<String, Int>> {
        val firstDayIndex = GregorianCalendar().firstDayOfWeek - 1
        val daysWithIndex = availableDays.mapIndexed { index, s -> s to index }
        return daysWithIndex.subList(firstDayIndex, 7) + daysWithIndex.subList(0, firstDayIndex)
    }
}
