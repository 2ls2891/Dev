package nonogram.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import nonogram.app.ads.AdsManager
import nonogram.app.ads.ConsentManager
import nonogram.app.ads.RewardedAdManager
import nonogram.app.data.ContentRepository
import nonogram.app.data.PlayerRepository
import nonogram.app.data.ProgressRepository
import java.time.LocalDate

/** 앱 수명 동안 하나씩만 존재하는 객체들 (수동 DI). ViewModel 팩토리가 여기서 의존성을 꺼낸다. */
class AppContainer(app: Application) {
    /** 화면이 사라져도 끝나야 하는 작업(저장 등)용 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val content = ContentRepository(app.assets)
    val progress = ProgressRepository(app.filesDir, appScope)
    val player = PlayerRepository(app)

    val consent = ConsentManager(app)
    val ads = AdsManager(app, consent, appScope)
    val rewarded = RewardedAdManager(app, ads, appScope)

    /** 테스트에서 바꿀 수 있도록 오늘 날짜 공급자를 한 곳에 둔다 */
    var today: () -> LocalDate = { LocalDate.now() }
}
