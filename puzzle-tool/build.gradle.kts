import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":nonogram-core"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.hyunnyApps.nonogram.tool.MainKt")
}

tasks.test { useJUnitPlatform() }

tasks.named<JavaExec>("run") {
    systemProperty("repo.root", rootProject.projectDir.absolutePath)
    maxHeapSize = "4g"
}
