package nonogram.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import nonogram.core.format.Localized

/** 기기 언어가 한국어면 ko, 아니면 en */
@Composable
fun Localized.text(): String {
    val locales = LocalConfiguration.current.locales
    return if (!locales.isEmpty && locales[0].language == "ko") ko else en
}
