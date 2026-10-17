package com.hyunnyApps.nonogram.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.hyunnyApps.nonogram.BuildConfig
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ads.AdConfig
import com.hyunnyApps.nonogram.ads.ConsentManager
import com.hyunnyApps.nonogram.data.PlayerRepository
import com.hyunnyApps.nonogram.ui.components.RuleExample
import com.hyunnyApps.nonogram.ui.theme.InkOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(consent: ConsentManager, player: PlayerRepository, onBack: () -> Unit, onOpenRules: () -> Unit, onOpenLicenses: () -> Unit) {
    val activity = LocalActivity.current
    val privacyRequired by consent.privacyOptionsRequired.collectAsStateWithLifecycle()
    val ink by player.inkIndex.collectAsStateWithLifecycle(initialValue = 0)
    val scope = rememberCoroutineScope()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BackTopBar(stringResource(R.string.settings_title), onBack) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            InkPicker(ink) { scope.launch { player.setInk(it) } }
            HorizontalDivider()
            // 동의가 필요한 지역에서는 사용자가 언제든 광고 동의를 바꿀 수 있어야 한다 (UMP 요구사항)
            if (privacyRequired && activity != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_privacy)) },
                    modifier = Modifier.clickable { consent.showPrivacyOptionsForm(activity) },
                )
                HorizontalDivider()
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rules)) },
                modifier = Modifier.clickable(onClick = onOpenRules),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_licenses)) },
                modifier = Modifier.clickable(onClick = onOpenLicenses),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)) },
                supportingContent = if (AdConfig.IS_TEST_ADS) {
                    { Text(stringResource(R.string.settings_test_ads)) }
                } else null,
            )
        }
    }
}

private val InkNames = listOf(R.string.ink_0, R.string.ink_1, R.string.ink_2, R.string.ink_3, R.string.ink_4, R.string.ink_5)

/** 기본 퍼즐 칠하기 색 선택 + 미리보기 */
@Composable
private fun InkPicker(selected: Int, onSelect: (Int) -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.settings_ink), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.settings_ink_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InkOptions.forEachIndexed { i, option ->
                val color = if (dark) option.dark else option.light
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .border(if (i == selected) 3.dp else 1.dp, if (i == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { onSelect(i) }
                            .padding(5.dp).clip(CircleShape).background(color),
                    )
                    Text(stringResource(InkNames[i]), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        RuleExample(listOf(2, 1), "##.#.", cell = 26.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val intro = stringResource(R.string.licenses_intro)
    val text by produceState("", intro) {
        value = withContext(Dispatchers.IO) {
            val assets = context.assets
            val files = assets.list("licenses").orEmpty().filter { it.endsWith(".txt") }.sorted()
            buildString {
                append(intro).append("\n\n")
                append(LIBRARIES).append("\n")
                for (f in files) {
                    append("──────── ").append(f.removeSuffix(".txt")).append(" ────────\n")
                    append(assets.open("licenses/$f").bufferedReader().use { it.readText() }).append("\n\n")
                }
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BackTopBar(stringResource(R.string.settings_licenses), onBack) },
    ) { padding ->
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        )
    }
}

private const val LIBRARIES = """
Puzzle artwork: Microsoft Fluent Emoji — MIT License
Puzzle names (Korean): Unicode CLDR — Unicode License v3
AndroidX, Jetpack Compose — Apache License 2.0
Kotlin, kotlinx.coroutines, kotlinx.serialization — Apache License 2.0
Google Mobile Ads SDK, User Messaging Platform — Google APIs Terms of Service
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        title = { Text(title) },
    )
}
