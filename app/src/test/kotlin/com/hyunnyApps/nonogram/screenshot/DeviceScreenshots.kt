package com.hyunnyApps.nonogram.screenshot

import org.junit.Test
import org.robolectric.annotation.Config

/*
 * 기기 크기별 화면 확인 (docs/screenshots/devices).
 * 기기 이름이 아니라 "창 크기(dp)"로 고른다. 실제 dp 는 모델·화면 크기 설정에 따라 달라지므로 범위의 양 끝을 본다.
 * 클래스 하나 = 시나리오 하나 (forkEvery=1 로 JVM 분리)
 */
private const val COVER344 = "ko-rKR-w344dp-h882dp-port-xxhdpi" // 폴드 커버 화면(좁은 쪽)
private const val COVER412 = "ko-rKR-w412dp-h915dp-port-xxhdpi" // 폴드 커버 화면(넓은 쪽)·큰 폰
private const val OPEN690 = "ko-rKR-w690dp-h829dp-port-xhdpi" // 폴드 펼침(구형, 세로에 가까움)
private const val OPEN984 = "ko-rKR-w984dp-h1092dp-port-xhdpi" // 폴드 펼침(Fold7급)
private const val OPEN829LAND = "ko-rKR-w829dp-h690dp-land-xhdpi" // 폴드 펼침 가로
private const val TABLETPORT = "ko-rKR-w800dp-h1280dp-port-hdpi" // 태블릿 세로
private const val TABLETLAND = "ko-rKR-w1280dp-h800dp-land-hdpi" // 태블릿 가로
private const val PHONELAND = "ko-rKR-w852dp-h393dp-land-xhdpi" // 폰 가로 창(분할·자유 창)
private const val SMALLFONT = "ko-rKR-w360dp-h640dp-port-xhdpi" // 작은 폰 + 글꼴 200%

@Config(qualifiers = COVER344)
class Cover344HomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/cover344_home")
}

@Config(qualifiers = COVER344)
class Cover344GameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/cover344_game")
}

@Config(qualifiers = COVER344)
class Cover344DoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/cover344_done")
}

@Config(qualifiers = COVER344)
class Cover344TutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/cover344_tutorial")
}

@Config(qualifiers = COVER344)
class Cover344PackShot : ScreenshotBase() {
    @Test
    fun run() = pack("devices/cover344_pack")
}

@Config(qualifiers = COVER412)
class Cover412HomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/cover412_home")
}

@Config(qualifiers = COVER412)
class Cover412GameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/cover412_game")
}

@Config(qualifiers = COVER412)
class Cover412DoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/cover412_done")
}

@Config(qualifiers = COVER412)
class Cover412TutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/cover412_tutorial")
}

@Config(qualifiers = OPEN690)
class Open690HomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/open690_home")
}

@Config(qualifiers = OPEN690)
class Open690GameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/open690_game")
}

@Config(qualifiers = OPEN690)
class Open690DoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/open690_done")
}

@Config(qualifiers = OPEN690)
class Open690TutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/open690_tutorial")
}

@Config(qualifiers = OPEN984)
class Open984HomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/open984_home")
}

@Config(qualifiers = OPEN984)
class Open984GameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/open984_game")
}

@Config(qualifiers = OPEN984)
class Open984DoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/open984_done")
}

@Config(qualifiers = OPEN984)
class Open984TutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/open984_tutorial")
}

@Config(qualifiers = OPEN829LAND)
class Open829LandHomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/open829land_home")
}

@Config(qualifiers = OPEN829LAND)
class Open829LandGameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/open829land_game")
}

@Config(qualifiers = OPEN829LAND)
class Open829LandDoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/open829land_done")
}

@Config(qualifiers = OPEN829LAND)
class Open829LandTutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/open829land_tutorial")
}

@Config(qualifiers = TABLETPORT)
class TabletPortHomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/tablet_port_home")
}

@Config(qualifiers = TABLETPORT)
class TabletPortGameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/tablet_port_game")
}

@Config(qualifiers = TABLETPORT)
class TabletPortDoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/tablet_port_done")
}

@Config(qualifiers = TABLETPORT)
class TabletPortTutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/tablet_port_tutorial")
}

@Config(qualifiers = TABLETLAND)
class TabletLandHomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/tablet_land_home")
}

@Config(qualifiers = TABLETLAND)
class TabletLandGameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/tablet_land_game")
}

@Config(qualifiers = TABLETLAND)
class TabletLandDoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/tablet_land_done")
}

@Config(qualifiers = TABLETLAND)
class TabletLandTutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/tablet_land_tutorial")
}

@Config(qualifiers = TABLETLAND)
class TabletLandPackShot : ScreenshotBase() {
    @Test
    fun run() = pack("devices/tablet_land_pack")
}

@Config(qualifiers = TABLETLAND)
class TabletLandColorShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/tablet_land_color", id = "fe-cherries-c20", source = "color-food-1")
}

@Config(qualifiers = PHONELAND)
class PhoneLandHomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/phone_land_home")
}

@Config(qualifiers = PHONELAND)
class PhoneLandGameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/phone_land_game")
}

@Config(qualifiers = PHONELAND)
class PhoneLandDoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/phone_land_done")
}

@Config(qualifiers = PHONELAND)
class PhoneLandTutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/phone_land_tutorial")
}

@Config(qualifiers = SMALLFONT, fontScale = 2.0f)
class SmallFontHomeShot : ScreenshotBase() {
    @Test
    fun run() = homeReturning("devices/small_font2x_home")
}

@Config(qualifiers = SMALLFONT, fontScale = 2.0f)
class SmallFontGameShot : ScreenshotBase() {
    @Test
    fun run() = gamePlaying("devices/small_font2x_game")
}

@Config(qualifiers = SMALLFONT, fontScale = 2.0f)
class SmallFontDoneShot : ScreenshotBase() {
    @Test
    fun run() = gameCompleted("devices/small_font2x_done")
}

@Config(qualifiers = SMALLFONT, fontScale = 2.0f)
class SmallFontTutorialShot : ScreenshotBase() {
    @Test
    fun run() = tutorialPractice("devices/small_font2x_tutorial")
}
