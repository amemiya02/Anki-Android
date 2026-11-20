#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Regenerate Android assets with ImageMagick 7 from the pinned Hayaki-iOS artwork.
set -euo pipefail
ios=${1:?Pass a checkout of amemiya02/Hayaki-iOS}
root=$(git rev-parse --show-toplevel)
revision=$(python3 -c 'import json, sys; print(json.load(sys.stdin)["revision"])' < "$root/docs/brand/themes.json")
res="$root/AnkiDroid/src/main/res"
temp=$(mktemp -d)
trap 'rm -rf "$temp"' EXIT

icons=App/Resources/Assets.xcassets/AppIcon.appiconset
git -C "$ios" show "$revision:docs/assets/app-icon/foreground.png" > "$temp/foreground.png"
git -C "$ios" show "$revision:$icons/AppIcon-Tinted.png" > "$temp/tinted.png"
git -C "$ios" show "$revision:$icons/AppIcon.png" > "$temp/light.png"
git -C "$ios" show "$revision:$icons/AppIcon-Dark.png" > "$temp/dark.png"

# The middle 72dp of an adaptive icon's 108dp viewport is visible at rest.
# Extend edge pixels into the outer 18dp so the face stays intact during parallax.
adaptive() {
    magick "$1" -resize 288x288 -virtual-pixel edge -set option:distort:viewport 432x432-72-72 \
        -distort SRT 0 +repage -strip -define png:exclude-chunks=date,time "$2"
}
mkdir -p "$res/mipmap-xxxhdpi" "$res/mipmap-night-xxxhdpi"
adaptive "$temp/foreground.png" "$res/mipmap-xxxhdpi/ic_launcher_foreground.png"
# The lighter card is opaque; the eyes, mouth and fold are openings for wallpaper colour.
magick "$temp/foreground.png" -alpha extract -morphology EdgeIn Disk:12 "$temp/outline.png"
magick "$temp/tinted.png" -colorspace Gray -level 65%,90% "$temp/outline.png" -compose Lighten -composite \
    -alpha copy -channel RGB -evaluate set 100% +channel "$temp/mask.png"
adaptive "$temp/mask.png" "$res/mipmap-xxxhdpi/ic_launcher_monochrome.png"

masked() {
    local source=$1 shape=$2 pixels=$3 output=$4
    magick "$source" -resize 384x384 \
        \( -size 384x384 xc:none -fill white -draw "$shape" \) \
        -alpha set -compose DstIn -composite -resize "${pixels}x${pixels}" -strip -define png:exclude-chunks=date,time "$output"
}
square='roundrectangle 0,0 383,383 86,86'
circle='circle 191.5,191.5 191.5,0'
masked "$temp/light.png" "$square" 384 "$res/mipmap-xxxhdpi/hayaki_mark.png"
masked "$temp/dark.png" "$square" 384 "$res/mipmap-night-xxxhdpi/hayaki_mark.png"
for entry in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
    dest="$res/mipmap-${entry%:*}"
    pixels=${entry#*:}
    masked "$temp/light.png" "$square" "$pixels" "$dest/ic_launcher.png"
    masked "$temp/light.png" "$circle" "$pixels" "$dest/ic_launcher_round.png"
done
