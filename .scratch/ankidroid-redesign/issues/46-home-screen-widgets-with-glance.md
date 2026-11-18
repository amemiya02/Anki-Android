# 46: Home-screen widgets with Glance

**What to build:** The Fork's home-screen widgets match the app.

**Blocked by:** 41 (Mockups for the rest)

**Status:** done

- [x] Every widget that exists today is rebuilt with Glance and shows the same information.
- [x] Tapping a widget opens the same place as today.
- [x] Widgets follow the app's theme, Black included (per the Mockups, decision 5), and stay legible at each size they can be resized to.
- [x] Widget text is provided in English, Simplified Chinese and Japanese.
- [x] It matches the agreed Mockup.

## Implementation

- All four launcher providers use Glance. Existing widget bindings and saved Deck selections carry over. Deck list setup keeps up to five Decks in the saved order and supports dragging, accessible move actions and removal.
- Deck list shows today's collection total and one or more complete Deck rows. One Deck uses compact counts at minimum width and labeled counts at larger sizes. A deleted Deck opens the chooser.
- The widgets use the app's Light, Dark and Black palette and its selected language. The launcher uses the system serif font for Glance text.

## Verification

An Android 15 AppWidgetHost kept both configured Deck widgets after the Play debug APK replaced the preceding build. It requested the minimum and larger sizes, displayed all four widgets on Light, Dark and Black, and showed English, Simplified Chinese and Japanese. At 200% font size the Japanese minimum widgets kept the action label, due count, estimated minutes and selected Deck name visible. [Widget screenshots](../evidence/ticket46-widget-host.jpg)

Real widget taps opened Add note, Decks, the reviewer for a Deck with cards to study, and Deck options for a Deck with zero due. A repeat Android walk verified that the Add note widget reached the editable note form and the Due count widget reached the populated Deck list. A deleted Deck showed the missing state and opened the Deck chooser when tapped. Both configured widgets could be edited on the same launcher instances. The Deck list accepted five choices, saved a drag reorder, and showed the new first Deck when resized. [Setup screenshots](../evidence/ticket46-widget-setup.jpg)

The One Deck configuration passed all 24 rendered combinations of theme, language and font size. All 41 focused app checks and all three widget module tests passed. The app checks covered the Japanese compact labels, launcher graphics, and the widget setup, update and recovery paths.
