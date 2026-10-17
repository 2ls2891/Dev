#!/usr/bin/env python3
"""
Fluent Emoji(MIT) SVG → 노노그램 후보 JSON.

이미지 처리만 담당한다. 풀이 가능 여부/난이도/팩 구성은 puzzle-tool(Kotlin, 앱과 같은 솔버)이 한다.

각 이모지에 대해 크기(긴 변 기준)별로 후보를 만든다.
  - mono : High Contrast 판을 칸 단위로 줄여 어두운 칸 = 채움. 클리어 후 보여줄 컬러 그림(reveal)은 Flat 판.
  - color: Flat 판을 소수의 색으로 줄인 컬러 노노그램 (추후 컬러 모드용)

사용법:
  python3 -I content/scripts/rasterize_fluent.py <fluentui-emoji 경로> <출력 json> [--cldr-dir DIR] [--limit N]
"""
import argparse
import glob
import io
import json
import os
import sys

import cairosvg
from PIL import Image

SIZES = [10, 15, 20, 25, 30]
RENDER = 600            # 고해상도 렌더 크기(px). 모든 크기의 배수에 가깝게 크게 잡는다
DARK_THRESHOLD = 0.5    # 칸 면적 중 어두운 비율이 이 이상이면 채움
ALPHA_THRESHOLD = 0.5   # 칸 면적 중 불투명 비율이 이 이상이면 색칠된 칸
MAX_REVEAL_COLORS = 12
MAX_PUZZLE_COLORS = 6
MIN_PUZZLE_COLOR_DISTANCE = 56  # RGB 거리. puzzle-tool 의 similar-colors 기준(48)보다 크게
EXCLUDED_GROUPS = {"Flags"}


def render(svg_path):
    png = cairosvg.svg2png(url=svg_path, output_width=RENDER, output_height=RENDER)
    return Image.open(io.BytesIO(png)).convert("RGBA")


def find_svg(emoji_dir, style):
    for pattern in (f"{style}/*.svg", f"Default/{style}/*.svg"):
        hits = sorted(glob.glob(os.path.join(glob.escape(emoji_dir), pattern)))
        if hits:
            return hits[0]
    return None


def darkness_image(rgba):
    """흰 배경에 합성한 뒤 '어두움'(0~1)을 L 채널 이미지로. #212121을 1.0으로 정규화"""
    white = Image.new("RGBA", rgba.size, (255, 255, 255, 255))
    lum = Image.alpha_composite(white, rgba).convert("L")
    base = 0x21
    return lum.point(lambda v: max(0, min(255, round((255 - v) * 255 / (255 - base)))))


def downsample(img, w, h):
    if img.mode == "RGBA":
        return img.convert("RGBa").resize((w, h), Image.Resampling.BOX).convert("RGBA")
    return img.resize((w, h), Image.Resampling.BOX)


def mono_grid(dark_crop, w, h):
    small = downsample(dark_crop, w, h)
    px = small.load()
    return [[1 if px[x, y] / 255 >= DARK_THRESHOLD else 0 for x in range(w)] for y in range(h)]


def fidelity(grid, dark_crop):
    """줄인 격자를 원래 크기로 키웠을 때 고해상도 이진 그림과의 IoU"""
    h, w = len(grid), len(grid[0])
    cw, ch = dark_crop.size
    ref = dark_crop.point(lambda v: 255 if v / 255 >= DARK_THRESHOLD else 0).load()
    inter = union = 0
    step = 2  # 속도를 위해 2px 간격 샘플링
    for y in range(0, ch, step):
        gy = min(h - 1, y * h // ch)
        for x in range(0, cw, step):
            a = grid[gy][min(w - 1, x * w // cw)] == 1
            b = ref[x, y] > 0
            inter += a and b
            union += a or b
    return inter / union if union else 0.0


def merge_similar(palette_rgb, counts, assign, min_distance):
    """가까운 색끼리 합친다(적게 쓰인 색을 많이 쓰인 색으로). 컬러 퍼즐에서 구분 안 되는 색을 없애기 위함"""
    alive = set(range(len(palette_rgb)))
    while len(alive) > 1:
        best = None
        for i in alive:
            for j in alive:
                if i < j:
                    d = sum((a - b) ** 2 for a, b in zip(palette_rgb[i], palette_rgb[j])) ** 0.5
                    if d < min_distance and (best is None or d < best[0]):
                        best = (d, i, j)
        if best is None:
            break
        _, i, j = best
        keep, drop = (i, j) if counts[i] >= counts[j] else (j, i)
        counts[keep] += counts[drop]
        alive.discard(drop)
        assign = [keep if a == drop else a for a in assign]
    return assign


def quantize(rgba_small, max_colors, min_distance=0):
    """불투명 칸만 max_colors 이하로 줄인다. 반환: (팔레트 hex 목록, 칸 값 격자[0=투명, 1..])"""
    w, h = rgba_small.size
    px = rgba_small.load()
    opaque = [(x, y) for y in range(h) for x in range(w) if px[x, y][3] / 255 >= ALPHA_THRESHOLD]
    grid = [[0] * w for _ in range(h)]
    if not opaque:
        return [], grid
    strip = Image.new("RGB", (len(opaque), 1))
    strip.putdata([px[x, y][:3] for x, y in opaque])
    q = strip.quantize(colors=max_colors, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    pal = q.getpalette()
    idx = [q.getpixel((i, 0)) for i in range(len(opaque))]
    if min_distance:
        rgb = [tuple(pal[i * 3:i * 3 + 3]) for i in range(len(pal) // 3)]
        counts = [idx.count(i) for i in range(len(rgb))]
        idx = merge_similar(rgb, counts, idx, min_distance)
    used = sorted(set(idx), key=lambda i: -idx.count(i))  # 많이 쓰인 색부터 1번
    remap = {old: new + 1 for new, old in enumerate(used)}
    palette = ["#%02X%02X%02X" % tuple(pal[i * 3:i * 3 + 3]) for i in used]
    for (x, y), i in zip(opaque, idx):
        grid[y][x] = remap[i]
    return palette, grid


def encode_rows(grid):
    chars = ".123456789abcdefghijklmnopqrstuvwxyz"
    return ["".join(chars[v] for v in row) for row in grid]


def bbox_union(*images):
    boxes = [im.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox() for im in images]
    boxes = [b for b in boxes if b]
    if not boxes:
        return None
    return (min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes), max(b[3] for b in boxes))


def load_korean_names(cldr_dir):
    """CLDR 한국어 주석(tts)을 이모지 문자 → 이름으로. 변형 선택자(FE0F)는 빼고 키를 만든다"""
    names = {}
    if not cldr_dir:
        return names
    for file, root in (("ko.json", "annotations"), ("ko_derived.json", "annotationsDerived")):
        path = os.path.join(cldr_dir, file)
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as f:
            data = json.load(f)[root]["annotations"]
        for glyph, entry in data.items():
            tts = entry.get("tts")
            if tts:
                names.setdefault(glyph.replace("\ufe0f", ""), tts[0])
    return names


def process(emoji_dir, ko_names):
    meta_path = os.path.join(emoji_dir, "metadata.json")
    if not os.path.exists(meta_path):
        return []
    with open(meta_path, encoding="utf-8") as f:
        meta = json.load(f)
    if meta.get("group") in EXCLUDED_GROUPS:
        return []
    flat_svg, hc_svg = find_svg(emoji_dir, "Flat"), find_svg(emoji_dir, "High Contrast")
    if not flat_svg or not hc_svg:
        return []
    flat, hc = render(flat_svg), render(hc_svg)
    box = bbox_union(flat, hc)
    if not box:
        return []
    flat_c, dark_c = flat.crop(box), darkness_image(hc.crop(box))
    bw, bh = box[2] - box[0], box[3] - box[1]
    key = os.path.basename(flat_svg).rsplit("_flat.svg", 1)[0]
    base = {
        "source": "fluent-emoji",
        "key": key,
        "name": meta.get("cldr") or meta.get("tts") or key,
        "name_ko": ko_names.get(meta.get("glyph", "").replace("\ufe0f", "")),
        "glyph": meta.get("glyph", ""),
        "unicode": meta.get("unicode", ""),
        "group": meta.get("group", ""),
    }
    out = []
    for size in SIZES:
        scale = size / max(bw, bh)
        w, h = max(5, round(bw * scale)), max(5, round(bh * scale))
        r_pal, r_grid = quantize(downsample(flat_c, w, h), MAX_REVEAL_COLORS)
        reveal = {"palette": r_pal, "rows": encode_rows(r_grid)}

        m = mono_grid(dark_c, w, h)
        fill = sum(map(sum, m)) / (w * h)
        out.append({**base, "kind": "mono", "size": size, "w": w, "h": h,
                    "palette": ["#212121"], "rows": encode_rows(m),
                    "fidelity": round(fidelity(m, dark_c), 4), "fill": round(fill, 4), "reveal": reveal})

        c_pal, c_grid = quantize(downsample(flat_c, w, h), MAX_PUZZLE_COLORS, MIN_PUZZLE_COLOR_DISTANCE)
        c_fill = sum(1 for row in c_grid for v in row if v) / (w * h)
        out.append({**base, "kind": "color", "size": size, "w": w, "h": h,
                    "palette": c_pal, "rows": encode_rows(c_grid),
                    "fidelity": None, "fill": round(c_fill, 4), "reveal": reveal})
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("fluent_root")
    ap.add_argument("output")
    ap.add_argument("--cldr-dir", default=None, help="fetch_sources.sh가 받은 CLDR 한국어 주석 폴더")
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--only", nargs="*", default=None, help="이모지 폴더 이름 (테스트용)")
    args = ap.parse_args()

    dirs = sorted(d for d in glob.glob(os.path.join(glob.escape(args.fluent_root), "assets", "*")) if os.path.isdir(d))
    if args.only:
        dirs = [d for d in dirs if os.path.basename(d) in set(args.only)]
    if args.limit:
        dirs = dirs[:args.limit]
    ko_names = load_korean_names(args.cldr_dir)
    candidates = []
    for i, d in enumerate(dirs, 1):
        try:
            candidates += process(d, ko_names)
        except Exception as e:  # 일부 SVG 렌더 실패는 건너뛴다
            print(f"skip {os.path.basename(d)}: {e}", file=sys.stderr)
        if i % 100 == 0:
            print(f"{i}/{len(dirs)}", file=sys.stderr)
    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(candidates, f, ensure_ascii=False)
    print(f"{len(candidates)} candidates from {len(dirs)} emoji → {args.output}", file=sys.stderr)


if __name__ == "__main__":
    main()
