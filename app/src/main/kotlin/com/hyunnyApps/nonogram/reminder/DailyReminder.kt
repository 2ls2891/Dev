package com.hyunnyApps.nonogram.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hyunnyApps.nonogram.MainActivity
import com.hyunnyApps.nonogram.NonogramApplication
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.core.daily.DailyCalendar
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 매일 정해진 시각에 "오늘의 퍼즐" 알림. 오늘 데일리를 이미 풀었으면 보내지 않는다.
 * WorkManager 주기 작업(24시간)이라 시각은 ±15분 정도 오차가 있을 수 있다.
 */
object DailyReminder {
    const val CHANNEL_ID = "daily_puzzle"
    const val EXTRA_OPEN_DAILY = "open_daily"
    private const val WORK_NAME = "daily_reminder"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** hour 시 정각 기준으로 매일 실행. null 이면 해제 */
    fun schedule(context: Context, hour: Int?, now: LocalDateTime = LocalDateTime.now()) {
        val wm = WorkManager.getInstance(context)
        if (hour == null) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        var next = now.toLocalDate().atTime(LocalTime.of(hour, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun post(context: Context, streak: Int) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_DAILY, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = if (streak > 0) context.getString(R.string.reminder_text_streak, streak) else context.getString(R.string.reminder_text)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // canPost 로 확인함
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as NonogramApplication).container
        val daily = container.content.daily()
        val today = LocalDate.now()
        val todayIndex = DailyCalendar.dayIndex(LocalDate.parse(daily.epoch), today)
        val puzzle = daily.puzzles[DailyCalendar.puzzlePosition(todayIndex, daily.puzzles.size)]
        if (container.progress.isCompleted(puzzle.id)) return Result.success()
        val streak = container.player.streak.first().displayed(todayIndex)
        DailyReminder.post(applicationContext, streak)
        return Result.success()
    }
}
