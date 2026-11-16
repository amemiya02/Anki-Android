# 22: JavaScript API in the study screen: Card actions, speech and the rest

**What to build:** Every remaining part of the AnkiDroid JavaScript API works in the study screen, so that no Card needs the old study screen any more.

**Blocked by:** 21 (JavaScript API in the study screen: reading the Card and answering)

**Status:** ready-for-agent

- [ ] A Card Template can perform every Card action the API offers: mark, flag, bury, suspend, reset progress, set a due date, search, and the rest.
- [ ] A Card Template can use the API's speech, speech recognition, messages and screen controls.
- [ ] Every call the old study screen supports is supported, checked against a list of the whole API.
- [ ] The "not yet supported" warning can no longer appear, and it is removed.
- [ ] The tests that cover the API on the old study screen pass against the study screen.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
