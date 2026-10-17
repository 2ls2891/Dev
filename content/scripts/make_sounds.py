#!/usr/bin/env python3
"""
효과음 합성 (외부 음원 없음 → 라이선스 문제 없음). 결과: app/src/main/res/raw/*.wav
  python3 content/scripts/make_sounds.py
"""
import math
import os
import random
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "res", "raw")


def write(name, samples):
    os.makedirs(OUT, exist_ok=True)
    peak = max(1e-9, max(abs(s) for s in samples))
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(s / peak * 0.85 * 32767)) for s in samples))


def env(i, n, attack=0.005, release=None):
    t = i / RATE
    a = min(1.0, t / attack) if attack > 0 else 1.0
    r = 1.0 - i / n if release is None else math.exp(-t / release)
    return a * r


def tone(freq, dur, release=0.18, harmonics=((1, 1.0), (2, 0.25), (3, 0.08))):
    n = int(RATE * dur)
    return [sum(a * math.sin(2 * math.pi * freq * h * i / RATE) for h, a in harmonics) * env(i, n, release=release) for i in range(n)]


def mix(parts):
    total = max(off + len(p) for off, p in parts)
    out = [0.0] * total
    for off, p in parts:
        for i, s in enumerate(p):
            out[off + i] += s
    return out


random.seed(7)
# 연필 사각: 짧은 고역 노이즈
n = int(RATE * 0.045)
prev = 0.0
pencil = []
for i in range(n):
    x = random.uniform(-1, 1)
    hp = x - prev  # 단순 고역 통과
    prev = x
    pencil.append(hp * env(i, n, attack=0.002, release=0.012))
write("sfx_pencil", pencil)

# 지우개/✕: 더 낮고 짧은 노이즈
n = int(RATE * 0.05)
lp = 0.0
erase = []
for i in range(n):
    lp = lp * 0.7 + random.uniform(-1, 1) * 0.3
    erase.append(lp * env(i, n, attack=0.002, release=0.015))
write("sfx_cross", erase)

# 줄 완성: 맑은 두 음 (미 → 라)
d = int(RATE * 0.07)
write("sfx_line", mix([(0, tone(1318.5, 0.25, 0.08)), (d, tone(1760.0, 0.35, 0.12))]))

# 퍼즐 완성: 도-미-솔-도 아르페지오
notes = [523.25, 659.25, 783.99, 1046.5]
step = int(RATE * 0.11)
write("sfx_complete", mix([(k * step, tone(f, 0.9 if k == 3 else 0.4, 0.35 if k == 3 else 0.15)) for k, f in enumerate(notes)]))

# 도장 쾅: 낮은 쿵 + 짧은 노이즈
n = int(RATE * 0.25)
thump = [math.sin(2 * math.pi * (120 - 60 * i / n) * i / RATE) * env(i, n, attack=0.001, release=0.06) for i in range(n)]
noise = [random.uniform(-1, 1) * 0.4 * env(i, n, attack=0.001, release=0.01) for i in range(n)]
write("sfx_stamp", [a + b for a, b in zip(thump, noise)])
print("ok")
