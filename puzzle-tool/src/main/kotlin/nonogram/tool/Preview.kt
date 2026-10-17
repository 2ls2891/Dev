package nonogram.tool

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** 팩별 미리보기 PNG (문제 그림 | 클리어 후 컬러 그림). 콘텐츠 검수용 */
fun writePreviews(packsDir: File, outDir: File, cell: Int = 5, columns: Int = 8) {
    outDir.mkdirs()
    val index = json.decodeFromString<IndexOut>(File(packsDir, "index.json").readText())
    val sets = index.packs.map { it.id to json.decodeFromString<PackOut>(File(packsDir, it.file).readText()).puzzles } +
        ("daily" to json.decodeFromString<DailyOut>(File(packsDir, index.daily.file).readText()).puzzles.take(56))
    for ((name, puzzles) in sets) {
        val slot = 30 * cell + 10
        val tileW = slot * 2
        val tileH = slot + 16
        val rows = (puzzles.size + columns - 1) / columns
        val img = BufferedImage(columns * tileW, rows * tileH, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color.WHITE
        g.fillRect(0, 0, img.width, img.height)
        g.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        puzzles.forEachIndexed { i, p ->
            val ox = (i % columns) * tileW
            val oy = (i / columns) * tileH
            fun draw(pic: Picture, x0: Int) {
                pic.rows.forEachIndexed { y, row ->
                    row.forEachIndexed { x, ch ->
                        if (ch != '.') {
                            g.color = Color.decode(pic.palette[ch.digitToInt(36) - 1])
                            g.fillRect(x0 + x * cell, oy + y * cell, cell, cell)
                        }
                    }
                }
            }
            draw(Picture(p.palette, p.rows), ox)
            draw(p.reveal, ox + slot)
            g.color = Color.DARK_GRAY
            g.drawString("L${p.difficulty.level} ${p.w}x${p.h} ${p.title.ko}", ox, oy + slot + 10)
        }
        g.dispose()
        ImageIO.write(img, "png", File(outDir, "$name.png"))
    }
}
