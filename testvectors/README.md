# 공용 테스트 벡터

`./gradlew :puzzle-tool:run --args="testvectors testvectors content/packs"` 로 생성한다.
Kotlin(`nonogram-core` 테스트)과 iOS(Swift) 테스트가 같은 파일로 같은 결과를 검증한다.
형식은 [docs/puzzle-format.md](../docs/puzzle-format.md#테스트-벡터) 참고.
