# 퍼즐 데이터 형식 (format 1)

앱(Android/iOS)은 `content/packs/` 를 번들에 넣고 읽는다. 필드를 바꾸면 `FORMAT_VERSION`을 올린다.

## 칸 인코딩 (RowCodec)
한 줄 = 문자열. `.` = 빈칸, `1`~`9` = 팔레트 1~9번, `a`~`z` = 10~35번.
팔레트 번호 n의 색 = `palette[n-1]`.

## index.json
```json
{
  "format": 1,
  "packs": [{ "id": "mono-nature-1", "kind": "mono", "theme": "nature", "volume": 1,
              "title": {"ko": "동물과 자연 1", "en": "Animals & Nature 1"},
              "file": "mono-nature-1.json", "count": 38, "levels": [1, 9, 14, 9, 5] }],
  "daily": { "file": "daily.json", "epoch": "2027-01-04", "days": 401 },
  "sources": [ ... ]
}
```
`levels` = 레벨 1~5별 퍼즐 수.

## 팩 파일 (`<pack id>.json`)
```json
{
  "format": 1, "id": "mono-nature-1", "kind": "mono", "theme": "nature", "volume": 1,
  "title": {"ko": "...", "en": "..."},
  "puzzles": [{
    "id": "fe-cat_face-m20",
    "title": {"ko": "고양이 얼굴", "en": "Cat face"},
    "kind": "mono",                 // mono: 흑백(색 1개), color: 컬러 노노그램
    "w": 20, "h": 18,
    "palette": ["#212121"],
    "rows": ["....11...", "..."],   // 정답
    "reveal": { "palette": ["#F9A825", "..."], "rows": ["..."] },  // 클리어 후 보여줄 컬러 그림 (w×h 동일)
    "difficulty": { "level": 3, "score": 512.0, "technique": "line" },  // technique: line | probing
    "source": "fluent-emoji",
    "sourceKey": "cat_face"
  }]
}
```
- 팩 안의 퍼즐은 쉬운 순으로 정렬돼 있다.
- **퍼즐 `id`는 영구 키**다. 진행 데이터 저장 키로 사용한다. 출시 후 기존 id의 내용은 절대 바뀌지 않는다(빌드 도구가 보장).
- 앱은 저장된 진행 데이터의 칸 수가 `w*h`와 다르면 해당 진행을 버리도록 방어한다.

## daily.json
```json
{ "format": 1, "epoch": "2027-01-04", "weekdayLevels": [1,2,2,3,3,4,5], "puzzles": [ <퍼즐>, ... ] }
```
- `dayIndex = (기기 현지 오늘 날짜 - epoch)` 일수. 초/86400으로 나누지 말고 달력 날짜 차이로 계산한다(서머타임).
- `dayIndex < 0` 이면 0, `dayIndex >= puzzles.size` 이면 `dayIndex % puzzles.size` (앱 업데이트로 보충 전까지 반복).
- epoch은 월요일이며 `weekdayLevels[dayIndex % 7]`이 그날의 목표 난이도다.
- 데일리 퍼즐은 팩에 들어있지 않은 전용 퍼즐이다.

## 클리어 판정
정답 배열과 비교하지 말고, 현재 칠한 칸으로 계산한 단서가 모든 행/열 단서와 같은지로 판정한다.
모든 퍼즐은 해가 유일하므로 결과는 같고, 이 방식이 더 견고하다.

## 테스트 벡터
`testvectors/` 의 JSON을 Kotlin(`nonogram-core` 테스트)과 Swift 테스트가 함께 검증한다.
- `line_solver.json`: `in`/`out`은 칸 마스크(bit0 = 빈칸, bit c = 색 c). `out = null`은 모순.
- `board_solver.json`: 판 전체 풀이 결과(outcome, 라운드 수, probing 제거 수, 레벨).
- `daily_index.json`: 날짜 → dayIndex.
