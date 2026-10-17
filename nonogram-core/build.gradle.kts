import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Android 앱 모듈이 그대로 의존할 수 있도록 순수 Kotlin/JVM 17 라이브러리로 유지한다.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    systemProperty("testvectors.dir", rootProject.file("testvectors").absolutePath)
    systemProperty("content.dir", rootProject.file("content/packs").absolutePath)
}
