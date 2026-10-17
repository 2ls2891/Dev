package com.hyunnyApps.nonogram.tool

import java.io.File

private const val USAGE = """
사용법 (저장소 루트에서 ./gradlew :puzzle-tool:run --args="...")
  build <candidates.json> <packs 출력 폴더> [--fresh]   후보 검증·난이도 산정·팩/데일리 구성
  preview <packs 폴더> <png 출력 폴더>                   팩별 미리보기 이미지
  testvectors <testvectors 폴더> <packs 폴더>            iOS 포팅 검증용 테스트 벡터
"""

fun main(args: Array<String>) {
    // gradle run 은 하위 프로젝트 폴더에서 실행되므로 상대 경로는 저장소 루트 기준으로 해석한다
    val root = System.getProperty("repo.root")?.let(::File) ?: File(".")
    fun path(p: String) = File(p).let { if (it.isAbsolute) it else File(root, p) }
    when (args.firstOrNull()) {
        "build" -> {
            require(args.size >= 3) { USAGE }
            val report = build(BuildConfig(path(args[1]), path(args[2]), fresh = "--fresh" in args))
            File(path(args[2]).parentFile, "REPORT.md").writeText(report.toString() + "\n")
            println(report)
        }
        "preview" -> {
            require(args.size >= 3) { USAGE }
            writePreviews(path(args[1]), path(args[2]))
        }
        "testvectors" -> {
            require(args.size >= 3) { USAGE }
            writeTestVectors(path(args[1]), path(args[2]))
        }
        else -> println(USAGE)
    }
}
