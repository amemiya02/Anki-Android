# Verify a joint release

Use the previous signed Android Play release, all candidate Play release APKs from **Emulator Tests** with `release_artifacts_only=true`, and the matching built iOS app with its embedded extensions. Record the exact revision for each build and the SHA-256 of the Android APK used for the installation walkthrough.

```sh
python3 tools/joint-release/verify.py \
  --previous "$PREVIOUS_RELEASE" --candidate "$CANDIDATE_RELEASE" \
  --build-tools "$ANDROID_HOME/build-tools/36.0.0" \
  --ios-app "$IOS_APP" --version "$RELEASE_VERSION" --ios-build "$IOS_BUILD"
```

The checker reads the APK manifests and verified signing certificates, checks each effective version code against the previous APK for the same ABI, and reads the host and extension plists in the built iOS app. Keep the JSON output with the release evidence. On iOS, open **About** from **More** or **Settings** and check the visible version against the package.

## Walk through an Android upgrade

1. Use a fresh, disposable Android emulator of one of the shipped ABIs. Install the corresponding historical signed APK with `adb -s "$SERIAL" install "$PREVIOUS_APK"`. Push `AnkiDroid/src/androidTest/assets/hayaki-release/collection.colpkg` to `/sdcard/Download/` and import it with Hayaki's file picker.
2. In **More → Settings → Appearance**, select **Follow system**, then set **Night theme** to **Black**. Set **Hide ‘Hard’ and ‘Easy’ buttons** in **Studying** and add **K** to **Show answer** in **Controls**. Open **Geography**, reveal a Card with K and rate it so the old app writes new history. Record the Decks and queue, Stats or review history, settings, and the version in **More → About**.
3. Return to Decks and stop the app. Install the candidate for the same ABI with `adb -s "$SERIAL" install -r "$CANDIDATE_APK"`. Keep the existing app data. Launch Hayaki from its launcher icon.
4. Verify the same Decks, queues, review history and reviewer preference. With the device in light and dark modes, verify that System still uses Light and Black respectively. Check that **More → About** shows `0.2.0` and that `adb -s "$SERIAL" shell dumpsys package com.ichi2.anki.redesign` reports the matching candidate version and version code. Capture these screens and keep the install result with them.

For a more precise history comparison on a root-capable disposable emulator, copy `collection.anki2` and its WAL, if present, after stopping the app at each stage. Compare Deck and Card IDs, queue state, and the rows in `revlog`. Stop the candidate before making any further ratings so the two captures cover the same study history.
