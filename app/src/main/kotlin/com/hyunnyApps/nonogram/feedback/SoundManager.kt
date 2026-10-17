package com.hyunnyApps.nonogram.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.hyunnyApps.nonogram.R

enum class Sfx(val res: Int, val volume: Float) {
    PENCIL(R.raw.sfx_pencil, 0.35f),
    CROSS(R.raw.sfx_cross, 0.35f),
    LINE(R.raw.sfx_line, 0.6f),
    COMPLETE(R.raw.sfx_complete, 0.8f),
    STAMP(R.raw.sfx_stamp, 0.9f),
}

/**
 * 짧은 효과음 재생 (SoundPool). 게임 효과음 용도라 다른 앱 음악과 섞여 재생된다(오디오 포커스 요청 안 함).
 * 드래그로 여러 칸을 빠르게 칠할 때 소리가 겹쳐 시끄럽지 않도록 같은 소리는 45ms 간격으로 제한한다.
 */
class SoundManager(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val ids = Sfx.entries.associateWith { pool.load(context, it.res, 1) }
    private val lastPlayed = LongArray(Sfx.entries.size)

    @Volatile
    var enabled: Boolean = true

    fun play(sfx: Sfx) {
        if (!enabled) return
        val now = SystemClock.uptimeMillis()
        if (now - lastPlayed[sfx.ordinal] < 45) return
        lastPlayed[sfx.ordinal] = now
        val id = ids[sfx] ?: return
        pool.play(id, sfx.volume, sfx.volume, 1, 0, 1f)
    }
}
