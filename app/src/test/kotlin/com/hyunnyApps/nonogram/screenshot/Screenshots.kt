package com.hyunnyApps.nonogram.screenshot

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hyunnyApps.nonogram.NonogramApplication
import com.hyunnyApps.nonogram.core.Grid
import com.hyunnyApps.nonogram.core.game.Mark
import com.hyunnyApps.nonogram.ui.components.RulesContent
import com.hyunnyApps.nonogram.ui.game.GameScreen
import com.hyunnyApps.nonogram.ui.game.GameViewModel
import com.hyunnyApps.nonogram.ui.home.HomeScreen
import com.hyunnyApps.nonogram.ui.home.HomeViewModel
import com.hyunnyApps.nonogram.ui.home.KIND_COLOR
import com.hyunnyApps.nonogram.ui.pack.PackScreen
import com.hyunnyApps.nonogram.ui.pack.PackViewModel
import com.hyunnyApps.nonogram.ui.theme.NonogramTheme
import com.hyunnyApps.nonogram.ui.tutorial.TutorialScreen
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 실제 콘텐츠·ViewModel로 화면을 렌더링해 docs/screenshots 에 저장한다 (디자인 검토용).
 * ./gradlew :app:testDebugUnitTest --tests '*ScreensScreenshotTest*'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xhdpi")
abstract class ScreenshotBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    protected val container get() = ApplicationProvider.getApplicationContext<NonogramApplication>().container
    private val out = System.getProperty("roborazzi.output.dir") ?: "build/screenshots"

    /** 실제 앱처럼 하단에 광고 배너 자리를 둔다 */
    protected fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent {
            NonogramTheme(dark = dark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) { content() }
                        HorizontalDivider()
                        Box(Modifier.fillMaxWidth().height(90.dp()).background(Color(0x22888888)), contentAlignment = Alignment.Center) {
                            Text("광고 배너 자리 (대형 적응형)", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    protected fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())

    protected fun capture(name: String) {
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/$name.png")
    }

    protected fun game(id: String, source: String = "mono-nature-1"): GameViewModel {
        val vm = GameViewModel(SavedStateHandle(mapOf(GameViewModel.ARG_SOURCE to source, GameViewModel.ARG_PUZZLE_ID to id)), container)
        show { GameScreen(container.rewarded, {}, { _, _ -> }, vm) }
        rule.waitUntil(60_000) { !vm.state.value.loading }
        return vm
    }
}

class HomeScreenshot : ScreenshotBase() {
    @Test
    fun home() {
        val vm = HomeViewModel(container)
        show { HomeScreen({ _, _ -> }, {}, {}, {}, vm) }
        rule.waitUntil(60_000) { !vm.state.value.loading }
        capture("01_home")
    }
}

class HomeColorDarkScreenshot : ScreenshotBase() {
    @Test
    fun homeColorDark() {
        runBlocking { container.player.markTutorialSeen() }
        val vm = HomeViewModel(container)
        vm.selectKind(KIND_COLOR)
        show(dark = true) { HomeScreen({ _, _ -> }, {}, {}, {}, vm) }
        rule.waitUntil(60_000) { !vm.state.value.loading }
        capture("02_home_color_dark")
    }
}

class PackScreenshot : ScreenshotBase() {
    @Test
    fun pack() {
        val vm = PackViewModel(SavedStateHandle(mapOf(PackViewModel.ARG_PACK_ID to "mono-nature-1")), container)
        show { PackScreen({}, { _, _ -> }, vm) }
        rule.waitUntil(60_000) { !vm.state.value.loading }
        capture("03_pack")
    }

}

class GameFirstTimeScreenshot : ScreenshotBase() {
    @Test
    fun gameFirstTime() {
        game("fe-dog-m15")
        capture("04_game_first_time")
    }
}

class GamePlayingScreenshot : ScreenshotBase() {
    @Test
    fun gamePlaying() {
        val vm = game("fe-dog-m15")
        val p = vm.state.value.puzzle!!
        val grid = Grid.fromRows(p.rows)
        // 위쪽 절반은 정답대로 칠하고, 몇 칸은 X 표시, 마지막은 드래그 중 상태
        for (i in 0 until grid.width * (grid.height / 2)) {
            vm.selectTool(if (grid.cells[i] != 0) 1 else Mark.CROSS)
            vm.onStrokeStart(i); vm.onStrokeEnd()
        }
        vm.selectTool(1)
        val r = grid.height / 2 + 1
        vm.onStrokeStart(r * grid.width + 3)
        capture("05_game_playing")
    }
}

class GameColorScreenshot : ScreenshotBase() {
    @Test
    fun gameColor() {
        game("fe-cherries-c20", source = "color-food-1")
        capture("06_game_color")
    }
}

class GameCompletedScreenshot : ScreenshotBase() {
    @Test
    fun gameCompleted() {
        val vm = game("fe-dog-m15")
        val grid = Grid.fromRows(vm.state.value.puzzle!!.rows)
        for (i in grid.cells.indices) if (grid.cells[i] != 0) { vm.onStrokeStart(i); vm.onStrokeEnd() }
        rule.waitUntil(5_000) { vm.state.value.completed }
        capture("07_game_completed")
    }
}

class TutorialIntroScreenshot : ScreenshotBase() {
    @Test
    fun tutorialIntro() {
        show { TutorialScreen(onMarkSeen = {}, onDone = {}) }
        capture("08_tutorial_intro")
    }
}

class TutorialRule2Screenshot : ScreenshotBase() {
    @Test
    fun tutorialRule2() {
        show { TutorialScreen(onMarkSeen = {}, onDone = {}, initialStep = 2) }
        capture("09_tutorial_rule2")
    }
}

class TutorialPracticeScreenshot : ScreenshotBase() {
    @Test
    fun tutorialPractice() {
        show { TutorialScreen(onMarkSeen = {}, onDone = {}, initialStep = 4) }
        capture("10_tutorial_practice")
    }
}

class RulesScreenshot : ScreenshotBase() {
    @Test
    fun rules() {
        show { Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp())) { RulesContent(includeColor = true) } }
        capture("11_rules")
    }
}
