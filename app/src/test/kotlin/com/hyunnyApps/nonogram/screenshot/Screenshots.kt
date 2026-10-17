package com.hyunnyApps.nonogram.screenshot

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hyunnyApps.nonogram.NonogramApplication
import com.hyunnyApps.nonogram.core.Grid
import com.hyunnyApps.nonogram.core.daily.DailyCalendar
import com.hyunnyApps.nonogram.core.game.Mark
import com.hyunnyApps.nonogram.core.game.MarkCodec
import com.hyunnyApps.nonogram.data.DAILY
import com.hyunnyApps.nonogram.data.PuzzleProgress
import com.hyunnyApps.nonogram.ui.components.PaperBackground
import com.hyunnyApps.nonogram.ui.components.RulesContent
import com.hyunnyApps.nonogram.ui.gallery.GalleryScreen
import com.hyunnyApps.nonogram.ui.gallery.GalleryViewModel
import com.hyunnyApps.nonogram.ui.game.GameScreen
import com.hyunnyApps.nonogram.ui.game.GameViewModel
import com.hyunnyApps.nonogram.ui.home.HomeScreen
import com.hyunnyApps.nonogram.ui.home.HomeViewModel
import com.hyunnyApps.nonogram.ui.home.KIND_COLOR
import com.hyunnyApps.nonogram.ui.pack.PackScreen
import com.hyunnyApps.nonogram.ui.pack.PackViewModel
import com.hyunnyApps.nonogram.ui.settings.SettingsScreen
import com.hyunnyApps.nonogram.ui.theme.LocalNoteColors
import com.hyunnyApps.nonogram.ui.theme.NonogramTheme
import com.hyunnyApps.nonogram.ui.tutorial.TutorialScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * 실제 콘텐츠·ViewModel로 화면을 렌더링해 docs/screenshots 에 저장한다 (디자인 검토용).
 * 시나리오마다 클래스를 나눈다 → forkEvery=1 로 JVM 이 분리되어 Robolectric 테스트끼리 간섭하지 않는다.
 * ./gradlew :app:testDebugUnitTest
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xhdpi")
abstract class ScreenshotBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    protected val container get() = ApplicationProvider.getApplicationContext<NonogramApplication>().container
    protected val out = System.getProperty("roborazzi.output.dir") ?: "build/screenshots"

    /** 실제 앱처럼 모눈종이 배경 + 하단 광고 배너 자리 */
    protected fun show(dark: Boolean = false, ink: Int = 0, content: @Composable () -> Unit) {
        rule.setContent {
            NonogramTheme(dark = dark, inkIndex = ink) {
                CompositionLocalProvider(LocalContentColor provides LocalNoteColors.current.ink) {
                    PaperBackground(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) { content() }
                            HorizontalDivider(color = LocalNoteColors.current.ink.copy(alpha = 0.15f))
                            Box(Modifier.fillMaxWidth().height(90.dp).background(Color(0x22888888)), contentAlignment = Alignment.Center) {
                                Text("광고 배너 자리 (대형 적응형)", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    protected fun capture(name: String) {
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/$name.png")
    }

    protected fun home(dark: Boolean = false, setup: (HomeViewModel) -> Unit = {}): HomeViewModel {
        val vm = HomeViewModel(container)
        setup(vm)
        show(dark) { HomeScreen(container.rewarded, container.player, { _, _ -> }, {}, {}, {}, {}, vm) }
        rule.waitUntil(60_000) { !vm.state.value.loading }
        return vm
    }

    protected fun game(id: String, source: String = "mono-nature-1", ink: Int = 0): GameViewModel {
        val vm = GameViewModel(SavedStateHandle(mapOf(GameViewModel.ARG_SOURCE to source, GameViewModel.ARG_PUZZLE_ID to id)), container)
        show(ink = ink) { GameScreen(container.rewarded, container.sound, true, {}, { _, _ -> }, vm) }
        // IO 에서 돌아오는 Main 재개가 테스트 대기 루프에서 가끔 처리되지 않아 메인 루퍼를 직접 비운다
        rule.waitUntil(60_000) { shadowOf(Looper.getMainLooper()).idle(); !vm.state.value.loading }
        return vm
    }

    /** 정답대로 칠해서 완성시킨다 */
    protected fun solve(vm: GameViewModel) {
        val grid = Grid.fromRows(vm.state.value.puzzle!!.rows)
        for (i in grid.cells.indices) if (grid.cells[i] != 0) {
            vm.selectTool(grid.cells[i]); vm.onStrokeStart(i); vm.onStrokeEnd()
        }
    }

    /** 위쪽 rows 줄을 정답대로 칠하고 나머지 칸은 ✕ */
    protected fun fillRows(vm: GameViewModel, rows: Int) {
        val grid = Grid.fromRows(vm.state.value.puzzle!!.rows)
        for (i in 0 until grid.width * rows) {
            vm.selectTool(if (grid.cells[i] != 0) 1 else Mark.CROSS)
            vm.onStrokeStart(i); vm.onStrokeEnd()
        }
        vm.selectTool(1)
    }

    /** 컬렉션 화면용: 퍼즐 몇 개를 완성한 상태로 저장 */
    protected fun markCompleted(source: String, count: Int) = runBlocking {
        val puzzles = if (source == DAILY) container.content.daily().puzzles else container.content.pack(source).puzzles
        puzzles.take(count).forEach { p ->
            container.progress.save(p.id, source, PuzzleProgress(p.w, p.h, "", completed = true))
        }
    }
}

class HomeFirstRunScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        home()
        capture("01_home_first_run")
    }
}

class HomeReturningScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        // 돌아온 사용자: 튜토리얼 봄, 퍼즐 몇 개 완성, 하나는 반쯤 칠함, 어제 하루 놓쳐 5일 기록이 끊김
        runBlocking {
            val p = container.player
            p.markTutorialSeen()
            val daily = container.content.daily()
            val todayIndex = DailyCalendar.dayIndex(LocalDate.parse(daily.epoch), container.today())
            // 5일 연속 → 어제 놓침
            for (d in (todayIndex - 6)..(todayIndex - 2)) p.onDailyCleared(d)
            val pack = container.content.pack("mono-food-1")
            val target = pack.puzzles[4]
            val grid = Grid.fromRows(target.rows)
            val marks = IntArray(grid.cells.size) { i -> if (i < grid.cells.size * 6 / 10) grid.cells[i] else 0 }
            container.progress.save(target.id, pack.id, PuzzleProgress(grid.width, grid.height, MarkCodec.encode(marks)))
            p.setLastPlayed(pack.id, target.id)
            container.player.streak.first()
        }
        markCompleted("mono-nature-1", 7)
        val vm = home()
        rule.waitUntil(60_000) { vm.state.value.continueItem != null }
        capture("02_home_returning")
    }
}

class HomeColorDarkScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        runBlocking { container.player.markTutorialSeen() }
        home(dark = true) { it.selectKind(KIND_COLOR) }
        capture("03_home_color_dark")
    }
}

class PackScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        markCompleted("mono-nature-1", 4)
        val vm = PackViewModel(SavedStateHandle(mapOf(PackViewModel.ARG_PACK_ID to "mono-nature-1")), container)
        show { PackScreen({}, { _, _ -> }, vm) }
        rule.waitUntil(60_000) { !vm.state.value.loading && vm.state.value.completed == 4 }
        capture("04_pack")
    }
}

class GameFirstTimeScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        game("fe-dog-m15")
        capture("05_game_first_time")
    }
}

class GamePlayingScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        val vm = game("fe-dog-m15")
        fillRows(vm, 6)
        val grid = Grid.fromRows(vm.state.value.puzzle!!.rows)
        vm.onStrokeStart(8 * grid.width + 3) // 드래그 중 (행·열 강조)
        capture("06_game_playing")
    }
}

class GameLineFlashScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        val vm = game("fe-dog-m15")
        rule.mainClock.autoAdvance = false
        fillRows(vm, 5)
        rule.mainClock.advanceTimeBy(250) // 형광펜이 지나가는 중
        rule.onRoot().captureRoboImage("$out/07_game_line_flash.png")
    }
}

class GameColorScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        val vm = game("fe-cherries-c20", source = "color-food-1")
        fillRows(vm, 4)
        capture("08_game_color")
    }
}

class RevealMidScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        val vm = game("fe-dog-m15")
        rule.mainClock.autoAdvance = false
        solve(vm)
        rule.mainClock.advanceTimeBy(1_100)
        rule.onRoot().captureRoboImage("$out/09_reveal_mid.png")
    }
}

class GameCompletedScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        val vm = game("fe-dog-m15")
        solve(vm)
        rule.waitUntil(10_000) { vm.state.value.completed }
        rule.mainClock.advanceTimeBy(4_000)
        capture("10_game_completed_stamp")
    }
}

class GameIndigoScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        val vm = game("fe-dog-m15", ink = 1)
        fillRows(vm, 9)
        capture("11_game_ink_indigo")
    }
}

class TutorialIntroScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        show { TutorialScreen(onMarkSeen = {}, onDone = {}) }
        capture("12_tutorial_intro")
    }
}

class TutorialRule2Screenshot : ScreenshotBase() {
    @Test
    fun run() {
        show { TutorialScreen(onMarkSeen = {}, onDone = {}, initialStep = 2) }
        capture("13_tutorial_rule2")
    }
}

class TutorialPracticeScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        show { TutorialScreen(onMarkSeen = {}, onDone = {}, initialStep = 4) }
        capture("14_tutorial_practice")
    }
}

class RulesScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        show { Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) { RulesContent(includeColor = true) } }
        capture("15_rules")
    }
}

class GalleryScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        markCompleted(DAILY, 3)
        markCompleted("mono-nature-1", 9)
        markCompleted("color-food-1", 6)
        val vm = GalleryViewModel(container)
        show { GalleryScreen({}, { _, _ -> }, vm) }
        rule.waitUntil(60_000) { !vm.state.value.loading && vm.state.value.collected > 0 }
        capture("16_gallery")
    }
}

class GalleryEmptyScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        val vm = GalleryViewModel(container)
        show { GalleryScreen({}, { _, _ -> }, vm) }
        rule.waitUntil(60_000) { !vm.state.value.loading }
        capture("17_gallery_empty")
    }
}

class SettingsScreenshot : ScreenshotBase() {
    @Test
    fun run() {
        runBlocking { container.player.setInk(1); container.player.setReminderHour(19) }
        show(ink = 1) { SettingsScreen(container.consent, container.player, {}, {}, {}) }
        capture("18_settings")
    }
}
