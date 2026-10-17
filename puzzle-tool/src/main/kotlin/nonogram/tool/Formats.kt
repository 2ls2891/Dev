package nonogram.tool

import kotlinx.serialization.Serializable
import nonogram.core.format.Picture

/** content/scripts 의 래스터화 스크립트가 만든 후보 한 개 */
@Serializable
data class Candidate(
    val source: String,
    val key: String,
    val name: String,
    val name_ko: String? = null,
    val glyph: String = "",
    val unicode: String = "",
    val group: String = "",
    val kind: String,
    val size: Int,
    val w: Int,
    val h: Int,
    val palette: List<String>,
    val rows: List<String>,
    val fidelity: Double? = null,
    val fill: Double,
    val reveal: Picture,
)
