package com.hyunnyApps.nonogram.core

enum class Outcome {
    /** 추론만으로 모든 칸이 확정됨 → 해가 유일함이 보장된다 */
    SOLVED,
    /** 추론이 막힘 (해가 여러 개이거나 더 강한 기법이 필요) */
    STUCK,
    /** 단서끼리 모순 (해가 없음) */
    CONTRADICTION,
}

class SolveResult(
    val outcome: Outcome,
    /** 최종 칸 마스크 (row-major) */
    val masks: IntArray,
    /** 줄 추론만으로 진행한 라운드 수 (행 전체 + 열 전체 = 1라운드) */
    val lineRounds: Int,
    /** 가정-모순(probing)으로 제거한 값의 수. 0이면 줄 추론만으로 풀림 */
    val probeEliminations: Int,
    /** 처음 줄 추론이 막혔을 때 확정된 칸 비율 (probing이 필요 없으면 1.0) */
    val lineOnlyProgress: Double,
)

/**
 * 판 전체 추론기.
 *
 * 1) 줄 추론(LineSolver)을 행/열에 번갈아 적용해 더 바뀌는 칸이 없을 때까지 반복한다.
 * 2) 막히면 probing: 한 칸에 값 하나를 가정하고 줄 추론을 돌려 모순이 나면 그 값을 제거한다.
 *
 * 두 기법 모두 "강제된 추론"만 하므로 SOLVED면 해가 유일하다.
 * probing이 필요한 퍼즐은 사람이 풀기에도 어렵다 → 난이도 산정에 사용한다.
 */
class BoardSolver(val rowClues: List<Clue>, val colClues: List<Clue>) {
    val width = colClues.size
    val height = rowClues.size

    constructor(grid: Grid) : this(grid.rowClues(), grid.colClues())

    init {
        require(width >= 1 && height >= 1) { "board must not be empty" }
    }

    fun initialMasks(): IntArray {
        val rowColors = rowClues.map { clue -> clue.fold(0) { acc, b -> acc or CellMask.of(b.color) } }
        val colColors = colClues.map { clue -> clue.fold(0) { acc, b -> acc or CellMask.of(b.color) } }
        return IntArray(width * height) { i ->
            CellMask.EMPTY or (rowColors[i / width] and colColors[i % width])
        }
    }

    /**
     * masks를 직접 갱신하며 줄 추론을 전파한다. 모순이면 false.
     * @return 모순 여부와 함께 진행 라운드 수를 rounds[0]에 더한다(null이면 무시).
     */
    fun propagate(masks: IntArray, rowsDirty: BooleanArray, colsDirty: BooleanArray, rounds: IntArray? = null): Boolean {
        val rowBuf = IntArray(width)
        val colBuf = IntArray(height)
        var pending = rowsDirty.any { it } || colsDirty.any { it }
        while (pending) {
            pending = false
            rounds?.let { it[0]++ }
            for (r in 0 until height) {
                if (!rowsDirty[r]) continue
                rowsDirty[r] = false
                for (c in 0 until width) rowBuf[c] = masks[r * width + c]
                val solved = LineSolver.solve(rowBuf, rowClues[r]) ?: return false
                for (c in 0 until width) {
                    if (solved[c] != rowBuf[c]) {
                        masks[r * width + c] = solved[c]
                        colsDirty[c] = true
                        pending = true
                    }
                }
            }
            for (c in 0 until width) {
                if (!colsDirty[c]) continue
                colsDirty[c] = false
                for (r in 0 until height) colBuf[r] = masks[r * width + c]
                val solved = LineSolver.solve(colBuf, colClues[c]) ?: return false
                for (r in 0 until height) {
                    if (solved[r] != colBuf[r]) {
                        masks[r * width + c] = solved[r]
                        rowsDirty[r] = true
                        pending = true
                    }
                }
            }
        }
        return true
    }

    /**
     * @param useProbing false면 줄 추론만 사용한다.
     * @param probeBudget probing 시도(가정 1회 = 1) 상한. 초과하면 STUCK으로 본다(툴 실행 시간 보호용).
     */
    fun solve(useProbing: Boolean = true, probeBudget: Int = 200_000): SolveResult {
        val masks = initialMasks()
        val rounds = intArrayOf(0)
        if (!propagate(masks, BooleanArray(height) { true }, BooleanArray(width) { true }, rounds)) {
            return SolveResult(Outcome.CONTRADICTION, masks, rounds[0], 0, 0.0)
        }
        val lineRounds = rounds[0]
        val lineOnlyProgress = solvedRatio(masks)
        if (lineOnlyProgress == 1.0) return SolveResult(Outcome.SOLVED, masks, lineRounds, 0, 1.0)
        if (!useProbing) return SolveResult(Outcome.STUCK, masks, lineRounds, 0, lineOnlyProgress)

        var eliminations = 0
        var budget = probeBudget
        val total = width * height
        var cell = 0
        var sinceProgress = 0 // 마지막 진전 이후 검사한 칸 수. 한 바퀴 돌도록 진전이 없으면 종료
        while (sinceProgress < total) {
            val mask = masks[cell]
            var eliminated = false
            if (!CellMask.isSolved(mask)) {
                var bits = mask
                while (bits != 0) {
                    val bit = bits and -bits
                    bits = bits xor bit
                    if (--budget < 0) return SolveResult(Outcome.STUCK, masks, lineRounds, eliminations, lineOnlyProgress)
                    val trial = masks.copyOf()
                    trial[cell] = bit
                    val r = cell / width
                    val c = cell % width
                    val ok = propagate(trial, BooleanArray(height).also { it[r] = true }, BooleanArray(width).also { it[c] = true })
                    if (!ok) {
                        masks[cell] = masks[cell] and bit.inv()
                        eliminations++
                        eliminated = true
                        if (masks[cell] == 0) return SolveResult(Outcome.CONTRADICTION, masks, lineRounds, eliminations, lineOnlyProgress)
                    }
                }
            }
            if (eliminated) {
                val r = cell / width
                val c = cell % width
                if (!propagate(masks, BooleanArray(height).also { it[r] = true }, BooleanArray(width).also { it[c] = true })) {
                    return SolveResult(Outcome.CONTRADICTION, masks, lineRounds, eliminations, lineOnlyProgress)
                }
                if (solvedRatio(masks) == 1.0) return SolveResult(Outcome.SOLVED, masks, lineRounds, eliminations, lineOnlyProgress)
                sinceProgress = 0
            } else {
                sinceProgress++
            }
            cell = (cell + 1) % total
        }
        return SolveResult(Outcome.STUCK, masks, lineRounds, eliminations, lineOnlyProgress)
    }

    private fun solvedRatio(masks: IntArray): Double = masks.count { CellMask.isSolved(it) }.toDouble() / masks.size
}
