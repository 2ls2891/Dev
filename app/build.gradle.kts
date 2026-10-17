import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ---- AdMob ID ----
// Google 공식 테스트 ID: https://developers.google.com/admob/android/test-ads
object TestAds {
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER_ID = "ca-app-pub-3940256099942544/9214589741" // 적응형 배너
    const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val PUBLISHER = "3940256099942544"
}

val admobFile = Properties().apply {
    val f = file("admob.properties")
    if (f.exists()) f.inputStream().use(::load)
}

fun admob(key: String): String =
    (providers.gradleProperty(key).orNull ?: admobFile.getProperty(key) ?: "").trim()

val releaseAds = mapOf(
    "admob.appId" to admob("admob.appId"),
    "admob.bannerId" to admob("admob.bannerId"),
    "admob.rewardedId" to admob("admob.rewardedId"),
)

/** 릴리즈 광고 ID 문제 목록 (비어 있으면 정상) */
val releaseAdProblems: List<String> = releaseAds.mapNotNull { (key, value) ->
    val pattern = if (key == "admob.appId") Regex("""ca-app-pub-\d{16}~\d{10}""") else Regex("""ca-app-pub-\d{16}/\d{10}""")
    when {
        value.isEmpty() -> "$key 가 비어 있음"
        !pattern.matches(value) -> "$key 형식 오류: $value"
        TestAds.PUBLISHER in value -> "$key 에 테스트 ID가 들어 있음"
        else -> null
    }
}

android {
    namespace = "com.hyunnyApps.nonogram"
    compileSdk = 37

    defaultConfig {
        // ⚠️ 스토어에 한 번 올리면 바꿀 수 없다. (디버그 빌드는 .debug 가 붙어 운영 앱과 함께 설치 가능)
        applicationId = "com.hyunnyApps.nonogram"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // 디버그 전용 UMP 테스트: ./gradlew installDebug -Pump.debugEea=true [-Pump.testDeviceId=해시ID]
        buildConfigField("boolean", "UMP_DEBUG_EEA", (providers.gradleProperty("ump.debugEea").orNull == "true").toString())
        buildConfigField("String", "UMP_TEST_DEVICE_ID", "\"${providers.gradleProperty("ump.testDeviceId").orNull.orEmpty()}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = TestAds.APP_ID
            buildConfigField("String", "AD_BANNER_ID", "\"${TestAds.BANNER_ID}\"")
            buildConfigField("String", "AD_REWARDED_ID", "\"${TestAds.REWARDED_ID}\"")
            buildConfigField("boolean", "TEST_ADS", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = releaseAds.getValue("admob.appId")
            buildConfigField("String", "AD_BANNER_ID", "\"${releaseAds.getValue("admob.bannerId")}\"")
            buildConfigField("String", "AD_REWARDED_ID", "\"${releaseAds.getValue("admob.rewardedId")}\"")
            buildConfigField("boolean", "TEST_ADS", "false")
            buildConfigField("boolean", "UMP_DEBUG_EEA", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // 화면 스냅샷(Roborazzi): JVM에서 실제 Compose 렌더링
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                it.systemProperty("roborazzi.test.record", "true")
                it.systemProperty("roborazzi.output.dir", rootProject.file("docs/screenshots").absolutePath)
                it.maxHeapSize = "4g"
                // Robolectric 테스트끼리 메인 디스패처 상태가 섞이지 않도록 테스트 클래스마다 새 JVM
                it.forkEvery = 1
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 퍼즐 콘텐츠(content/packs)를 그대로 assets 루트로 묶는다
    sourceSets {
        getByName("main") {
            assets.directories.add(rootProject.file("content/packs").path)
        }
    }
}

/** 라이선스 고지(content/licenses)를 assets/licenses/ 로 복사하는 생성 태스크 */
abstract class CopyLicenseAssets : DefaultTask() {
    @get:InputDirectory
    abstract val source: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = output.get().asFile
        out.deleteRecursively()
        source.get().asFile.copyRecursively(out.resolve("licenses"), overwrite = true)
    }
}

val copyLicenseAssets = tasks.register<CopyLicenseAssets>("copyLicenseAssets") {
    source.set(rootProject.layout.projectDirectory.dir("content/licenses"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyLicenseAssets, CopyLicenseAssets::output)
    }
}

// 릴리즈 빌드는 운영 광고 ID가 올바를 때만 진행한다 (테스트 ID/빈 값으로 출시 방지)
val checkReleaseAdIds by tasks.registering {
    val problems = releaseAdProblems
    doLast {
        if (problems.isNotEmpty()) {
            throw GradleException(
                "운영 광고 ID 설정이 필요합니다 (app/admob.properties):\n - " + problems.joinToString("\n - ")
            )
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkReleaseAdIds) }

dependencies {
    implementation(project(":nonogram-core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.work.runtime)
    implementation(libs.play.services.ads)
    implementation(libs.ump)

    // Android 단위 테스트는 JUnit4 러너를 쓴다 (kotlin.test 애너테이션 → org.junit)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}
