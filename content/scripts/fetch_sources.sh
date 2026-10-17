#!/usr/bin/env bash
# 원본 소스 에셋을 고정된 버전으로 content/.cache 에 받는다 (git에는 커밋하지 않음).
# 버전을 올릴 때는 아래 값만 바꾸고, 생성 결과(content/packs)의 변화를 검토한 뒤 커밋한다.
set -euo pipefail

FLUENT_COMMIT="1ffb34c752ecf5d402f04cfb4b392c77f57c54bc"   # microsoft/fluentui-emoji (MIT)
CLDR_TAG="48.0.0"                                           # unicode-org/cldr-json (Unicode License v3)

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="$ROOT/.cache"
mkdir -p "$CACHE"

if [ ! -d "$CACHE/fluentui-emoji/.git" ]; then
  git clone -q --filter=blob:none --no-checkout https://github.com/microsoft/fluentui-emoji.git "$CACHE/fluentui-emoji"
fi
git -C "$CACHE/fluentui-emoji" sparse-checkout set --no-cone \
  '/LICENSE' '/assets/*/metadata.json' \
  '/assets/*/Flat/*.svg' '/assets/*/High Contrast/*.svg' \
  '/assets/*/Default/Flat/*.svg' '/assets/*/Default/High Contrast/*.svg'
git -C "$CACHE/fluentui-emoji" fetch -q --depth 1 origin "$FLUENT_COMMIT"
git -C "$CACHE/fluentui-emoji" checkout -q "$FLUENT_COMMIT"

mkdir -p "$CACHE/cldr"
base="https://raw.githubusercontent.com/unicode-org/cldr-json/$CLDR_TAG"
curl -fsSL "$base/cldr-json/cldr-annotations-full/annotations/ko/annotations.json" -o "$CACHE/cldr/ko.json"
curl -fsSL "$base/cldr-json/cldr-annotations-derived-full/annotationsDerived/ko/annotations.json" -o "$CACHE/cldr/ko_derived.json"
curl -fsSL "$base/LICENSE" -o "$CACHE/cldr/LICENSE"

echo "fluentui-emoji @ $FLUENT_COMMIT, cldr-json @ $CLDR_TAG → $CACHE"
