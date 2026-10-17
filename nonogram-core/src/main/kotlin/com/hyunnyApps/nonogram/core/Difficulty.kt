package com.hyunnyApps.nonogram.core

enum class Technique {
    /** 줄 하나만 보고 추론해서 풀 수 있음 */
    LINE,
    /** 가정 후 모순을 찾는 추론(probing)이 필요함 */
    PROBING,
}

/**
 * @param level 1(입문) ~ 5(전문가). 사용자에게 보여주는 별 개수.
 * @param score 같은 레벨 안에서 정렬할 때 쓰는 세부 점수 (클수록 어려움).
 */
data class Difficulty(val level: Int, val score: Double, val technique: Technique)

/**
 * 난이도 규칙. 앱과 툴이 같은 값을 쓰도록 한 곳에서 정의한다.
 *
 * level = 크기 단계(10 이하=1, 15=2, 20=3, 25=4, 그 이상=5) + probing 필요 시 1 (최대 5)
 * score = 칸 수 × (1 + 줄 추론 라운드/10) × (1 + probing 제거 수/20)
 */
object DifficultyRater {
    fun rate(grid: Grid, result: SolveResult): Difficulty {
        require(result.outcome == Outcome.SOLVED) { "only solved puzzles can be rated" }
        val side = maxOf(grid.width, grid.height)
        val sizeLevel = when {
            side <= 10 -> 1
            side <= 15 -> 2
            side <= 20 -> 3
            side <= 25 -> 4
            else -> 5
        }
        val technique = if (result.probeEliminations > 0) Technique.PROBING else Technique.LINE
        val level = (sizeLevel + if (technique == Technique.PROBING) 1 else 0).coerceIn(1, 5)
        val score = grid.width * grid.height *
            (1 + result.lineRounds / 10.0) *
            (1 + result.probeEliminations / 20.0)
        return Difficulty(level, score, technique)
    }
}
