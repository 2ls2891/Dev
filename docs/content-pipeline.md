# 콘텐츠 파이프라인

```
fetch_sources.sh          고정 버전 원본 받기 (content/.cache, git 제외)
   ↓
rasterize_fluent.py       SVG → 크기별(10~30) 후보 격자 + 클리어 그림 (content/build/candidates.json)
   ↓
puzzle-tool build         앱과 같은 솔버로 검증 → 난이도 산정 → 테마 팩/데일리 구성 (content/packs)
   ↓
puzzle-tool preview       팩별 미리보기 PNG (content/build/preview) → 눈으로 검수
```

## 실행
```bash
pip install cairosvg pillow
content/scripts/fetch_sources.sh
python3 -I content/scripts/rasterize_fluent.py content/.cache/fluentui-emoji content/build/candidates.json --cldr-dir content/.cache/cldr
./gradlew :puzzle-tool:run --args="build content/build/candidates.json content/packs"
./gradlew :puzzle-tool:run --args="preview content/packs content/build/preview"
./gradlew :puzzle-tool:run --args="testvectors testvectors content/packs"
./gradlew test
```

## 품질 기준 (자동)
| 항목 | 기준 |
|---|---|
| 유일해 | 줄 추론 + probing만으로 끝까지 풀려야 채택 (찍기 불필요, 해 1개 보장) |
| 흑백 충실도 | 줄인 격자와 고해상도 원본의 IoU ≥ 0.60 (10×10은 0.72) |
| 채움 비율 | 흑백 15~75%, 컬러 25% 이상 |
| 컬러 | 15×15 이상, 2~6색, 색끼리 RGB 거리 48 이상 (비슷한 색은 래스터화 단계에서 병합) |

## 난이도
`nonogram-core/Difficulty.kt` 한 곳에서 정의한다.
- 레벨 = 크기 단계(≤10:1, ≤15:2, ≤20:3, ≤25:4, 그 이상:5) + probing 필요 시 1 (최대 5)
- 점수 = 칸 수 × (1 + 줄 추론 라운드/10) × (1 + probing 제거 수/20) → 팩 안 정렬용
- 그림마다 크기 하나만 수록한다(같은 그림 중복 방지). 크기는 그림별로 고정된 해시로 목표를 정하고
  품질 기준을 통과하는 가장 가까운 크기를 쓴다. 분포: 흑백 10/15/20/25/30 = 10/30/30/20/10%, 컬러 15/20/25 = 35/40/25%

## 콘텐츠 추가 (출시 후)
빌드는 기본적으로 **증분**이다. 이미 수록된 퍼즐(팩·데일리)은 그대로 두고, 새 그림만 새 볼륨/데일리 뒤쪽에 붙인다.
→ 사용자 진행 데이터와 데일리 일정이 깨지지 않는다.

1. 새 소스 추가: `content/scripts/` 에 래스터화 스크립트를 추가해 같은 후보 형식(`Candidate`)으로 출력한다.
   라이선스 확인 후 `content/licenses/` 와 `Builder.kt` 의 `SOURCES` 에 등록한다.
2. 여러 후보 파일을 합쳐서 `build` 를 실행한다 (합치기: `jq -s add a.json b.json > all.json`).
3. `content/REPORT.md` 와 미리보기를 확인하고 커밋한다.

`--fresh` 는 출시 전에만 쓴다. 출시 후에 쓰면 퍼즐 배치가 바뀌어 진행 데이터·데일리 일정이 깨진다.
데일리 epoch(기본 2027-01-04, 월요일)도 출시 전에 확정해야 하며 이후 변경 불가다.

## 더 늘리고 싶을 때
- 탈락한 `not-unique` 그림(약 250개): "처음부터 채워진 힌트 칸(givens)"을 몇 개 주면 살릴 수 있다 → 형식 확장 필요.
- CC0 도트 소스(예: Kenney 1-Bit Pack, CC0)를 추가하면 16×16 흑백 퍼즐이 1,000개 이상 늘어난다.
- 무작위 생성 퍼즐(무한 모드): core 솔버로 유일해만 걸러 쓰면 된다.
