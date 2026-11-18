import type { Theme } from "@/mock/data"
import { Phone } from "@/mock/screens"

import { settingsMap } from "./map"
import {
  DialogScreen,
  SettingsAppearance,
  SettingsControls,
  SettingsHome,
  SettingsSearch,
  SettingsStudying,
  SettingsToolbar,
  type DestructiveStyle,
  type DialogKind,
  type ThemeStyle,
} from "./screens"
import type { SLang } from "./text"

const langName: Record<SLang, string> = { en: "English", zh: "简体中文" }
const themeName: Record<Theme, string> = { light: "Light", dark: "Dark", black: "Black" }
const themes: Theme[] = ["light", "dark", "black"]

const dialogKinds: DialogKind[] = [
  "confirm",
  "destructive",
  "input",
  "single",
  "multiple",
  "progress",
  "error",
]

export type SettingsKey =
  | "home"
  | "search"
  | "appearance"
  | "studying"
  | "controls"
  | "toolbar"
  | `dialog-${DialogKind}`

const keys: SettingsKey[] = [
  "home",
  "search",
  "appearance",
  "studying",
  "controls",
  "toolbar",
  ...dialogKinds.map((k) => `dialog-${k}` as const),
]

const surfaceName: Record<SettingsKey, string> = {
  home: "Settings home",
  search: "Settings search",
  appearance: "Appearance",
  studying: "Studying",
  controls: "Controls",
  toolbar: "Toolbar actions (drag and drop)",
  "dialog-confirm": "Dialog: confirmation",
  "dialog-destructive": "Dialog: destructive confirmation",
  "dialog-input": "Dialog: input",
  "dialog-single": "Dialog: single choice",
  "dialog-multiple": "Dialog: multiple choice",
  "dialog-progress": "Dialog: progress",
  "dialog-error": "Dialog: error",
}

interface Options {
  themeStyle?: ThemeStyle
  destructive?: DestructiveStyle
}

function Surface({ k, lang, options = {} }: { k: SettingsKey; lang: SLang; options?: Options }) {
  switch (k) {
    case "home":
      return <SettingsHome lang={lang} />
    case "search":
      return <SettingsSearch lang={lang} />
    case "appearance":
      return <SettingsAppearance lang={lang} style={options.themeStyle} />
    case "studying":
      return <SettingsStudying lang={lang} />
    case "controls":
      return <SettingsControls lang={lang} />
    case "toolbar":
      return <SettingsToolbar lang={lang} />
    default:
      return (
        <DialogScreen
          lang={lang}
          kind={k.replace("dialog-", "") as DialogKind}
          destructive={options.destructive}
        />
      )
  }
}

function Shot({
  k,
  lang,
  theme = "light",
  label,
  options,
}: {
  k: SettingsKey
  lang: SLang
  theme?: Theme
  label: string
  options?: Options
}) {
  return (
    <Phone direction="paper" lang={lang} screen="decks" theme={theme} label={label}>
      <Surface k={k} lang={lang} options={options} />
    </Phone>
  )
}

function Header({ title, line }: { title: string; line: string }) {
  return (
    <div>
      <h2 className="text-[34px] leading-tight font-semibold text-neutral-900">{title}</h2>
      <p className="mt-1 text-[17px] text-neutral-600">{line}</p>
    </div>
  )
}

/** One surface in Light, Dark and Black. */
function ThemeBoard({ k, lang }: { k: SettingsKey; lang: SLang }) {
  return (
    <section className="flex flex-col gap-8">
      <Header title={`${surfaceName[k]} · ${langName[lang]}`} line="Quiet paper, in the three themes." />
      <div className="flex gap-12">
        {themes.map((theme) => (
          <Shot key={theme} k={k} lang={lang} theme={theme} label={themeName[theme]} />
        ))}
      </div>
    </section>
  )
}

/** Every surface in Light. */
function Overview({ lang }: { lang: SLang }) {
  return (
    <section className="flex flex-col gap-8">
      <Header
        title={`Settings and dialogs · ${langName[lang]}`}
        line="Every surface in Milestone 4, in Quiet paper, Light theme."
      />
      <div className="grid grid-cols-[repeat(5,380px)] gap-x-12 gap-y-14">
        {keys.map((k) => (
          <Shot key={k} k={k} lang={lang} label={surfaceName[k]} />
        ))}
      </div>
    </section>
  )
}

/** Every setting that exists today, in its new group. */
function MapBoard({ lang }: { lang: SLang }) {
  const m = settingsMap[lang]
  const zh = lang === "zh"
  return (
    <section className="flex flex-col gap-8">
      <Header
        title={`Where every setting goes · ${langName[lang]}`}
        line="Every setting that exists today, in the group the Redesign gives it. A note in cinnabar marks a change; #n is a row of core-loop/old-study-screen-features.md."
      />
      <div className="map" lang={zh ? "zh-CN" : "en"}>
        <div className="map-columns">
          {m.groups.map((g) => (
            <div key={g.title} className="map-group">
              <h3>{g.title}</h3>
              {g.sections.map((s, i) => (
                <div key={s.title ?? i}>
                  {s.title && <h4>{s.title}</h4>}
                  <ul>
                    {s.items.map(([name, note]) => (
                      <li key={name}>
                        <span>{name}</span>
                        {note && (
                          <small className={/^(from|built|old|Theme and|来自|新做|沿用|主题与)/.test(note) ? "is-change" : ""}>
                            {note}
                          </small>
                        )}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          ))}
          <div className="map-group map-aside">
            <h3>{zh ? "随旧学习界面移除（负责人 2026-11-17 的决定）" : "Removed with the old study screen (owner, 2026-11-17)"}</h3>
            <ul>
              {m.dropped.map(([name, note]) => (
                <li key={name}>
                  <span>{name}</span>
                  <small>{note}</small>
                </li>
              ))}
            </ul>
          </div>
          <div className="map-group map-aside">
            <h3>{zh ? "仅在开发者选项开启时" : "Behind a developer option"}</h3>
            <ul>
              {m.flagged.map(([name, note]) => (
                <li key={name}>
                  {name}
                  <small>{note}</small>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  )
}

type DecisionKey = "theme" | "destructive"

const decisions: Record<
  DecisionKey,
  {
    title: string
    line: string
    k: SettingsKey
    a: { label: string; options: Options }
    b: { label: string; options: Options }
  }
> = {
  theme: {
    title: "Appearance: how the theme is chosen",
    line: "Pick A or B. Both offer Follow system, Light, Dark and Black.",
    k: "appearance",
    a: {
      label: "A (recommended): one choice of four, shown as pictures",
      options: { themeStyle: "tiles" },
    },
    b: {
      label: "B: two settings, Theme and Night theme, as today",
      options: { themeStyle: "lists" },
    },
  },
  destructive: {
    title: "Dialogs: how a destructive choice is marked",
    line: "Pick A or B. The same mark is used in every destructive confirmation.",
    k: "dialog-destructive",
    a: {
      label: "A (recommended): a warning mark, and an outlined button never filled",
      options: { destructive: "outlined" },
    },
    b: {
      label: "B: a filled primary button, like any confirmation",
      options: { destructive: "filled" },
    },
  },
}

function DecisionBoard({ which }: { which: DecisionKey }) {
  const d = decisions[which]
  return (
    <section className="flex flex-col gap-8">
      <Header title={d.title} line={d.line} />
      <div className="flex gap-20">
        {[d.a, d.b].map((option) => (
          <div key={option.label} className="flex flex-col gap-5">
            <p className="text-[19px] font-semibold text-neutral-800">{option.label}</p>
            <div className="flex gap-9">
              {(["en", "zh"] as SLang[]).map((lang) => (
                <Shot key={lang} k={d.k} lang={lang} label={langName[lang]} options={option.options} />
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

/** `?settings=<surface>|overview|map|decide-theme|decide-destructive&lang=en|zh` */
export function SettingsBoard({ which, lang }: { which: string; lang: SLang }) {
  if (which === "overview") return <Overview lang={lang} />
  if (which === "map") return <MapBoard lang={lang} />
  if (which === "decide-theme") return <DecisionBoard which="theme" />
  if (which === "decide-destructive") return <DecisionBoard which="destructive" />
  return <ThemeBoard k={which as SettingsKey} lang={lang} />
}
