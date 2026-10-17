package com.hyunnyApps.nonogram.core.daily

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 데일리 퍼즐 날짜 계산. 기기 현지 "날짜" 기준이며 초/86400 나눗셈을 쓰지 않는다(서머타임 안전).
 * iOS도 같은 규칙으로 구현하고 testvectors/daily_index.json 으로 검증한다.
 */
object DailyCalendar {
    fun dayIndex(epoch: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(epoch, today)

    /** 일정보다 이전이면 0, 일정이 바닥나면 처음부터 반복 */
    fun puzzlePosition(dayIndex: Long, scheduleSize: Int): Int {
        require(scheduleSize > 0) { "empty daily schedule" }
        if (dayIndex <= 0) return 0
        return (dayIndex % scheduleSize).toInt()
    }
}

/**
 * 연속 클리어 기록. lastDay = 마지막으로 데일리를 클리어한 dayIndex.
 * 기기 시간을 바꿔 조작하는 경우는 오프라인 게임이므로 막지 않는다.
 */
data class Streak(val current: Int = 0, val best: Int = 0, val lastDay: Long? = null) {
    fun onDailyCleared(day: Long): Streak {
        if (lastDay == day) return this
        val next = if (lastDay != null && day == lastDay + 1) current + 1 else 1
        return Streak(next, maxOf(best, next), day)
    }

    /** 오늘 기준으로 화면에 보여줄 연속 기록 (어제도 안 했으면 이미 끊긴 것) */
    fun displayed(today: Long): Int = if (lastDay != null && today - lastDay <= 1) current else 0

    fun isClearedOn(day: Long) = lastDay == day
}
