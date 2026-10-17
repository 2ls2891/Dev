package com.hyunnyApps.nonogram.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.hyunnyApps.nonogram.core.format.Localized

/** 앱 기본 언어는 한국어. 기기(또는 앱) 언어가 영어일 때만 en */
@Composable
fun Localized.text(): String {
    val locales = LocalConfiguration.current.locales
    return if (!locales.isEmpty && locales[0].language == "en") en else ko
}
