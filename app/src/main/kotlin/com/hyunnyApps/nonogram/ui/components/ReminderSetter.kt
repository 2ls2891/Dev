package com.hyunnyApps.nonogram.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hyunnyApps.nonogram.data.PlayerRepository
import com.hyunnyApps.nonogram.reminder.DailyReminder
import kotlinx.coroutines.launch

/**
 * 매일 알림 켜기/끄기. 켤 때 Android 13+ 알림 권한이 없으면 먼저 요청하고, 거부되면 onDenied.
 * @return (시각 or null) → 적용 함수
 */
@Composable
fun rememberReminderSetter(player: PlayerRepository, onDenied: () -> Unit): (Int?) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingHour by remember { mutableStateOf<Int?>(null) }
    val apply: (Int?) -> Unit = { hour ->
        scope.launch {
            player.setReminderHour(hour)
            DailyReminder.schedule(context, hour)
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val hour = pendingHour
        pendingHour = null
        if (granted && hour != null) apply(hour) else onDenied()
    }
    return { hour ->
        if (hour == null || Build.VERSION.SDK_INT < 33 || DailyReminder.canPost(context)) {
            apply(hour)
        } else {
            pendingHour = hour
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
