package com.hyunnyApps.nonogram.ads

import android.content.Context
import com.google.android.gms.ads.AdSize
import com.hyunnyApps.nonogram.BuildConfig

/**
 * 광고 단위 ID. 빌드 타입별로 app/build.gradle.kts 에서 주입된다.
 * - debug  : Google 공식 테스트 ID (실제 광고 클릭으로 인한 계정 정지 위험 없음)
 * - release: app/admob.properties 의 운영 ID (비어 있거나 테스트 ID면 빌드 실패)
 */
object AdConfig {
    const val BANNER_ID: String = BuildConfig.AD_BANNER_ID
    const val REWARDED_ID: String = BuildConfig.AD_REWARDED_ID
    const val IS_TEST_ADS: Boolean = BuildConfig.TEST_ADS

    /**
     * 하단 배너 크기.
     *  - false(기본): 320×50 표준 배너. 모든 기기에서 높이 50dp → 퍼즐 화면을 최대한 남긴다.
     *  - true       : 대형 앵커 적응형. SDK 계산 높이가 폰 약 108~123dp, 폴드 펼침·태블릿 150dp로 크다.
     *                 동영상 등 광고 수요가 더 많아 노출당 수익이 오를 수 있지만 보드가 그만큼 작아진다.
     * 수익과 이용 시간을 보고 바꿀 수 있게 한 곳에 둔다.
     */
    const val USE_LARGE_BANNER = false

    fun bannerSize(context: Context, widthDp: Int): AdSize =
        if (USE_LARGE_BANNER) AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) else AdSize.BANNER

    /** 퍼즐 게임 특성상 성인용 광고는 받지 않는다 */
    const val MAX_CONTENT_RATING = com.google.android.gms.ads.RequestConfiguration.MAX_AD_CONTENT_RATING_T
}
