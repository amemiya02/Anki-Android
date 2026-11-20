# Local candidate performance replay

The signed preview release `fb65dea3ecd23407aab005f9c8a7e7a317bb509f` ran on the same Android 15 AOSP Pixel 9 emulator as the recorded `5336449995` baseline. It imported a fresh copy of the same 100,000-Card fixture. Both Play release APKs were R8-optimized and compiled with ART `speed`. The baseline used the `.perf` package suffix to keep the two installed collections separate. The preview ARM64 APK SHA-256 was `132204962388e62cc3cb0bbb085042e2ce31467891566c5181b09346cbbe5e67`. [Package build](https://github.com/amemiya02/Hayaki/actions/runs/44738659742) · [Local baseline](android-5336449995.md)

## Local observations

| Interaction | Samples per build | Baseline action p95 (ms) | Preview action p95 (ms) | Baseline render p95 (ms) | Preview render p95 (ms) | Baseline missed deadlines | Preview missed deadlines |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Cold start | 3 | 1928 | 6515 | 185.98 | 302.56 | 28 / 88 | 35 / 59 |
| Reveal | 5 | 459.03 | 3067.18 | 84.17 | 919.03 | 11 / 51 | 13 / 16 |
| Easy rating | 5 | 916.86 | 2955.22 | 56.04 | 231.28 | 23 / 164 | 45 / 136 |
| Browse swipe | 10 | 300 ms gesture | 300 ms gesture | 47.31 | 84.41 | 42 / 574 | 72 / 404 |

Reveal and rating action times are host-to-visible upper bounds that include ADB and screenshots. The preview cold launches were 3282, 6515 and 3253 ms. Host one-minute loads were 170.69–185.77 during preview launches and 71.60–79.22 during baseline launches. The preview Study and Browse runs sampled 61.45–68.89 and 81.36–89.73, respectively. The preview Browse list began at row 00000 and advanced to rows 00209–00223 after ten warm-up and ten measured swipes. Its first attempt opened on the empty Default Deck. The replay clears this filter before measuring. [Machine-readable measurements](android-fb65dea3e.json) · [Raw Android captures](android-fb65dea3e-raw.tar.gz)

## Diagnosing the local outliers

- The slowest preview reveal frame took 919.03 ms. Of this, 241.73 ms elapsed between RenderThread sync and `SwapBuffers`, 575.15 ms between `SwapBuffers` and `FrameCompleted`, and 2.07 ms between `HandleInputStart` and `DrawStart`. The preview p95 final screenshot round trip also rose to 791.54 ms from 379.10 ms.
- The slowest preview rating frame took 673.71 ms. `Vsync` arrived 450 ms after `IntendedVsync`, and another 179.63 ms elapsed from `SwapBuffers` to `FrameCompleted`. Final screenshot round-trip p95 was 1495.04 ms for the preview and 400.93 ms for the baseline.
- The slowest preview Browse frame took 304.02 ms. The `IntendedVsync` to `Vsync` interval accounted for 216.67 ms.

A separate diagnostic pass recorded two reveal-and-rating pairs with Perfetto per APK. The baseline's longest `WebViewFunctor::drawGl` slice was 975.65 ms. The preview's was 201.76 ms. Its RenderThread was Running for 164.91 ms, Runnable for 32.86 ms, and sleeping for 3.99 ms within that slice. The baseline and preview had maximum main-thread `postAndWait` slices of 850.18 ms and 217.94 ms, respectively. Those waits overlapped draw work on the RenderThread. The reversal across the local passes shows how unstable the observed ordering was. The frame phases and screenshot costs identify where individual outliers spent time. Attribution to app code requires a repeatable paired difference under comparable conditions. [Perfetto Android tracing](https://perfetto.dev/docs/learning-more/android) · [Android FrameTimeline](https://perfetto.dev/docs/data-sources/frametimeline)
