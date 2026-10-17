package nonogram.app.ads

import nonogram.app.BuildConfig

/**
 * 광고 단위 ID. 빌드 타입별로 app/build.gradle.kts 에서 주입된다.
 * - debug  : Google 공식 테스트 ID (실제 광고 클릭으로 인한 계정 정지 위험 없음)
 * - release: app/admob.properties 의 운영 ID (비어 있거나 테스트 ID면 빌드 실패)
 */
object AdConfig {
    const val BANNER_ID: String = BuildConfig.AD_BANNER_ID
    const val REWARDED_ID: String = BuildConfig.AD_REWARDED_ID
    const val IS_TEST_ADS: Boolean = BuildConfig.TEST_ADS

    /** 퍼즐 게임 특성상 성인용 광고는 받지 않는다 */
    const val MAX_CONTENT_RATING = com.google.android.gms.ads.RequestConfiguration.MAX_AD_CONTENT_RATING_T
}
