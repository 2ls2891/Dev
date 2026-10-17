package nonogram.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nonogram.core.daily.Streak

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
    }
}
