import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

repositories { mavenCentral() }

dependencies {
    implementation(project(":nonogram-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("nonogram.tool.MainKt")
}

tasks.test { useJUnitPlatform() }

tasks.named<JavaExec>("run") {
    systemProperty("repo.root", rootProject.projectDir.absolutePath)
    maxHeapSize = "4g"
}
