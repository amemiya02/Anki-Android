# 21: JavaScript API in the study screen: reading the Card and answering

**What to build:** A Card whose Card Template uses the AnkiDroid JavaScript API to read information about the Card or to show and answer it works in the study screen, as it does in the old study screen.

**Blocked by:** 19 (Study screen: Card Frame and answering)

**Status:** ready-for-agent

- [ ] A Card Template can read what the API offers about the current Card: counts, Card and Note details, scheduling details and the time-remaining estimate.
- [ ] A Card Template can show the answer and answer Again, Hard, Good or Easy through the API.
- [ ] The older helper functions that Card Templates call to show or answer a Card work.
- [ ] For the same Card Template, the study screen and the old study screen give the same result for every call covered here.
- [ ] A Card that uses only these calls no longer shows the "not yet supported" warning.
- [ ] A rendered-screen test studies a Card whose Card Template answers itself through the API.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
