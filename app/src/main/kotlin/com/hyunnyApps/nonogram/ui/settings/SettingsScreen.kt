package com.hyunnyApps.nonogram.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyunnyApps.nonogram.BuildConfig
import com.hyunnyApps.nonogram.R
import com.hyunnyApps.nonogram.ads.AdConfig
import com.hyunnyApps.nonogram.ads.ConsentManager
import com.hyunnyApps.nonogram.data.PlayerRepository
import com.hyunnyApps.nonogram.ui.components.NoteTopBar
import com.hyunnyApps.nonogram.ui.components.PaperCard
import com.hyunnyApps.nonogram.ui.components.RuleExample
import com.hyunnyApps.nonogram.ui.components.rememberReminderSetter
import com.hyunnyApps.nonogram.ui.theme.HandFont
import com.hyunnyApps.nonogram.ui.theme.InkOptions
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val hand18 = TextStyle(fontFamily = HandFont, fontSize = 18.sp, lineHeight = 20.sp)
private val InkNames = listOf(R.string.ink_0, R.string.ink_1, R.string.ink_2, R.string.ink_3, R.string.ink_4, R.string.ink_5)
private val ReminderHours = listOf(8 to R.string.reminder_hour_8, 12 to R.string.reminder_hour_12, 19 to R.string.reminder_hour_19, 21 to R.string.reminder_hour_21)

@Composable
fun SettingsScreen(consent: ConsentManager, player: PlayerRepository, onBack: () -> Unit, onOpenRules: () -> Unit, onOpenLicenses: () -> Unit) {
    val activity = LocalActivity.current
    val note = LocalNoteColors.current
    val privacyRequired by consent.privacyOptionsRequired.collectAsStateWithLifecycle()
    val ink by player.inkIndex.collectAsStateWithLifecycle(initialValue = 0)
    val soundOn by player.soundOn.collectAsStateWithLifecycle(initialValue = true)
    val hapticOn by player.hapticOn.collectAsStateWithLifecycle(initialValue = true)
    val reminderHour by player.reminderHour.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val deniedText = stringResource(R.string.reminder_denied)
    val setReminder = rememberReminderSetter(player) { scope.launch { snackbar.showSnackbar(deniedText) } }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Column(Modifier.fillMaxSize()) {
            NoteTopBar(stringResource(R.string.settings_title), onBack)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // 칠하기 색
                PaperCard(rotation = -0.5f) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_ink), style = MaterialTheme.typography.titleLarge, color = note.ink)
                        Text(stringResource(R.string.settings_ink_desc), style = MaterialTheme.typography.bodySmall, color = note.inkSoft)
                        val dark = note.paper.red < 0.5f
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            InkOptions.forEachIndexed { i, option ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier.size(42.dp).clip(CircleShape)
                                            .border(if (i == ink) 3.dp else 1.5.dp, if (i == ink) note.redPencil else note.ink.copy(alpha = 0.4f), CircleShape)
                                            .clickable { scope.launch { player.setInk(i) } }
                                            .padding(6.dp).clip(CircleShape).background(if (dark) option.dark else option.light),
                                    )
                                    Text(stringResource(InkNames[i]), style = hand18, color = note.ink)
                                }
                            }
                        }
                        RuleExample(listOf(2, 1), "##.#.", cell = 26.dp)
                    }
                }

                // 소리 · 진동 · 알림
                PaperCard(rotation = 0.4f) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ToggleRow(stringResource(R.string.settings_sound), soundOn) { scope.launch { player.setSound(it) } }
                        HorizontalDivider(color = note.paperLineStrong)
                        ToggleRow(stringResource(R.string.settings_haptic), hapticOn) { scope.launch { player.setHaptic(it) } }
                        HorizontalDivider(color = note.paperLineStrong)
                        Text(stringResource(R.string.settings_reminder), style = MaterialTheme.typography.titleLarge, color = note.ink, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                            Choice(stringResource(R.string.settings_reminder_off), reminderHour == null) { setReminder(null) }
                            ReminderHours.forEach { (hour, label) ->
                                Choice(stringResource(label), reminderHour == hour) { setReminder(hour) }
                            }
                        }
                    }
                }

                // 기타
                PaperCard(rotation = -0.3f) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        LinkRow(stringResource(R.string.settings_rules), onOpenRules)
                        // 동의가 필요한 지역에서는 사용자가 언제든 광고 동의를 바꿀 수 있어야 한다 (UMP 요구사항)
                        if (privacyRequired && activity != null) {
                            HorizontalDivider(color = note.paperLineStrong)
                            LinkRow(stringResource(R.string.settings_privacy)) { consent.showPrivacyOptionsForm(activity) }
                        }
                        HorizontalDivider(color = note.paperLineStrong)
                        LinkRow(stringResource(R.string.settings_licenses), onOpenLicenses)
                        HorizontalDivider(color = note.paperLineStrong)
                        Column(Modifier.padding(vertical = 14.dp)) {
                            Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = note.inkSoft)
                            if (AdConfig.IS_TEST_ADS) Text(stringResource(R.string.settings_test_ads), style = MaterialTheme.typography.bodySmall, color = note.inkSoft)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val note = LocalNoteColors.current
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = note.ink, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = note.card, checkedTrackColor = note.redPencil, checkedBorderColor = note.ink,
                uncheckedThumbColor = note.inkSoft, uncheckedTrackColor = note.card, uncheckedBorderColor = note.inkSoft,
            ),
        )
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    val note = LocalNoteColors.current
    val shape = RoundedCornerShape(10.dp)
    Text(
        label,
        style = hand18,
        color = note.ink,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) note.highlighter else note.card)
            .border(if (selected) 2.dp else 1.dp, note.ink.copy(alpha = if (selected) 1f else 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    val note = LocalNoteColors.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = note.ink, modifier = Modifier.weight(1f))
        Text("›", style = MaterialTheme.typography.titleLarge, color = note.inkSoft)
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val note = LocalNoteColors.current
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
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        NoteTopBar(stringResource(R.string.settings_licenses), onBack)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = note.ink,
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        )
    }
}

private const val LIBRARIES = """
Puzzle artwork: Microsoft Fluent Emoji — MIT License
Puzzle names (Korean): Unicode CLDR — Unicode License v3
Handwriting font: Gaegu (JIKJI SOFT) — SIL Open Font License 1.1
AndroidX, Jetpack Compose, WorkManager — Apache License 2.0
Kotlin, kotlinx.coroutines, kotlinx.serialization — Apache License 2.0
Google Mobile Ads SDK, User Messaging Platform — Google APIs Terms of Service
"""

