# Paired Android release performance comparison

On 2026-11-20, one GitHub-hosted runner measured pre-change `53364499959a13d6e9348cd5212cc83c632af73a` and final runtime `0ca354386eea699c91a8c34d86bb46084081217d`, in that order. The x86_64 baseline APK SHA-256 was `cb6d21c9c254a8fcb9042217e66c4ca3370fb8057ead04db356b67749f16b3c9`. The signed x86_64 candidate APK SHA-256 was `9eb220b7ee1b9b08341a820452edce5e660ba58e47c6931ad39493e44db57e0f`. [Paired run and Perfetto traces](https://github.com/amemiya02/Hayaki/actions/runs/44748691056) · [Signed candidate build](https://github.com/amemiya02/Hayaki/actions/runs/44746442899)

The same Android 15 AOSP x86_64 Pixel 9 AVD ran both packages at 720 × 1616, 280 dpi and 60 Hz with four vCPUs, 4 GB RAM, SwiftShader and WebView 124.0.6367.219. Both packages used ART `speed`, airplane mode, English, Light appearance, and default font and motion scales. The baseline host-load samples ranged from 4.21 to 5.78, and the candidate samples ranged from 3.86 to 7.99. The build task ended before booting the emulator. [Raw frame data and reports](android-hosted-0ca35438-frames.tar.gz)

All six baseline-first reports and all four candidate-first reports record the same media-free 100,000-Card fixture with SHA-256 `10148ad4f78ccedcb08a4054646935999bf94a2068b7b128c348f472b7a7b935`. The hosted generator used Python 3.14.4, SQLite 3.46.1 and zlib 1.3.1. A local regeneration with Python 3.12.11, SQLite 3.49.1 and zlib 1.2.12 reproduced the original local SHA-256 `98521cda65c48d402271722a92a45f2ffdc610a7bc4d45237922e46b7ee2db4f`. SQL `EXCEPT` comparisons in both directions returned zero differences across the schema definitions and all five tables, and the media entry was identical. Repacking the hosted SQLite payload with local zlib reproduced the hosted archive byte for byte, locating the difference in SQLite's physical file layout. [Local fixture evidence](android-5336449995.md)

## Measurements

| Interaction | Samples per APK | Baseline action p50 / p95 (ms) | Candidate action p50 / p95 (ms) | Baseline render p50 / p95 / p99 (ms) | Candidate render p50 / p95 / p99 (ms) | Baseline misses | Candidate misses |
| --- | ---: | --- | --- | --- | --- | ---: | ---: |
| Cold start | 3 | 1854 / 1997 | 2003 / 2042 | 69.79 / 602.98 / 1384.53 | 53.89 / 787.12 / 1365.98 | 41 / 71 | 41 / 72 |
| Reveal | 15 | 354.47 / 842.72 | 351.81 / 737.69 | 80.79 / 135.01 / 156.47 | 76.55 / 133.58 / 146.03 | 56 / 72 | 48 / 64 |
| Easy rating | 15 | 827.45 / 1000.72 | 803.44 / 1254.81 | 109.74 / 154.00 / 174.21 | 105.98 / 157.56 / 184.94 | 334 / 390 | 345 / 392 |
| Browse swipe | 30 | 300 ms swipe | 300 ms swipe | 84.42 / 103.86 / 116.83 | 84.09 / 103.85 / 116.89 | 606 / 918 | 551 / 822 |

The AVD presents to a virtual display. Reveal and rating times report the upper endpoints of host observation intervals. The last screenshot request that still showed the old face supplies the lower endpoint. The first screenshot response with the new face supplies the upper endpoint. The intervals include ADB transfer and screenshot work. `gfxinfo` reports app-window frame completion and deadlines. Perfetto reports compositor scheduling and rendering events on their own timelines. [Replay and metric definitions](../README.md) · [Android FrameTimeline](https://perfetto.dev/docs/data-sources/frametimeline)

## Repeatability checks

Cold launches in the baseline-first run took 1997→2042, 1731→1776 and 1854→2003 ms. Its largest candidate startup frame spent 1166.67 of 1365.98 ms between intended and delivered vsync. The largest baseline frame spent 1333.33 of 1384.53 ms in that interval.

The per-group median reveal bounds were 367.13→362.95, 342.88→336.73 and 354.47→354.25 ms. Rating medians were 810.90→870.72, 842.01→772.42 and 827.45→749.21 ms. The slowest candidate rating had a 373.59–1254.81 ms observation interval across three screenshots. The slowest baseline rating had a 125.49–1000.72 ms interval across two. The overlap and differing screenshot counts limit the attribution of the observed tail increase to app work.

Browse started with 100,000 Cards and advanced through new fixture rows in all six passes. Its render p95 and p99 differed by less than 0.1 ms between APKs.

### Candidate-first replay

A second hosted runner measured the exact same APKs and fixture, running the candidate first. Host load for cold start was 3.61–4.27 for the baseline and 4.02–4.88 for the candidate. During Study it was 2.58–4.82 for the baseline and 5.19–5.85 for the candidate. [Candidate-first run and Perfetto traces](https://github.com/amemiya02/Hayaki/actions/runs/44752886490) · [Raw frame data, reports and fixture manifest](android-hosted-0ca35438-reverse-frames.tar.gz)

| Interaction | Samples per APK | Baseline action p50 / p95 (ms) | Candidate action p50 / p95 (ms) | Baseline render p50 / p95 / p99 (ms) | Candidate render p50 / p95 / p99 (ms) | Baseline misses | Candidate misses |
| --- | ---: | --- | --- | --- | --- | ---: | ---: |
| Cold start | 3 | 1938 / 2060 | 1788 / 1799 | 53.09 / 652.07 / 1136.01 | 56.26 / 686.51 / 1669.56 | 33 / 65 | 43 / 78 |
| Reveal | 10 | 316.50 / 622.14 | 331.26 / 629.26 | 61.03 / 122.40 / 135.64 | 70.13 / 136.44 / 168.48 | 38 / 47 | 45 / 56 |
| Easy rating | 10 | 682.68 / 778.90 | 720.28 / 789.85 | 89.91 / 150.46 / 167.50 | 96.81 / 151.19 / 169.89 | 202 / 270 | 212 / 269 |

Cold launches were 1805→1602, 1938→1788 and 2060→1799 ms, reversing the direction in all three positions. The largest startup frames again spent most of their duration between intended and delivered vsync, 1100.00 of 1136.01 ms for the baseline and 1550.00 of 1669.56 ms for the candidate. The rating median changed direction between runs. The rating p95 observation bound increased by 10.95 ms, versus 254.09 ms in the baseline-first run. The candidate's median reveal bound was 578.15 ms in the first Study group and 318.56 ms in the second, while the baseline medians were 316.50 and 326.08 ms.

### Independent traces

With Perfetto enabled, two additional cold launches took 1577 and 1555 ms for the baseline and 1927 and 1534 ms for the candidate, according to Android `TotalTime`. The slower candidate launch had a 358.03 ms `bindApplication` slice and spent 257.36 ms with the main thread runnable. Those timings dropped to 94.07 and 53.26 ms on the next candidate launch. The baseline pairs were 91.63 and 190.64 ms for `bindApplication` and 74.00 and 96.61 ms runnable. The largest main-thread traversal in each traced launch spanned roughly 1.04–1.17 seconds. The candidate spent longer in the startup `postAndWait` slice in both traced launches, at 64.87 and 53.98 ms compared with 14.35 and 11.14 ms. Its overall startup took longer in the first comparison and less time in the second. [Android startup trace analysis](https://perfetto.dev/docs/analysis/stdlib-docs)

The independent Study traces place most main-thread traversal time in `postAndWait` on both releases. The baseline-first traces contain 44 traversals per APK, totaling 1521.60→1476.59 ms, with `postAndWait` accounting for 1413.12→1386.44 ms. The reverse traces contain 63 baseline and 60 candidate traversals totaling 1839.61→1847.87 ms, of which 1794.70→1801.63 ms was `postAndWait`. Maximum `WebViewFunctor::drawGl` slices went from 99.25 to 90.36 ms and from 55.66 to 61.60 ms in the two runs, respectively. The launch-order reversal, overlapping screenshot intervals and similar Study trace totals are consistent with run-to-run variation on these emulators. [Baseline-first traces](https://github.com/amemiya02/Hayaki/actions/runs/44748691056) · [Candidate-first traces](https://github.com/amemiya02/Hayaki/actions/runs/44752886490)
