# Android 앱 구조

## 아키텍처: MVVM + 단방향 데이터 흐름 (Jetpack Compose)

```
UI (Compose Screen) ──이벤트──▶ ViewModel ──호출──▶ core 규칙(GameSession) / Repository
        ▲                          │
        └──── StateFlow<UiState> ◀─┘
```

- **Screen**: 상태를 그리기만 한다. 로직 없음. (`ui/*/…Screen.kt`)
- **ViewModel**: 화면 상태(`StateFlow<…UiState>`)를 만들고, 사용자 이벤트를 받아 core/Repository에 위임한다.
- **core (`nonogram-core`)**: 게임 규칙(드래그·실행 취소·클리어 판정·힌트), 솔버, 데일리/연속 기록. Android 의존성 0 → JVM 테스트로 검증, iOS 포팅 기준.
- **Repository**: 콘텐츠(assets), 진행 저장(AtomicFile), 플레이어 상태(DataStore).
- **DI**: `AppContainer` 수동 DI (Hilt 없이 단순하게). ViewModel은 `viewModelFactory`로 생성.

MVP 대신 MVVM을 고른 이유: Compose는 상태를 관찰해 다시 그리는 구조라 ViewModel+StateFlow가 자연스럽고,
화면 회전·프로세스 재생성 시 상태 보존(ViewModel, SavedStateHandle)을 플랫폼이 지원한다. Google 공식 권장 구조이기도 하다.

```
app/src/main/kotlin/com/hyunnyApps/nonogram/
├─ NonogramApplication, AppContainer, MainActivity
├─ ads/      AdConfig, ConsentManager(UMP), AdsManager(초기화), RewardedAdManager, BannerAd
├─ data/     ContentRepository, ProgressRepository, PlayerRepository
└─ ui/       home/ pack/ game/ settings/ components/ theme/ NonogramNavHost
```

## 광고

| | debug | release |
|---|---|---|
| AdMob 앱 ID | Google 테스트 ID | `app/admob.properties` |
| 배너 / 보상형 | Google 테스트 ID | `app/admob.properties` |
| 검증 | - | 비어 있음·형식 오류·테스트 ID면 **빌드 실패** |

### 흐름
1. 앱 시작: 이전 동의가 있으면 즉시 `MobileAds.initialize` (백그라운드 스레드)
2. 매 실행마다 UMP 동의 정보 갱신 → 필요 시(EEA·영국 등) 동의 양식 → 끝나면 초기화 재시도
3. 초기화 완료(`AdsManager.ready`) 전에는 어떤 광고도 요청하지 않는다
4. 설정 화면: 동의가 필요한 지역이면 "개인정보 · 광고 동의 설정" 버튼 노출 (UMP 필수 요구사항)

### 배너 (하단 고정, 앱 전체에 1개)
- 대형 앵커 적응형 배너(`getLargeAnchoredAdaptiveBannerAdSize`, Google 현재 권장)
- 로드 전에도 배너 높이만큼 자리 확보 → 광고가 늦게 떠도 보드가 밀리지 않음 (오클릭 방지)
- 게임 조작 버튼과 배너 사이 20dp 여백, 완료 화면 버튼은 화면 위쪽에 배치
- 생명주기에 맞춰 pause/resume/destroy
- 성인용 광고 차단: 최대 콘텐츠 등급 T

### 보상형 (힌트)
- 하루 무료 힌트 3개, 다 쓰면 "광고 보고 힌트 3개 받기" 대화상자 (사용자가 선택할 때만 광고)
- 미리 1개 로드, 보여준 뒤 즉시 다음 광고 로드, 실패 시 30초→최대 5분 백오프 재시도

### 운영 ID 넣기
`app/admob.properties`에 값을 채운다.
```
admob.appId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
admob.bannerId=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
admob.rewardedId=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
```
CI에서는 `-Padmob.appId=…` 처럼 넘겨도 된다(파일보다 우선).

### 디버그에서 동의 양식 테스트
```bash
./gradlew installDebug -Pump.debugEea=true                       # 에뮬레이터
./gradlew installDebug -Pump.debugEea=true -Pump.testDeviceId=…   # 실기기 (logcat에 해시 ID가 출력됨)
```

## 디자인 컨셉: 모눈종이 노트

"AI 템플릿 같은" 기본 Material 화면을 피하려고, 앱 전체를 하나의 물성으로 통일했다.
- **배경**: 크림색 모눈종이 (`PaperBackground`), 밤에는 남색 노트
- **색**: 만년필 잉크(네이비) · 빨간 색연필(강조/버튼) · 형광펜(선택/강조) · 마스킹테이프
- **글꼴**: 제목·숫자·버튼은 손글씨체 Gaegu(OFL), 본문은 시스템 글꼴 (가독성)
- **아이콘**: 이모지·기성 아이콘 대신 직접 그린 12×12 픽셀 아이콘 (`PixelIcons`)
- **부품** (`ui/components/Notebook.kt`): 종이 카드(살짝 기울임, 테이프), 스티커 버튼(잉크 테두리 + 딱 떨어지는 그림자, 누르면 눌림), 난이도 꼬리표, 연필 밑줄, "참 잘했어요" 도장
- **팩** = 스프링 노트, **컬렉션** = 폴라로이드, **퍼즐 목록** = 메모지

## 게임 연출·손맛
- **줄 완성**: 단서 위로 형광펜이 지나가고(0.7초) 숫자에 연필 줄이 그어짐 + 효과음 + 진동
- **칠하기**: 칸마다 연필 사각 소리 + 가벼운 진동 (같은 소리 45ms 간격 제한)
- **완성 연출**: 단서·격자가 사라지고 그림이 가운데로 모이며 대각선 물결로 컬러 그림이 칠해짐(2초) → "참 잘했어요" 도장 쾅 → 결과. 다시 열면 연출 없이 결과 (`ui/game/RevealBoard.kt`)
- **효과음**: 외부 음원 없이 `content/scripts/make_sounds.py`로 합성 (라이선스 문제 없음)
- 설정에서 효과음·진동 각각 끄기 가능

## 재방문 장치
- **이어서 그리기**: 마지막으로 연 미완성 퍼즐을 홈 상단에서 바로 이어하기 (칠한 비율 표시)
- **매일 알림**: 설정 또는 첫 완성 후 제안 카드에서 켬. 아침 8시/낮 12시/저녁 7시/밤 9시. 오늘의 퍼즐을 이미 풀었으면 보내지 않음. 알림을 누르면 오늘의 퍼즐로 바로 이동. Android 13+ 알림 권한은 켤 때 요청 (`reminder/DailyReminder.kt`, WorkManager)
- **연속 기록 지키기**: 2일 이상 이어온 기록이 어제 하루 끊겼으면 홈에 카드 → 보상형 광고 시청 시 어제 푼 것으로 처리
- **칠하기 색**: 6가지 (다크 모드는 밝은 색으로 자동 전환)
- **내 컬렉션**: 완성한 컬러 그림 모음

## 저장
- 퍼즐 진행: `files/progress/<puzzleId>.json` (AtomicFile), 목록용 요약 `summary.json`
- 저장 시점: 칸 변경 후 0.4초 디바운스, 화면이 가려질 때(onPause), ViewModel 종료 시. 앱 범위 코루틴에서 써서 화면이 닫혀도 끝까지 쓴다
- 손상되었거나 판 크기가 다른 저장 파일은 무시하고 새로 시작한다
- 힌트·연속 기록: DataStore

## 로컬에서 실행하기

### 터미널만으로 (Android Studio 불필요) — 권장
```bash
git clone -b claude/blissful-pasteur-grgmnf https://github.com/2ls2891/Dev.git nonogram
cd nonogram
scripts/run-android.sh          # SDK 설치 → 에뮬레이터 생성·부팅 → 빌드 → 설치 → 실행
```
- 필요한 것: **JDK 17+** 하나 (`brew install --cask temurin@21`). 나머지(SDK, 에뮬레이터, 시스템 이미지)는 스크립트가 받는다.
- 처음 한 번은 약 3GB를 받는다. 이후 실행은 빌드·설치만 하므로 빠르다 (에뮬레이터가 켜져 있으면 재사용).
- SDK 위치: `ANDROID_HOME` → `local.properties` → `~/Library/Android/sdk` (Android Studio 기본 위치와 같아 공유된다)
- Apple Silicon은 arm64 이미지, Intel 맥은 x86_64 이미지를 자동 선택 (Android 16 / API 36, Google Play 포함)

| 명령 | 용도 |
|---|---|
| `scripts/run-android.sh --logs` | 실행 후 앱 로그 보기 (크래시 확인) |
| `scripts/run-android.sh --eea` | 유럽 사용자처럼 광고 동의 양식 테스트 |
| `scripts/run-android.sh --wipe` | 에뮬레이터 초기화 후 실행 (첫 실행 상태) |
| `scripts/run-android.sh --device` | USB로 연결한 실기기에 설치 |
| `scripts/run-android.sh --stop` | 에뮬레이터 종료 |
| `scripts/run-android.sh --lang en` | 앱을 영어로 보기 (기본은 한국어. 기기 언어는 그대로) |

코드를 고친 뒤에는 같은 명령을 다시 실행하면 된다.

### Android Studio로
**Android Studio Quail 4 (2026.1.4) 이상** (AGP 9.4 지원 버전)에서 폴더를 열고 `app` 실행 구성을 Run.

디버그 빌드는 Google 테스트 광고가 나온다 (배너에 "Test Ad" 표시).
Python·콘텐츠 도구는 필요 없다 (생성된 퍼즐이 `content/packs/`에 커밋되어 있음).

## 화면 스냅샷 (디자인 검토)

에뮬레이터 없이 JVM(Robolectric)에서 실제 화면을 렌더링해 `docs/screenshots/`에 저장한다.
```bash
./gradlew :app:testDebugUnitTest      # 단위 테스트 + 화면 스냅샷 갱신
```
시나리오는 `app/src/test/.../screenshot/Screenshots.kt` (홈, 팩, 게임 진행/완료, 컬러, 튜토리얼, 규칙).
화면을 바꾸면 스냅샷도 함께 커밋해 변경 전후를 비교한다.

## 출시 전 체크리스트
- [x] `applicationId` = `com.hyunnyApps.nonogram` (디버그: `com.hyunnyApps.nonogram.debug`)
- [ ] `app/admob.properties` 운영 ID 입력
- [ ] 데일리 시작일(epoch) 확정 (`puzzle-tool/src/main/kotlin/com/hyunnyApps/nonogram/tool/Builder.kt` 의 `epoch`, 현재 2027-01-04)
- [ ] 앱 이름·아이콘 확정 (`res/values*/strings.xml`, `res/drawable/ic_launcher_foreground.xml`)
- [ ] 서명 키 / Play App Signing 설정, `./gradlew bundleRelease`
- [ ] 개발자 웹사이트에 app-ads.txt 게시 → AdMob 앱 검토
- [ ] Play Console: 데이터 보안(광고 ID 수집), 타깃 연령 13세 이상, 광고 포함 표시
- [ ] 개인 계정이면 비공개 테스트 12명 × 14일
