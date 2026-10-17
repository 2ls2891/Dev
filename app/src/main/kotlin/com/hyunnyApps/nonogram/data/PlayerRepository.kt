package com.hyunnyApps.nonogram.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.hyunnyApps.nonogram.core.daily.Streak

private val Context.playerStore by preferencesDataStore(name = "player")

/**
 * 플레이어 상태: 힌트 개수, 데일리 연속 기록.
 * 힌트는 하루에 [DAILY_FREE_HINTS]개까지 무료로 채워지고, 보상형 광고 1회 시청마다 [REWARDED_HINTS]개 추가.
 */
class PlayerRepository(context: Context) {
    private val store = context.applicationContext.playerStore

    val hints: Flow<Int> = store.data.map { it[HINTS] ?: DAILY_FREE_HINTS }

    val streak: Flow<Streak> = store.data.map {
        Streak(it[STREAK_CURRENT] ?: 0, it[STREAK_BEST] ?: 0, it[STREAK_LAST_DAY])
    }

    /** 튜토리얼을 한 번이라도 열었는지 (첫 실행 시 자동으로 한 번만 보여준다) */
    val tutorialSeen: Flow<Boolean> = store.data.map { it[TUTORIAL_SEEN] == true }

    /** 마지막으로 연 퍼즐 (홈의 "이어하기") */
    val lastPlayed: Flow<Pair<String, String>?> = store.data.map { p ->
        val source = p[LAST_SOURCE]
        val id = p[LAST_ID]
        if (source != null && id != null) source to id else null
    }

    suspend fun setLastPlayed(source: String, puzzleId: String) {
        store.edit { it[LAST_SOURCE] = source; it[LAST_ID] = puzzleId }
    }

    val soundOn: Flow<Boolean> = store.data.map { it[SOUND] ?: true }
    val hapticOn: Flow<Boolean> = store.data.map { it[HAPTIC] ?: true }
    suspend fun setSound(on: Boolean) { store.edit { it[SOUND] = on } }
    suspend fun setHaptic(on: Boolean) { store.edit { it[HAPTIC] = on } }

    /** 매일 알림 시각 (0~23시). null 이면 꺼짐 */
    val reminderHour: Flow<Int?> = store.data.map { p -> p[REMINDER_HOUR]?.takeIf { it >= 0 } }
    /** 알림 제안 카드를 이미 보여줬는지 */
    val reminderAsked: Flow<Boolean> = store.data.map { it[REMINDER_ASKED] == true }

    suspend fun setReminderHour(hour: Int?) {
        store.edit { it[REMINDER_HOUR] = hour ?: -1; it[REMINDER_ASKED] = true }
    }

    suspend fun markReminderAsked() { store.edit { it[REMINDER_ASKED] = true } }

    /** 끊긴 연속 기록을 지킨다 (어제 하루만 놓친 경우). 성공하면 true */
    suspend fun restoreStreak(todayIndex: Long): Boolean {
        var ok = false
        store.edit {
            val s = Streak(it[STREAK_CURRENT] ?: 0, it[STREAK_BEST] ?: 0, it[STREAK_LAST_DAY])
            if (s.canRestore(todayIndex)) {
                s.restored(todayIndex).lastDay?.let { day -> it[STREAK_LAST_DAY] = day }
                ok = true
            }
        }
        return ok
    }

    /** 기본(흑백) 퍼즐에서 칸을 칠하는 색 (ui.theme.InkOptions 의 순번) */
    val inkIndex: Flow<Int> = store.data.map { it[INK] ?: 0 }

    suspend fun setInk(index: Int) {
        store.edit { it[INK] = index }
    }

    suspend fun markTutorialSeen() {
        store.edit { it[TUTORIAL_SEEN] = true }
    }

    /** 날짜가 바뀌었으면 무료 힌트를 채운다 (이미 그 이상이면 유지) */
    suspend fun refillHints(todayEpochDay: Long) {
        store.edit {
            if (it[HINTS_REFILL_DAY] != todayEpochDay) {
                it[HINTS] = maxOf(it[HINTS] ?: DAILY_FREE_HINTS, DAILY_FREE_HINTS)
                it[HINTS_REFILL_DAY] = todayEpochDay
            }
        }
    }

    /** 힌트 1개 사용. 남은 게 없으면 false */
    suspend fun consumeHint(): Boolean {
        var ok = false
        store.edit {
            val n = it[HINTS] ?: DAILY_FREE_HINTS
            if (n > 0) {
                it[HINTS] = n - 1
                ok = true
            }
        }
        return ok
    }

    suspend fun addHints(count: Int) {
        store.edit { it[HINTS] = ((it[HINTS] ?: DAILY_FREE_HINTS) + count).coerceAtMost(MAX_HINTS) }
    }

    suspend fun onDailyCleared(dayIndex: Long) {
        store.edit {
            val next = Streak(it[STREAK_CURRENT] ?: 0, it[STREAK_BEST] ?: 0, it[STREAK_LAST_DAY]).onDailyCleared(dayIndex)
            it[STREAK_CURRENT] = next.current
            it[STREAK_BEST] = next.best
            next.lastDay?.let { day -> it[STREAK_LAST_DAY] = day }
        }
    }

    companion object {
        const val DAILY_FREE_HINTS = 3
        const val REWARDED_HINTS = 3
        const val MAX_HINTS = 99

        private val HINTS = intPreferencesKey("hints")
        private val HINTS_REFILL_DAY = longPreferencesKey("hints_refill_day")
        private val STREAK_CURRENT = intPreferencesKey("streak_current")
        private val STREAK_BEST = intPreferencesKey("streak_best")
        private val STREAK_LAST_DAY = longPreferencesKey("streak_last_day")
        private val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        private val INK = intPreferencesKey("ink")
        private val LAST_SOURCE = stringPreferencesKey("last_source")
        private val LAST_ID = stringPreferencesKey("last_id")
        private val SOUND = booleanPreferencesKey("sound")
        private val HAPTIC = booleanPreferencesKey("haptic")
        private val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val REMINDER_ASKED = booleanPreferencesKey("reminder_asked")
    }
}
