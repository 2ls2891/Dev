package com.hyunnyApps.nonogram.core.format

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * 앱이 읽는 콘텐츠 형식 (docs/puzzle-format.md 참고). 필드를 바꾸면 FORMAT_VERSION을 올린다.
 * puzzle-tool(생성)과 Android 앱(읽기)이 이 정의를 공유한다.
 */

const val FORMAT_VERSION = 1

@Serializable
data class Localized(val ko: String, val en: String)

@Serializable
data class Picture(val palette: List<String>, val rows: List<String>)

@Serializable
data class DifficultyOut(val level: Int, val score: Double, val technique: String)

@Serializable
data class PuzzleOut(
    val id: String,
    val title: Localized,
    val kind: String,
    val w: Int,
    val h: Int,
    val palette: List<String>,
    val rows: List<String>,
    val reveal: Picture,
    val difficulty: DifficultyOut,
    val source: String,
    /** 같은 원본 그림에서 나온 퍼즐을 구분하는 키 (중복 방지용) */
    val sourceKey: String,
)

@Serializable
data class PackOut(
    val format: Int = FORMAT_VERSION,
    val id: String,
    val kind: String,
    val theme: String,
    val volume: Int,
    val title: Localized,
    val puzzles: List<PuzzleOut>,
)

@Serializable
data class DailyOut(
    val format: Int = FORMAT_VERSION,
    /** day 0의 날짜(기기 현지 날짜 기준). dayIndex = (오늘 - epoch)일 */
    val epoch: String,
    /** 요일별(월~일) 목표 난이도. 문서화용 */
    val weekdayLevels: List<Int>,
    val puzzles: List<PuzzleOut>,
)

@Serializable
data class PackSummary(
    val id: String,
    val kind: String,
    val theme: String,
    val volume: Int,
    val title: Localized,
    /** 테마 이름 (볼륨 번호 없이). format 1에 추가된 필드라 기본값을 둔다 */
    val themeTitle: Localized = Localized("", ""),
    val file: String,
    val count: Int,
    /** 레벨 1~5별 퍼즐 수 */
    val levels: List<Int>,
)

@Serializable
data class IndexOut(
    val format: Int = FORMAT_VERSION,
    val packs: List<PackSummary>,
    val daily: DailySummary,
    val sources: List<SourceInfo>,
)

@Serializable
data class DailySummary(val file: String, val epoch: String, val days: Int)

@Serializable
data class SourceInfo(val id: String, val name: String, val license: String, val url: String, val version: String)

/** 콘텐츠 JSON 파서. 앞으로 필드가 추가돼도 구버전 앱이 깨지지 않도록 모르는 키는 무시한다 */
val ContentJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
