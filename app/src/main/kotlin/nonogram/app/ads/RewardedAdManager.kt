package nonogram.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class RewardedState { NOT_READY, LOADING, READY }

/**
 * 보상형 광고 (힌트 충전). 미리 한 개를 로드해 두고, 보여준 뒤에는 다음 광고를 다시 로드한다.
 * 로드 실패 시 30초 → 1분 → 2분 … (최대 5분) 간격으로 재시도한다.
 * 모든 메서드는 메인 스레드에서 호출한다.
 */
class RewardedAdManager(
    private val context: Context,
    private val ads: AdsManager,
    private val scope: CoroutineScope,
    private val unitId: String = AdConfig.REWARDED_ID,
) {
    private var ad: RewardedAd? = null
    private var retryJob: Job? = null
    private var failures = 0
    private val _state = MutableStateFlow(RewardedState.NOT_READY)
    val state: StateFlow<RewardedState> = _state.asStateFlow()

    init {
        scope.launch(Dispatchers.Main) {
            ads.ready.first { it }
            load()
        }
    }

    private fun load() {
        if (!ads.ready.value || _state.value != RewardedState.NOT_READY) return
        _state.value = RewardedState.LOADING
        RewardedAd.load(context, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(loaded: RewardedAd) {
                ad = loaded
                failures = 0
                _state.value = RewardedState.READY
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "load failed: ${error.code} ${error.message}")
                ad = null
                _state.value = RewardedState.NOT_READY
                scheduleRetry()
            }
        })
    }

    private fun scheduleRetry() {
        retryJob?.cancel()
        val delayMs = (30_000L shl failures.coerceAtMost(4)).coerceAtMost(300_000L)
        failures++
        retryJob = scope.launch(Dispatchers.Main) {
            delay(delayMs)
            load()
        }
    }

    /**
     * 광고를 보여준다. 준비되지 않았으면 false (이때 로드를 재촉한다).
     * @param onRewarded 사용자가 끝까지 시청해 보상을 받을 때 1회 호출
     * @param onClosed 광고가 닫히거나 표시에 실패했을 때 호출
     */
    fun show(activity: Activity, onRewarded: () -> Unit, onClosed: () -> Unit = {}): Boolean {
        val current = ad
        if (current == null) {
            if (_state.value == RewardedState.NOT_READY) {
                retryJob?.cancel()
                load()
            }
            return false
        }
        ad = null
        _state.value = RewardedState.NOT_READY
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onClosed()
                load()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "show failed: ${error.code} ${error.message}")
                onClosed()
                load()
            }
        }
        current.show(activity) { onRewarded() }
        return true
    }

    private companion object {
        const val TAG = "RewardedAd"
    }
}
