# 21: JavaScript API in the study screen: reading the Card and answering

**What to build:** A Card whose Card Template uses the AnkiDroid JavaScript API to read information about the Card or to show and answer it works in the study screen, as it does in the old study screen.

**Blocked by:** 19 (Study screen: Card Frame and answering)

**Status:** done

- [x] A Card Template can read what the API offers about the current Card: counts, Card and Note details, scheduling details and the time-remaining estimate.
- [x] A Card Template can show the answer and answer Again, Hard, Good or Easy through the API.
- [x] The older helper functions that Card Templates call to show or answer a Card work.
- [x] For the same Card Template, the study screen and the old study screen give the same result for every call covered here.
- [x] A Card that uses only these calls no longer shows the "not yet supported" warning.
- [x] A rendered-screen test studies a Card whose Card Template answers itself through the API.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

- The study screen uses the shared `AnkiDroidJS` script through the origin-scoped WebView POST bridge. It accepts the Card-reading and answering methods. The older `showAnswer()` and `buttonAnswerEase1()` through `buttonAnswerEase4()` helpers use the same bridge.
- The API reports an ETA of zero with the old screen's Show ETA preference off. The interval methods return the current Card's intervals on its answer side when the collection shows answer intervals. On the question side they return empty labels.
- On the Pixel 9 emulator, `ReviewerFragmentTest` compared all 27 read methods on both sides of a Card in both study screens and checked for the unsupported-feature dialog. Two other tests recorded Easy through the versioned API and Hard through the older helpers. The API test awaits the flip while the custom scheduler prepares the answer, then reads the answer side and grades the Card. Each failed before the corresponding implementation and passed with it.
- `ReviewerViewModelTest`, `ReviewerTest` and `AnkiDroidJsAPITest` finished with 42 passed and 2 skipped. `:libanki:test` finished with 143 passed and 7 skipped. The custom-scheduler instrumentation helper advances the Compose clock through the Card Frame's double-tap interval before pressing Good, so its existing assertions test scheduling after an accepted grade.
