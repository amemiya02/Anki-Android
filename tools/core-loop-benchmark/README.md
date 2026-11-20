# Hayaki Android Core Loop benchmark

The baseline revision is `53364499959a13d6e9348cd5212cc83c632af73a`. Capture it and a candidate on the same device with the same release variant, compilation mode, app and device settings, fixture, and run count. The [earlier Browser check](../../.scratch/ankidroid-redesign/issues/26-card-browser-list-search-and-sort.md#controlled-100000-card-scroll-benchmark) used an Android 15 emulator and a different build and collection. It informs the swipe procedure.

## Prepare a run

1. Build the source revision under test with the release variant and normal R8 optimizations. Use the device's ABI APK and the disposable `.perf` application ID. Run from the revision's checkout with your Android SDK and the repository's Gradle JDK configured.

   ```sh
   ./gradlew :AnkiDroid:packagePlayRelease -PcustomSuffix=.perf -PcustomName=Hayaki -x :AnkiDroid:lintVitalPlayRelease
   adb -s "$SERIAL" install AnkiDroid/build/outputs/apk/play/release/AnkiDroid-play-arm64-v8a-release.apk
   ```

2. Use a dedicated device or a disposable emulator instance. Configure portrait, English, default font scale, Light mode, offline, and no automatic sync. Match the display size, density, refresh rate, motion settings and WebView version between captures. On an emulator, also match the image, emulator version, renderer, assigned cores and RAM. Keep other builds and emulators idle during measurements. Use the same thermal and power conditions for physical-device runs.
3. Generate the fixture from the benchmark checkout with `uv`. It is one Deck named `Browser fixture` with 100,000 new Cards in due order, one Basic Note Type and no media. Its two Card faces have distinct solid color markers for observing the WebView on screen. Fixed IDs and timestamps let you verify the archive hash.

   ```sh
   uv run --with genanki==0.13.1 python tools/core-loop-benchmark/make_fixture.py /tmp/Browser-fixture.apkg
   adb -s "$SERIAL" push /tmp/Browser-fixture.apkg /sdcard/Download/Browser-fixture.apkg
   ```

4. Start the `.perf` app with an empty collection. Complete onboarding with **Start with no Decks**. Import `Browser-fixture.apkg` with the Android system file picker and return to Decks. If the backup reminder appears, select **Later** before starting the capture. Keep the default study limits, enable the rating buttons, and clear any Browser filter. Before each complete capture, recreate the disposable app's empty collection and reimport the fixture. The benchmark grades up to 18 Cards.
5. Use the benchmark checkout for the capture, regardless of which APK revision is on the device. Enter the installed APK's full revision and describe the host activity, configuration and renderer used for the run. The capture requests ART `speed` compilation before the unmeasured warm-up. Use `--journey cold_start`, `--journey study`, or `--journey browse` to repeat a single journey. To use an already packaged release, select its package with `--package` and install it on a disposable device with only the fixture imported.

   ```sh
   python3 tools/core-loop-benchmark/run.py capture \
     --serial "$SERIAL" --revision "$APK_REVISION" \
     --fixture /tmp/Browser-fixture.apkg --runs 3 \
     --conditions "$RUN_CONDITIONS" --output "$REPORT_DIRECTORY"
   ```

## Read the report

- **Cold start.** Force-stop, then launch the launcher Activity. Take Android's `am start -W` `TotalTime` for a successful `COLD` launch and verify the fixture Deck appears. Android process state is cold. The OS filesystem cache and compiled code are warm.
- **Reveal and consecutive grading.** Enter the fixture Deck, perform one unmeasured reveal and Easy grading pair, then measure the next five pairs. Use a button tap for each action. The device stays in the study screen until the run ends. The timing window starts when the host dispatches `adb shell input tap`. It ends when the first screenshot showing only the expected Card face arrives. The start of the last screenshot without the expected marker bounds the observation from below. Each sample includes both bounds, the capture count, and the final screenshot's round-trip time. The device compresses raw screenshot pixels before ADB transfer. The interval includes transport and screenshot overhead, so compare same-device distributions and inspect the individual bounds before attributing a small difference to the app.
- **Browse.** Reopen the launcher, enter Browse in Cards mode and verify 100,000 results. At the middle of the screen's width, swipe from 76% to 33% of the screen height in 300 ms. Warm up with ten swipes and collect frame data for each of the next ten. Each swipe has 700 ms of settling time. The report verifies movement through distinct fixture rows across both passes.
- **Frame distributions.** Retain raw `dumpsys gfxinfo … framestats` per action. Use newly observed, unflagged, completed app-window frames. Render time is `FrameCompleted − IntendedVsync`. A deadline miss has `FrameCompleted > FrameDeadline`. The overrun histogram expresses the positive amount by which a frame exceeded its deadline, rounded up to full display refresh intervals. Zero is on time. These are app frame deadlines as reported by Android. `gfxinfo` does not sample every refresh or report all compositor drops. Read the samples and their filenames to trace an outlier to its launch, reveal, grade, or swipe.
- **Comparisons.** Retain each report and its raw files. Compare the pooled p50, p95 and p99 and the missed-deadline histograms for the same action, then check the per-action samples and conditions for outliers. Rerun a repeatable slowdown on the same device under comparable load before attributing it to the candidate. Emulator data is useful for like-for-like replay. Use a representative physical device for absolute device performance claims.

To inspect one frame capture with the cutoff from `report.json`:

```sh
python3 tools/core-loop-benchmark/run.py frames CAPTURE.gfxinfo.txt \
  --after LAST_INTENDED_VSYNC_NS --refresh-rate 60
```

The existing [Macrobenchmark module](../../baselineprofile/README.md) provides an additional cold-start check for the shipped baseline profile on physical hardware. This procedure uses ART speed mode for both builds and a common interaction capture for all four journeys.

## Verify the tooling

```sh
python3 tools/core-loop-benchmark/test_report.py
uv run --with genanki==0.13.1 python tools/core-loop-benchmark/test_fixture.py
```
