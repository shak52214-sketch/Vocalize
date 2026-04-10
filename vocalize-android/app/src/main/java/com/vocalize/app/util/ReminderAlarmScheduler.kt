package com.vocalize.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vocalize.app.data.local.entity.MemoEntity
import com.vocalize.app.data.local.entity.RepeatType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleReminder(memo: MemoEntity) {
        val reminderTime = memo.reminderTime ?: return
        if (reminderTime <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = Constants.ACTION_PLAY
            putExtra(Constants.EXTRA_MEMO_ID, memo.id)
            putExtra(Constants.EXTRA_MEMO_TITLE, memo.title)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            memo.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pending)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pending)
        }
    }

    fun cancelReminder(memoId: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = Constants.ACTION_PLAY
            putExtra(Constants.EXTRA_MEMO_ID, memoId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            memoId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun scheduleNextRepeat(memo: MemoEntity) {
        val reminderTime = memo.reminderTime ?: return
        val now = System.currentTimeMillis()

        val nextTime: Long? = when (memo.repeatType) {
            RepeatType.DAILY -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = reminderTime
                    while (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            RepeatType.WEEKLY -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = reminderTime
                    while (timeInMillis <= now) add(Calendar.WEEK_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            RepeatType.CUSTOM_DAYS -> {
                val days = memo.customDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (days.isEmpty()) null else {
                    val cal = Calendar.getInstance()
                    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                    val nextDay = days.firstOrNull { it > currentDay } ?: days.first()
                    val daysUntil = if (nextDay > currentDay) nextDay - currentDay
                    else 7 - currentDay + nextDay
                    cal.apply {
                        timeInMillis = reminderTime
                        add(Calendar.DAY_OF_YEAR, daysUntil)
                    }.timeInMillis
                }
            }
            RepeatType.NONE -> null
        }

        nextTime?.let {
            scheduleReminder(memo.copy(reminderTime = it))
        }
    }
}
