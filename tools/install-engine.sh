#!/usr/bin/env bash
# install-engine.sh — push .rkx ke device dan tampilkan instruksi
#
# Pemakaian:
#   bash tools/install-engine.sh dist/en.example-1.rkx
#   bash tools/install-engine.sh                    # cari .rkx terbaru di dist/
#
# Prasyarat:
#   - adb di PATH
#   - device terhubung (adb devices)
set -euo pipefail

# ---------- resolve root proyek ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# ---------- argumen ----------
ARG="${1:-}"

if [[ -z "$ARG" ]]; then
    # cari .rkx terbaru di dist/
    DIST_DIR="$ROOT/dist"
    if [[ ! -d "$DIST_DIR" ]]; then
        echo "✖ dist/ belum ada. Jalankan dulu: node tools/build-rkx.mjs <engine>"
        exit 1
    fi
    LATEST="$(ls -t "$DIST_DIR"/*.rkx 2>/dev/null | head -n1 || true)"
    if [[ -z "$LATEST" ]]; then
        echo "✖ Tidak ada file .rkx di $DIST_DIR"
        exit 1
    fi
    ARG="$LATEST"
    echo "▸ Menggunakan .rkx terbaru: $(basename "$ARG")"
fi

# resolve path relatif terhadap cwd pemanggil atau root
if [[ ! -f "$ARG" ]]; then
    if [[ -f "$ROOT/$ARG" ]]; then
        ARG="$ROOT/$ARG"
    else
        echo "✖ File tidak ditemukan: $ARG"
        exit 1
    fi
fi

if [[ "${ARG##*.}" != "rkx" ]]; then
    echo "✖ Bukan file .rkx: $ARG"
    exit 1
fi

# ---------- cek adb ----------
if ! command -v adb >/dev/null 2>&1; then
    echo "✖ adb tidak ada di PATH. Install Android platform-tools dulu."
    exit 1
fi

DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -z "$DEVICES" ]]; then
    echo "✖ Tidak ada device terhubung."
    echo "  Cek dengan: adb devices"
    exit 1
fi

FIRST_DEVICE="$(echo "$DEVICES" | head -n1)"
COUNT="$(echo "$DEVICES" | wc -l | tr -d ' ')"
if [[ "$COUNT" -gt 1 ]]; then
    echo "▸ $COUNT device terhubung, pakai yang pertama: $FIRST_DEVICE"
fi

# ---------- push ----------
FNAME="$(basename "$ARG")"
REMOTE="/sdcard/Download/$FNAME"

echo "▸ Push $FNAME ke $FIRST_DEVICE:$REMOTE"
adb -s "$FIRST_DEVICE" push "$ARG" "$REMOTE" >/dev/null

# ---------- ringkasan isi .rkx ----------
echo ""
echo "──────────────────────────────────────────────────"
echo "✔ Ter-push ke device"
echo "  $REMOTE"
if command -v unzip >/dev/null 2>&1; then
    echo ""
    echo "  Isi .rkx:"
    unzip -l "$ARG" | sed -n '4,$p' | head -n -2 | sed 's/^/    /'
fi
echo "──────────────────────────────────────────────────"
echo ""
echo "Langkah berikutnya di device:"
echo "  1. Buka RAKARA"
echo "  2. Tab Engine → + Install .rkx"
echo "  3. Pilih file: Download/$FNAME"
echo "  4. Tab Sumber → tap engine untuk mulai browse"
echo ""

# ---------- optional: auto-launch app ----------
if command -v adb >/dev/null 2>&1; then
    read -r -p "Launch RAKARA sekarang? [y/N] " ans
    if [[ "${ans:-N}" =~ ^[Yy]$ ]]; then
        adb -s "$FIRST_DEVICE" shell monkey -p dev.rakara.app -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
        echo "▸ RAKARA launched."
    fi
fi