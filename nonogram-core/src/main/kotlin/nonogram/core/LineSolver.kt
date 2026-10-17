package nonogram.core

/**
 * 한 줄에 대한 완전한 추론기.
 *
 * 입력 줄(칸별 가능한 값 마스크)과 단서를 만족하는 모든 배치를 고려해서,
 * 각 칸에 대해 "어떤 배치에서든 나올 수 있는 값"의 합집합으로 마스크를 좁힌다.
 * 흑백/컬러 노노그램을 모두 지원한다(같은 색 블록 사이에만 빈칸이 필수).
 *
 * 상태 (pos, b): 0..pos-1 칸이 정해졌고 다음에 놓을 블록이 b이며, pos 칸은 자유로운 상태.
 *  - S(pos, b): 그 상태에서 끝까지 유효하게 배치할 수 있는가 (뒤에서부터 계산)
 *  - R(pos, b): (0, 0)에서 유효한 경로로 도달 가능한가 (앞에서부터 계산)
 * 두 조건을 모두 만족하는 전이만 결과에 반영하므로 결과는 정확한 합집합이다.
 * 시간 복잡도 O(n * k * maxLen), n <= 50 수준에서는 무시할 만하다.
 */
object LineSolver {

    /** 좁혀진 마스크를 반환한다. 어떤 배치도 불가능하면(모순) null. 입력 배열은 변경하지 않는다. */
    fun solve(masks: IntArray, clue: Clue): IntArray? {
        val n = masks.size
        val k = clue.size
        val stride = k + 1

        // run[b][pos]: pos부터 블록 b의 색을 허용하는 칸이 연속으로 몇 개인가
        val runByColor = HashMap<Int, IntArray>()
        val run = Array(k) { b ->
            runByColor.getOrPut(clue[b].color) {
                val color = clue[b].color
                IntArray(n + 1).also { r ->
                    for (pos in n - 1 downTo 0) r[pos] = if (CellMask.allows(masks[pos], color)) r[pos + 1] + 1 else 0
                }
            }
        }
        fun allowEmpty(pos: Int) = masks[pos] and CellMask.EMPTY != 0
        fun gapAfter(b: Int) = b + 1 < k && clue[b + 1].color == clue[b].color

        val s = BooleanArray((n + 2) * stride)
        for (pos in n downTo 0) {
            for (b in k downTo 0) {
                s[pos * stride + b] = if (b == k) {
                    pos == n || (allowEmpty(pos) && s[(pos + 1) * stride + k])
                } else {
                    var ok = pos < n && allowEmpty(pos) && s[(pos + 1) * stride + b]
                    if (!ok) {
                        val end = pos + clue[b].length
                        if (end <= n && run[b][pos] >= clue[b].length) {
                            ok = if (gapAfter(b)) end < n && allowEmpty(end) && s[(end + 1) * stride + b + 1]
                                 else s[end * stride + b + 1]
                        }
                    }
                    ok
                }
            }
        }
        if (!s[0]) return null

        val result = IntArray(n)
        val reach = BooleanArray((n + 2) * stride)
        reach[0] = true
        for (pos in 0 until n) {
            for (b in 0..k) {
                val idx = pos * stride + b
                if (!reach[idx] || !s[idx]) continue
                if (allowEmpty(pos) && s[(pos + 1) * stride + b]) {
                    result[pos] = result[pos] or CellMask.EMPTY
                    reach[(pos + 1) * stride + b] = true
                }
                if (b == k) continue
                val block = clue[b]
                val end = pos + block.length
                if (end > n || run[b][pos] < block.length) continue
                val bit = CellMask.of(block.color)
                if (gapAfter(b)) {
                    if (end < n && allowEmpty(end) && s[(end + 1) * stride + b + 1]) {
                        for (i in pos until end) result[i] = result[i] or bit
                        result[end] = result[end] or CellMask.EMPTY
                        reach[(end + 1) * stride + b + 1] = true
                    }
                } else if (s[end * stride + b + 1]) {
                    for (i in pos until end) result[i] = result[i] or bit
                    reach[end * stride + b + 1] = true
                }
            }
        }
        return result
    }
}
