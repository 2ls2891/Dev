package nonogram.core

/**
 * 검증 전용: 가능한 모든 값 조합을 나열해서 줄 추론 결과를 구한다.
 * LineSolver와 독립적인 구현이므로 테스트 벡터 생성과 교차 검증에 쓴다. 지수 시간이므로 짧은 줄에만 사용.
 */
object LineBruteForce {
    fun solve(masks: IntArray, clue: Clue): IntArray? {
        val n = masks.size
        val values = IntArray(n)
        val result = IntArray(n)
        var found = false
        fun rec(i: Int) {
            if (i == n) {
                if (clueOf(values) == clue) {
                    found = true
                    for (j in 0 until n) result[j] = result[j] or CellMask.of(values[j])
                }
                return
            }
            var bits = masks[i]
            while (bits != 0) {
                val bit = bits and -bits
                bits = bits xor bit
                values[i] = Integer.numberOfTrailingZeros(bit)
                rec(i + 1)
            }
        }
        rec(0)
        return if (found) result else null
    }
}
