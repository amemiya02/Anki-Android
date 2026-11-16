# AnkiDroid Redesign

Status: ready-for-agent

This spec uses the vocabulary in `GLOSSARY.md` and respects ADR-0001 (own fork, no upstreaming), ADR-0003 (the interface is rebuilt natively in Compose) and ADR-0004 (one study screen: port, then remove the old one).

## Problem Statement

I study with AnkiDroid every day, in Chinese, Japanese and English, and the app is ugly.

- The deck list is a bright blue bar over a bare white list. The three counts that tell me what is due are tiny coloured numbers pushed to the far edge.
- The study screen I use by default shows three unrelated colours at once: a blue bar, a pale blue strip for the counts, and a slate bar for "Show answer".
- Chinese and Japanese text in the interface is drawn in a heavy system font that matches nothing else on the screen.
- When I finish a Deck, I get two lines of text at the top of an empty white page.
- The places I go most (Decks, the card browser, statistics, settings) are hidden in a side drawer.
- Dialogs, settings and secondary screens each look like they belong to a different app, because they were built at different times.
- There are five themes, and none of them looks designed.

Upstream cannot fix this for me. It accepts only small single-purpose changes, it has no design direction of its own, and its policy rules out work built with AI assistance from a newcomer.

## Solution

A Fork of AnkiDroid, usable by me and by other people, whose whole interface is redesigned: a Reskin, a Restructure and a Reflow, together.

- Every Native Surface is rebuilt in one coherent look that I choose by eye from three Directions.
- The app has its own identity: a fixed palette, typefaces chosen to look good in Chinese, Japanese and English, one icon family, and three designed themes (Light, Dark and Black).
- The places I go most are one tap away in a bottom bar.
- The Core Loop (choose a Deck, study its Cards, reach the finished screen) is calm and fast, and ends on a finished screen that was actually designed.
- Nothing I rely on disappears. Every existing feature stays reachable, my collection stays compatible with desktop Anki, and scheduling behaves exactly as before.
- The Fork has its own name, icon and app ID, so it installs beside the Official App and I can keep both.
- The Fork does not sync with AnkiWeb, whose terms allow only named clients. My Decks reach it through a self-hosted sync server or by export and import.

The work lands in Milestones, and the app stays usable after each one.

## User Stories

### Choosing the look

1. As the Fork's owner, I want to see three Directions as Mockups before any app code is written, so that I pick the look by eye rather than from a description.
2. As the Fork's owner, I want each Direction shown on both the deck list and the study screen, so that a Direction that only flatters the deck list cannot win.
3. As the Fork's owner, I want every Mockup shown in English and Simplified Chinese side by side, so that I can see the font pairing and layout hold up in both scripts.
4. As the Fork's owner, I want each Direction to show its own font pairing, so that the typeface is part of what I am choosing.
5. As the Fork's owner, I want the Dark and Black themes drawn only for the Direction I pick, so that effort is not spent on looks I rejected.
6. As the Fork's owner, I want the Fork's name and launcher icon proposed right after I pick the Direction, so that they fit the look.
7. As the Fork's owner, I want a Mockup agreed for each major screen before it is built, so that I am never surprised by what gets built.

### Foundation

8. As a Daily Reviewer, I want the chosen colours, type, spacing and shapes applied across the whole app at once, so that even screens not yet rebuilt stop looking like the old app.
9. As a Daily Reviewer, I want a fixed palette that does not change with my wallpaper, so that the app looks the same every day.
10. As a Daily Reviewer, I want a Light theme, so that I can study in daylight.
11. As a Daily Reviewer, I want a Dark theme, so that I can study at night without glare.
12. As a Daily Reviewer with an OLED screen, I want a Black theme with true black backgrounds, so that the screen stays dark and saves power.
13. As a Daily Reviewer, I want the app to follow my system's light or dark setting by default, so that I do not have to switch it by hand.
14. As a Daily Reviewer, I want every icon to come from one family in one style, so that the app reads as a single product.
15. As a Chinese-reading user, I want interface text in a bundled Chinese typeface chosen for this app, so that it does not fall back to a heavy system font.
16. As a Japanese-reading user, I want interface text in a bundled Japanese typeface, so that Japanese glyph shapes are correct and match the rest of the interface.
17. As an English-reading user, I want a Latin typeface that sits well beside the Chinese and Japanese ones, so that mixed-language screens look intentional.
18. As a user of any other language, I want text to fall back to the system font cleanly, so that nothing is missing or broken.
19. As a reader of Chinese or Japanese, I want the type hierarchy carried by size and weight only, so that emphasis survives in scripts with no italics or capitals.
20. As a Daily Reviewer, I want no leftover old colours in icons, graphics or screens that were not rebuilt yet, so that the app never shows two looks at once.

### Navigation

21. As a Daily Reviewer on a phone, I want a bottom bar with Decks, Browse, Stats and More, so that the places I use most are one tap away and always visible.
22. As a Daily Reviewer, I want settings, help and support under More, so that they are reachable without crowding the bar.
23. As a Daily Reviewer, I want a prominent button for adding a Note on Decks and on Browse, so that adding is never more than one tap from where I am.
24. As a Daily Reviewer, I want sync in the top bar of Decks, so that I can start it from where I begin each session.
25. As a Daily Reviewer, I want the system back gesture to behave predictably between screens, so that I never leave the app by accident.
26. As a Daily Reviewer, I want screen-to-screen transitions to be quick and consistent, so that moving around feels like one app.
27. As a tablet user, I want the existing drawer and two-pane layouts to keep working in the new look, so that the tablet is not left behind while the phone is redesigned.

### Core Loop: deck list

28. As a Daily Reviewer, I want to see at a glance which Decks have Cards to study today, so that I can decide where to start.
29. As a Daily Reviewer, I want the new, learning and due counts for each Deck to be easy to read, so that I do not squint at tiny numbers.
30. As a Daily Reviewer, I want sub-Decks to expand and collapse, so that a deep Deck tree stays manageable.
31. As a Daily Reviewer, I want to start studying a Deck with one tap, so that the Core Loop begins immediately.
32. As a Daily Reviewer, I want a total of what is due today, so that I know the size of the session.
33. As a Daily Reviewer, I want today's study summary shown where I can see it, so that I know what I have already done.
34. As a Daily Reviewer, I want every Deck action that exists today (rename, options, export, delete, custom study, create sub-Deck, and the rest) to stay reachable, so that the Redesign costs me nothing.
35. As a Daily Reviewer, I want to create a Deck, a filtered Deck, or get shared Decks from the deck list, so that those paths remain where I expect them.
36. As a Daily Reviewer with long Deck names in Chinese or Japanese, I want names to wrap or truncate gracefully, so that the counts stay visible.
37. As a Newcomer with no Decks, I want the empty deck list to tell me clearly what to do first, so that I am not staring at an empty screen.

### Core Loop: study screen

38. As a Daily Reviewer, I want the Card Content to be the visual focus of the study screen, so that the Card Frame does not compete with what I am learning.
39. As a Daily Reviewer, I want Card Content rendered exactly as its Card Template styles it, so that my existing Decks look the way their authors intended.
40. As a Daily Reviewer, I want the answer buttons (Again, Hard, Good, Easy) to be large, distinct and reachable with one thumb, so that I can answer without looking.
41. As a Daily Reviewer, I want each answer button to show when the Card will come back, so that I can judge my answer.
42. As a Daily Reviewer, I want the remaining counts visible but quiet, so that I know my progress without being distracted.
43. As a Daily Reviewer, I want undo within easy reach, so that a mis-tap costs nothing.
44. As a Daily Reviewer, I want flag, mark, edit, Card info, bury, suspend, delete, reschedule and the other Card actions available from a menu, so that no action is lost.
45. As a Daily Reviewer, I want my gestures, tap zones, hardware keys and controller bindings to keep working, so that my habits carry over.
46. As a Daily Reviewer, I want type-in-the-answer, the whiteboard, auto-advance, the timer, text-to-speech tags, voice recording and timeboxing to keep working, so that the study screen is a full replacement for daily use.
47. As a Daily Reviewer, I want the redesigned study screen to be the default, so that I get it without changing a setting.
48. As a person whose Cards use the AnkiDroid JavaScript API, I want those Cards to work in the study screen, so that shared Decks with custom buttons and behaviour keep working.
49. As a Daily Reviewer, I want there to be one study screen, so that no old-look screen is left in the app and settings never refer to a screen I do not use.
50. As a Daily Reviewer, I want each feature only the old study screen had to be either built into the study screen or dropped on purpose, so that nothing disappears by accident.

### Core Loop: finished screen

51. As a Daily Reviewer, I want a designed finished screen when a Deck has nothing left today, so that the session ends on something better than two lines on an empty page.
52. As a Daily Reviewer, I want the finished screen to tell me when more Cards become available, so that I know when to come back.
53. As a Daily Reviewer, I want the finished screen to tell me if a daily limit is why I am finished, so that I can decide whether to study more.
54. As a Daily Reviewer, I want to unbury Cards from the finished screen when some are buried, so that I can continue if I choose.
55. As a Daily Reviewer, I want custom study and Deck options reachable from the finished screen, so that everything the current page offers is still there.
56. As a Daily Reviewer, I want one tap from the finished screen back to my Decks, so that the Core Loop closes.

### Finding and adding

57. As a Daily Reviewer, I want the card browser to stay fast with a very large collection, so that searching 100,000 Cards does not lag.
58. As a Daily Reviewer, I want to search, sort, filter and choose columns in the card browser, so that I can find any Card.
59. As a Daily Reviewer, I want to select many Cards and act on them together, so that bulk edits stay possible.
60. As a Daily Reviewer, I want to preview a Card from the card browser, so that I can check it without studying it.
61. As a Daily Reviewer, I want the note editor to make the fields the focus, so that adding a Note is quick.
62. As a Daily Reviewer, I want to choose the Deck and the Note Type while adding, so that the Note lands in the right place.
63. As a Daily Reviewer, I want the formatting toolbar, tags, attachments, audio recording and image tools to keep working in the note editor, so that rich Notes are still possible.
64. As a Daily Reviewer, I want to add a Note from text shared by another app, so that capturing from a browser or dictionary still works.
65. As a Daily Reviewer, I want unsaved changes protected when I leave the note editor, so that I do not lose work.

### Settings and dialogs

66. As a Daily Reviewer, I want settings grouped so that I can find a setting without searching, so that configuration is not a maze.
67. As a Daily Reviewer, I want to search settings, so that I can jump straight to one I know by name.
68. As a Daily Reviewer, I want every existing setting to remain, so that no behaviour I configured is taken away.
69. As a Daily Reviewer, I want settings that only the old study screen used to be gone, so that every setting I see has an effect.
70. As a Daily Reviewer, I want theme selection to offer Light, Dark and Black, so that the choice matches what the Fork actually designs.
71. As a Daily Reviewer, I want every dialog (confirmation, progress, error, picker, input) to share one look, so that no dialog feels like a different app.
72. As a Daily Reviewer, I want destructive actions to ask for confirmation in a consistent way, so that I always recognise a dangerous choice.

### The rest

73. As a Daily Reviewer, I want managing Note Types, their fields and their Card Templates in the new look, so that the advanced screens are part of the same app.
74. As a Daily Reviewer, I want to edit a Card Template and preview the result, so that I can design my own Cards.
75. As a Daily Reviewer, I want to point the Fork at my own sync server, so that my devices sync without AnkiWeb.
76. As a Daily Reviewer, I want to bring Decks in and out by export and import, so that I can move data without any server.
77. As a Daily Reviewer, I want to browse and download shared Decks, so that I can still find community material.
78. As a Newcomer, I want a first-run screen that offers a clear way to begin, so that I am not asked to sign in to something the Fork does not use.
79. As a Daily Reviewer, I want home-screen widgets in the new look, so that the redesign extends to my launcher.
80. As a Daily Reviewer, I want review reminders and other notifications in the new look and wording, so that they match the app.
81. As a Daily Reviewer, I want statistics, Deck options, Card info and the importers to match the app's colours and typeface, so that opening an Anki Page does not feel like leaving the app.
82. As a Daily Reviewer, I want the Anki Pages to keep their content and arrangement, so that they stay identical in function to desktop Anki.

### Accessibility, languages and devices

83. As a low-vision user, I want all text to meet WCAG AA contrast in all three themes, so that I can read every screen.
84. As a user with a motor impairment, I want every control to offer a touch target of at least 48dp, so that I can hit it reliably.
85. As a user who sets the system font to 200%, I want every screen to remain usable, so that large text does not hide controls or clip content.
86. As a right-to-left reader, I want every screen mirrored correctly, so that the layout follows my reading direction.
87. As a screen-reader user, I want every control labelled, so that I can use the app without seeing it.
88. As a Chinese-reading or Japanese-reading user, I want new interface text translated into my language, so that no English leaks into redesigned screens.
89. As a foldable or tablet user, I want layouts that keep working at my screen size, so that the app does not stretch a phone layout across a large screen.

### Safety, identity and continuity

90. As a Daily Reviewer, I want scheduling to behave exactly as in the Official App, so that my reviews are not disturbed.
91. As a Daily Reviewer, I want my collection to stay compatible with desktop Anki, so that I can move it between apps at any time.
92. As a person who keeps the Official App installed, I want the Fork to install beside it with its own data, so that I can fall back at any time.
93. As a person who keeps the Official App installed, I want the Fork to have a different name and icon, so that I never open the wrong one.
94. As the Fork's owner, I want the Fork to send no crash reports or usage statistics to Upstream, so that my unfinished builds do not pollute the maintainers' data.
95. As the Fork's owner, I want the Fork to stay usable after every Milestone, so that I can use it for real long before the whole Redesign is finished.
96. As the Fork's owner, I want the Engine kept up to date from Upstream, so that the Fork keeps working with newer desktop Anki and with my sync server.
97. As the Fork's owner, I want nothing ever opened against Upstream, so that the Fork respects Upstream's contribution rules.
98. As the Fork's owner, I want the source public in my fork, so that the licence is honoured if I ever share a build.
99. As an agent implementing a ticket, I want each Native Surface to have an agreed Mockup and a mechanical Quality Bar, so that I know when the work is done.
100. As an agent implementing a ticket, I want to use the glossary's words in code, tests and tickets, so that the project speaks one language.

### Moving data in and out

101. As a person using the Fork, I want to export my whole collection with scheduling, review history and media, so that I can import it into the Official App and sync it to AnkiWeb from there.
102. As a person using the Fork, I want to export a single Deck with its scheduling and media, so that I can merge it into the Official App without replacing what is already there.
103. As a person using the Fork, I want to import a collection or a Deck exported from the Official App or desktop Anki, so that I can bring my existing study history with me.
104. As a person whose Official App is older than the Fork's Engine, I want an export that older versions can read, so that my data is never stranded in the Fork.
105. As a person exporting, I want the export screen to say whether the result will replace or merge with what is in the other app, so that I do not overwrite my AnkiWeb data by surprise.
106. As a person using the Fork, I want export to stay easy to find however the interface is rearranged, so that leaving the Fork is always possible.
107. As a person arriving from AnkiWeb, I want the Fork to explain how to bring my Decks in through the Official App or desktop Anki, so that I am not stuck when I find there is no AnkiWeb sign-in.
108. As a person arriving from AnkiDroid, I want to import on the first-run screen, so that my first minute in the Fork starts with my own Decks.
109. As a person with a package file on my device, I want the Fork offered when I open it, so that importing does not require finding a menu.

### Other people using the Fork

110. As a person who is not the Fork's owner, I want the app to have its own name and icon, so that I know which app I am using and do not confuse it with AnkiDroid.
111. As a person who is not the Fork's owner, I want the about screen to say this is an unofficial fork of AnkiDroid and to link to its source, so that I know what I installed and where it came from.
112. As a maintainer of AnkiDroid, I want the Fork to be clearly not AnkiDroid, so that its users do not bring its problems to us.

## Implementation Decisions

### Fork and identity

- The work lives in a public personal fork on the `redesign` branch. `main` mirrors Upstream. Nothing is ever opened against Upstream (ADR-0001).
- The Fork gets its own name, launcher icon and app ID, and installs beside the Official App with separate data. Until the Direction is chosen it builds under the working name "AnkiDroid Redesign" with an app ID suffix. The final name and icon are chosen immediately after the Direction.
- The build already supports a custom name and app ID suffix for a parallel install; the Fork makes its own identity the default for every build type rather than an opt-in.
- The public API module hard-codes the Official App's package name in its authority and permission. The Fork changes these so that the two apps do not collide. Other apps that integrate with the Official App's API will not see the Fork; this is accepted.
- Other people may use the Fork, not only its owner. How they get it (a store release, signing for distribution, support) is deferred and is not decided here.
- Because other people will use it, a real name and launcher icon are required, not optional:
  - The name does not contain "AnkiDroid" and cannot be mistaken for the Official App or for a product of Anki's makers.
  - The launcher icon does not reuse the Official App's or Anki's logo.
  - The working name "AnkiDroid Redesign" is for development builds only and never reaches another person.
- The about screen says the app is an unofficial fork of AnkiDroid, credits Upstream, states the licence, and links to the Fork's public source. The licence requires source to be offered with any build given to someone else.

### Engine boundary

- The Engine (collection storage, scheduling, sync and the Anki backend) stays Upstream's code and is not modified.
- The interface no longer follows Upstream. Engine updates are taken from Upstream whenever sync requires it, or a few times a year. Each Engine update may need interface code adjusted by hand.
- Collection format, sync compatibility and scheduling behaviour stay identical to Upstream. This is the hard limit on every other decision in this spec.
- History stays linear, except for merges that bring Engine updates in.

### Stack and structure

- Every Native Surface is rebuilt in Jetpack Compose with Material 3 components, themed to the chosen Direction. A custom component is written only where a Material 3 component cannot reach the Mockup.
- The rebuild happens in place, one Milestone at a time. A rebuilt screen replaces its old version; screens not yet rebuilt keep working beside it. There is no parallel new app.
- The logic behind each screen is kept. A rebuilt screen is a Compose function of one state object plus event callbacks, fed by that screen's existing logic holder. Where a screen has no separate logic holder today, the rebuild extracts one so that the screen's state can be rendered and tested without the screen's host.
- The app moves gradually to a single activity with Compose navigation, adopting it screen by screen as each is rebuilt. No dependency-injection framework is introduced.
- Home-screen widgets are rebuilt with Glance, since ordinary Compose cannot draw them.
- No existing feature is removed. Defaults may change.
- Non-UI improvements (speed, bugs, defaults, small features) are taken only when found in a surface already being rebuilt. They are never sought out.

### Foundation

- The Foundation is one set of design values for colour, type, spacing, shape and elevation, defined once and read by both Compose and the remaining XML screens, so that a half-migrated app still shows one look.
- Colours are a fixed palette with semantic roles. Wallpaper-derived dynamic colour is not used.
- There are three themes: Light, Dark and Black. The Plain and E-ink themes are removed, along with their settings entries. The app follows the system's light or dark setting by default; Black is an explicit choice for the night side.
- Hard-coded colours and text sizes do not pick up theme values on their own. The Foundation Milestone includes sweeping them: roughly 430 colours fixed in icons and graphics, roughly 90 in layouts, and roughly 55 fixed text sizes.
- All interface icons come from Material Symbols in one style and weight chosen with the Direction. Only the launcher icon is drawn specially.

### Typography

- Full typefaces are bundled for Chinese (Simplified), Japanese and Latin script. Other scripts fall back to the system font.
- Only openly licensed fonts (SIL Open Font License) are used, because the Fork's source is public.
- The Japanese and Chinese typefaces are selected by the interface language, so that Japanese text gets Japanese glyph forms and Chinese text gets Chinese ones.
- The hierarchy uses size and weight only. Italics, capitals and letter-spacing carry no meaning anywhere in the interface.
- Line height and spacing are tuned for Chinese and Japanese text, not only for Latin.
- The added app size (roughly 20–40 MB) is accepted.
- Card Content keeps whatever font its Card Template sets. The bundled typefaces apply to the Card Frame and the rest of the interface only.

### Languages

- English, Simplified Chinese and Japanese are first-class: every Mockup and every rebuilt surface is checked in them. Other major languages are checked after, and one right-to-left language is checked on every surface.
- Existing translated strings are reused wherever the wording still fits.
- Upstream's translations come from its own translation platform, which the Fork does not use. New or changed interface text is written by the Fork in English, Simplified Chinese and Japanese; other languages fall back to English for those strings until someone translates them.
- Anki's domain words are used verbatim in every language (Note, Card, Deck, Note Type, Card Template and their established translations).

### Navigation

- On phones, a bottom bar replaces the side drawer as primary navigation, with four destinations: Decks, Browse, Stats and More.
- More holds settings, help and support.
- Adding a Note is a prominent button on Decks and on Browse, not a tab.
- Sync is in the top bar of Decks.
- Upstream's unfinished bottom navigation (behind a developer flag) is the starting point. The drawer is removed on phones once the bottom bar is the default, not merely disabled underneath it.
- Tablets and other large screens keep the drawer and their existing two-pane layouts, reskinned. A bespoke large-screen design is deferred.

### Core Loop

- **Deck list.** Rebuilt in Compose. It keeps the Deck tree with expand and collapse, the three counts per Deck, the due total, today's study summary, the add menu, and every Deck action available today. How the counts are presented is decided in the Mockup.
- **Study screen.** Upstream's newer study screen is rebuilt and becomes the Fork's default. The Card Frame is redesigned; the Card Content is rendered exactly as today.
- **One study screen (ADR-0004).** The old study screen is never rebuilt. It is removed once the study screen can replace it, in this order:
  1. The AnkiDroid JavaScript API is implemented in the study screen, with the same results as the old study screen for the same Card Template. This is non-UI work taken on deliberately.
  2. Each feature only the old study screen has is marked "build" or "drop" by the owner when the study screen's Mockup is drawn: saving a whiteboard drawing, reading the whole Card aloud, answer-button position, minimum press time, double-tap timeout, and any other found. A "drop" is a deliberate exception to "no existing feature is removed".
  3. The old study screen, the setting that chose between the two, and the settings only the old one used are deleted.
- Until the removal, the old study screen stays selectable in settings and gets no redesign.
- Deck title and time-remaining estimate are decided in the same Mockup.
- **Finished screen.** Built as a Native Surface, replacing the Anki Page for this one screen. It shows everything the page shows today, from data the Engine already exposes to native code: when more Cards become available, whether a daily limit was reached, whether Cards are buried and a way to unbury them, a route to custom study, and a route to Deck options. It opens both when a study session ends and when a finished Deck is tapped.

### Finding and adding

- The card browser, note editor and previewer are rebuilt in Compose, keeping their existing logic.
- The card browser list must stay smooth on very large collections; it renders rows lazily and never loads the whole result set into the interface.
- Upstream's unfinished card-browser work (the new search view and the search tools behind developer flags) is evaluated when the card browser's Mockup is drawn, and is adopted, finished or dropped as the Mockup requires.

### Settings and dialogs

- All settings screens and all dialogs (roughly 50) are rebuilt in Compose.
- Every existing setting remains. Settings may be regrouped.
- Settings only the old study screen used are removed with it, before the settings rebuild. Settings never need to serve two study screens.
- Dialogs share one small set of patterns: confirmation, destructive confirmation, input, single choice, multiple choice, progress and error.

### The rest

- Note Type management, the field editor, the Card Template editor and its previewer are rebuilt in Compose.
- Sign-in and sync setup are rebuilt around a self-hosted sync server.
- The first-run screen is rebuilt for a Newcomer and does not lead with AnkiWeb.
- Widgets (Glance) and notifications are restyled and reworded.
- The shared-decks browser, which shows AnkiWeb's website, stays.

### Anki Pages

- Statistics, Deck options, Card info, the importers, change Note Type and image occlusion remain Anki Pages. They receive Harmonisation only: the Fork's colours and typeface are supplied to them, and their content and arrangement are untouched.
- The finished screen is the single exception, as above.
- Web work on the Anki Pages follows the modern-web-guidance skill.

### Sync and reporting

- The Fork does not sync with AnkiWeb. AnkiWeb's terms allow access only from the browser and four named clients, and a renamed fork is not one of them (ADR-0001).
- AnkiWeb sign-in and sync are dropped from the Fork early, before the Foundation Milestone is signed off, so that an unfinished build can never sign in by accident. Sync setup asks for a server address and offers no AnkiWeb default; without a server address there is no way to sign in. The first-run screen no longer offers to sync from AnkiWeb, and links to AnkiWeb account sign-up and password reset are removed.
- Browsing and downloading shared Decks from AnkiWeb's website stays, since that is browser access.
- Decks reach the Fork through a self-hosted sync server, using the custom sync server setting the app already has, or by export and import.
- Data must always be able to leave the Fork and reach AnkiWeb through the Official App. A whole collection or a single Deck exported from the Fork, with scheduling, review history and media, must import into the current release of the Official App, which can then sync it to AnkiWeb. The reverse direction must work too.
  - This holds because export and import are done by the Engine, unchanged, in the same package formats the Official App uses. It is checked on a device at every Milestone sign-off rather than assumed.
  - The Fork's Engine can be newer than the Official App a person has installed. The existing export option that writes a package older versions can read is kept and stays easy to find.
  - Before an Engine update is taken, the round trip is checked against the current release of the Official App.
  - Importing a whole collection into the Official App replaces what is there, and its next sync is a one-way upload that overwrites AnkiWeb. Importing a Deck merges into what is there, and its next sync is a normal one. The export screen says which is which.
- Data must be just as easy to bring in. A person arriving from AnkiDroid or AnkiWeb can bring their Decks, scheduling, review history and media into the Fork:
  - **From AnkiDroid or desktop Anki:** export a collection or a Deck package there, and import it into the Fork.
  - **From AnkiWeb:** the Fork cannot sign in, so the route is indirect. Sync AnkiWeb down into the Official App or desktop Anki, export a package there, and import it into the Fork. The Fork explains these steps in the app where a person would look for them.
  - **From a file:** opening a collection or Deck package from a file manager or another app offers the Fork as a place to open it.
  - **Shared Decks:** browsing and downloading from AnkiWeb's website stays.
- Import is offered on the first-run screen from the moment AnkiWeb sync is dropped, in the place where "Sync from AnkiWeb" used to be.
- During development, the Fork is tested with a copy of a collection brought in by import, never with a live synced account.
- Crash reporting to Upstream's server and usage analytics are switched off in the Fork, and their settings entries are removed.

### Mockups

- Mockups are web pages at phone size, built with React, Tailwind and shadcn/ui, following the modern-web-guidance skill. Ideas may be borrowed from astryx's themes.
- Mockups are disposable. Only design values carry over to Android: colour, type sizes and weights, spacing and corner shapes. Components are rebuilt natively to match.
- The three Directions are Quiet paper (warm off-white, ink-dark text, one accent, editorial), Crisp tool (cool neutrals, sharp sans, denser, one vivid accent) and Soft and friendly (rounder, more colour, larger elements). All three are drawn from the same components, themed differently.
- Each Direction is shown on the deck list and the study screen, in the Light theme, in English and Simplified Chinese side by side. Dark and Black are drawn only for the chosen Direction.
- Mockups use the same sample Decks as the "before" pictures, so that before and after are comparable.

### Milestones

0. **Direction.** Three Directions as Mockups; the owner picks one. Then the name, the launcher icon, and the Dark and Black themes for it.
1. **Foundation.** The chosen Direction applied app-wide; the Fork's identity; crash reporting and analytics off; themes cut to three; fonts bundled; the hard-coded colour and text-size sweep.
2. **Core Loop.** Bottom bar, deck list, study screen, finished screen; the AnkiDroid JavaScript API in the study screen; removal of the old study screen.
3. **Finding and adding.** Card browser, note editor, previewer.
4. **Settings and dialogs.**
5. **The rest.** Note Types and Card Templates, sign-in and sync, first run, widgets, notifications, Harmonisation of the Anki Pages.

A Milestone is done when every surface in it has an agreed Mockup, is rebuilt, passes the Quality Bar, and has run on a device. Nothing in the next Milestone starts before that.

### Quality Bar

A rebuilt Native Surface counts as done only when all of these hold:

- Text contrast meets WCAG AA.
- Touch targets are at least 48dp.
- The layout mirrors correctly in a right-to-left language.
- The layout is usable at 200% font size.
- It has been checked in Light, Dark and Black.
- It has been checked in English, Simplified Chinese and Japanese.

## Testing Decisions

### What makes a good test here

A good test exercises a Native Surface the way a person does and checks what that person would see. It opens the surface on a seeded collection, taps and types by visible label, and asserts on what is then visible. It does not reach into a screen's internal structure, name its private functions, or depend on which toolkit drew it. A test written this way survives the rebuild it is meant to protect.

### Seams

- **The rendered screen (the main seam; all new tests go here).** Each rebuilt Native Surface is rendered without a device on a seeded collection, driven through its accessibility labels, checked for visible outcomes, and captured as a picture at the same point. One test covers behaviour and looks.
  - The existing harness varies theme and device size. It is extended to also vary language (English, Simplified Chinese, Japanese, one right-to-left language) and font size (normal and 200%), and its themes are cut to Light, Dark and Black.
  - The accessibility check that already runs at capture enforces contrast and touch-target size.
  - With this extension the Quality Bar is checked mechanically on every rebuilt surface.
- **Screen logic (existing; kept passing, not expanded).** The rebuild leaves the logic behind each screen alone, so these tests keep passing unchanged. A new test goes here only for a rule that cannot be observed on the screen.
- **The Engine (existing; untouched).** Upstream's tests stay as they are. Nothing is added, because nothing is changed there.

### What happens to existing tests

- Upstream's screenshot test for a surface is replaced when that surface is rebuilt, because the picture changes.
- Tests that reach into a screen's old XML internals are rewritten at the rendered-screen seam, not deleted.
- Tests of screen logic and of the Engine must stay green throughout. A failure there means the rebuild changed behaviour and is a defect.
- Upstream's lint checks keep passing, except rules that only serve Upstream's translation platform, which are reviewed when the Fork first adds its own strings.
- Baseline pictures are regenerated once per Milestone and reviewed by the owner.

### Prior art in the codebase

- `ScreenshotTest` and its roughly 90 subclasses: device-free screenshot tests on a real test collection, parameterised by theme and device size, with an accessibility check at capture.
- `ComposeScreenshotTest`: the Compose equivalent, with a font-size argument.
- `AnswerAreaTest` and `TextInputDialogTest`: Compose components driven by their visible labels.
- `DeckPickerViewModelTest`, `ReviewerViewModelTest`, `CardBrowserViewModelTest`, `CongratsViewModelTest` and about 26 others: screen-logic tests on a real in-memory collection.
- `InMemoryAnkiTest`: the Engine-level base for tests on a collection.

### On-device check

Each Milestone ends with a run on the Pixel 9 emulator, repeating the walk used for the "before" pictures, and a sign-off on the owner's own phone once it is connected.

Each sign-off also includes the round trip with the Official App: a collection and a Deck exported from the Fork are imported into the current release of the Official App installed beside it, and checked for Cards, scheduling, review history and media; then the same in the other direction.

## Out of Scope

- Any change to collection format, sync protocol, scheduling behaviour or the Anki backend.
- Redesigning the old study screen. It is removed, not redesigned.
- New features in the AnkiDroid JavaScript API. The study screen matches what the old study screen supported and adds nothing.
- Restyling Card Content or the default Card Templates.
- Rebuilding any Anki Page natively, other than the finished screen.
- Syncing with AnkiWeb.
- Contributing anything upstream, or shaping code to be mergeable there.
- Deciding how other people get the app: a store release, signing for distribution and support. Deferred, not ruled out.
- A bespoke tablet, foldable or desktop design.
- The Plain and E-ink themes, and wallpaper-derived dynamic colour.
- Translating new strings into languages other than English, Simplified Chinese and Japanese. Deferred, not ruled out.
- A dependency-injection framework, or any internal re-architecture that changes nothing visible.
- Compatibility with other apps that integrate with the Official App's public API.

## Further Notes

### Open questions

- **Decisions made when the Fork was for its owner only.** Other people will now use it. These were decided with one person in mind and have not been revisited:
  - The Plain and E-ink themes are removed.
  - Other apps that send Cards to AnkiDroid through its public API will not see the Fork. Text shared the ordinary way still arrives.
  - There is no crash reporting at all.
- **Distribution and further translations.** The owner has deferred both: how other people get the app, and translating new text beyond English, Simplified Chinese and Japanese.
- **Self-hosted sync server.** Setting one up is the owner's task and is outside the app. The Fork only needs to point at it.

### Decided later, by looking

The Direction; the Fork's name and launcher icon; each font pairing; the icon style; and per-screen details such as how Deck counts are shown and whether the study screen shows the deck title and time remaining.

### Facts established so far

- Upstream at commit `2f6b7a649e` builds on this machine with Java 21 (the build enforces 21–25). The first build took about 20 minutes.
- A Pixel 9 emulator on Android 16 is set up with 36 sample Cards across five Decks in Japanese, Chinese and English.
- "Before" pictures of the deck list, drawer, both of Upstream's study screens, the finished screen, a dialog and one Anki Page are in `docs/redesign/before/`.
- The newer study screen lacks the AnkiDroid JavaScript API, saving a whiteboard drawing, the legacy read-aloud speech, the time-remaining estimate, the deck title, and a choice of answer-button position.
- Upstream's bottom navigation is phone-only and unfinished; the drawer still sits underneath it.
- The app is about 190 XML layouts plus about 20 Compose files, on Material 3 base themes, with 82 locales.

### Cost

This is the largest version of the project. Rebuilding a surface in Compose costs several times more than restyling it, Upstream's interface work no longer arrives for free, and each Engine update needs manual attention.
