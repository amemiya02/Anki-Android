# Paired Android release performance comparison

On 2026-11-20, one GitHub-hosted runner measured pre-change `53364499959a13d6e9348cd5212cc83c632af73a` and final runtime `0ca354386eea699c91a8c34d86bb46084081217d`, in that order. The x86_64 baseline APK SHA-256 was `cb6d21c9c254a8fcb9042217e66c4ca3370fb8057ead04db356b67749f16b3c9`. The signed x86_64 candidate APK SHA-256 was `9eb220b7ee1b9b08341a820452edce5e660ba58e47c6931ad39493e44db57e0f`. [Paired run and Perfetto traces](https://github.com/amemiya02/Hayaki/actions/runs/44748691056) · [Signed candidate build](https://github.com/amemiya02/Hayaki/actions/runs/44746442899)

The same Android 15 AOSP x86_64 Pixel 9 AVD ran both packages at 720 × 1616, 280 dpi and 60 Hz with four vCPUs, 4 GB RAM, SwiftShader and WebView 124.0.6367.219. Both packages used ART `speed`, airplane mode, English, Light appearance, and default font and motion scales. The benchmark built one media-free 100,000-Card fixture. Its SHA-256 `10148ad4f78ccedcb08a4054646935999bf94a2068b7b128c348f472b7a7b935` is present in all six phase reports. The baseline host-load samples ranged from 4.21 to 5.78, and the candidate samples ranged from 3.86 to 7.99. The build task ended before booting the emulator. [Raw frame data and reports](android-hosted-0ca35438-frames.tar.gz)

## Measurements

| Interaction | Samples per APK | Baseline action p50 / p95 (ms) | Candidate action p50 / p95 (ms) | Baseline render p50 / p95 / p99 (ms) | Candidate render p50 / p95 / p99 (ms) | Baseline misses | Candidate misses |
| --- | ---: | --- | --- | --- | --- | ---: | ---: |
| Cold start | 3 | 1854 / 1997 | 2003 / 2042 | 69.79 / 602.98 / 1384.53 | 53.89 / 787.12 / 1365.98 | 41 / 71 | 41 / 72 |
| Reveal | 15 | 354.47 / 842.72 | 351.81 / 737.69 | 80.79 / 135.01 / 156.47 | 76.55 / 133.58 / 146.03 | 56 / 72 | 48 / 64 |
| Easy rating | 15 | 827.45 / 1000.72 | 803.44 / 1254.81 | 109.74 / 154.00 / 174.21 | 105.98 / 157.56 / 184.94 | 334 / 390 | 345 / 392 |
| Browse swipe | 30 | 300 ms swipe | 300 ms swipe | 84.42 / 103.86 / 116.83 | 84.09 / 103.85 / 116.89 | 606 / 918 | 551 / 822 |

The AVD presents to a virtual display. Reveal and rating times report the upper endpoints of host observation intervals. The last screenshot request that still showed the old face supplies the lower endpoint. The first screenshot response with the new face supplies the upper endpoint. The intervals include ADB transfer and screenshot work. `gfxinfo` reports app-window frame completion and deadlines. Perfetto reports compositor scheduling and rendering events on their own timelines. [Replay and metric definitions](../README.md) · [Android FrameTimeline](https://perfetto.dev/docs/data-sources/frametimeline)

## Repeatability checks

Cold launch times were 1997→2042, 1731→1776 and 1854→2003 ms. The candidate was slower in each position of the baseline-first run. The largest candidate startup frame was 1365.98 ms, including 1166.67 ms between intended and delivered vsync. The largest baseline frame spent 1333.33 of its 1384.53 ms in that interval. A short candidate-first replay and independent cold-start trace are in progress. [Reverse-order run](https://github.com/amemiya02/Hayaki/actions/runs/44752886490)

The per-group median reveal bounds were 367.13→362.95, 342.88→336.73 and 354.47→354.25 ms. Rating medians were 810.90→870.72, 842.01→772.42 and 827.45→749.21 ms. The slowest candidate rating had a 373.59–1254.81 ms observation interval across three screenshots. The slowest baseline rating had a 125.49–1000.72 ms interval across two. The overlap and differing screenshot counts limit the attribution of the observed tail increase to app work.

Independent Study traces had 44 main-thread traversals per APK, lasting 1521.60 ms in total for the baseline and 1476.59 ms for the candidate. The main thread spent 1413.12 and 1386.44 ms in `postAndWait`, respectively. The largest `WebViewFunctor::drawGl` slices were 99.25 and 90.36 ms. These traces place most traversal time in RenderThread waits on both releases. Browse started with 100,000 Cards and advanced through new fixture rows in all six passes. The nearly identical Browse render p95 and p99 and the Study timing across groups provide the paired evidence for those interactions. [Paired traces](https://github.com/amemiya02/Hayaki/actions/runs/44748691056)
