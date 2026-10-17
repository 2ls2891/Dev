# Nonogram

배너 광고 기반 노노그램(그림 로직) 게임. Android 먼저, 이후 iOS.

| 경로 | 내용 |
|---|---|
| `nonogram-core/` | 순수 Kotlin 게임 로직: 줄/판 추론기(흑백·컬러), 난이도. Android 앱이 그대로 의존 |
| `puzzle-tool/` | 콘텐츠 빌드 CLI (core 솔버 재사용): 검증·난이도·팩/데일리 구성, 미리보기, 테스트 벡터 |
| `content/` | 원본 소스 스크립트, 생성된 퍼즐 팩(`packs/`), 빌드 리포트, 라이선스 |
| `testvectors/` | Kotlin/Swift 공용 검증 데이터 (iOS 포팅 검증용) |
| `docs/` | [퍼즐 형식](docs/puzzle-format.md), [콘텐츠 파이프라인](docs/content-pipeline.md) |

현재 콘텐츠: [content/REPORT.md](content/REPORT.md)

```bash
./gradlew test   # core 테스트 (전수 탐색 대조 + 테스트 벡터)
```
