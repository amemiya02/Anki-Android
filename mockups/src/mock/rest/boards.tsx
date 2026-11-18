// Boards for the rest of the app (ticket 41): one surface in Light, Dark and
// Black, an overview of every surface, and the decisions for the owner.
// Reached with ?rest=<surface>&lang=en|zh, ?rest=overview and ?rest=decide-<which>.

import type { ReactNode } from "react"

import type { Lang, Theme } from "../data"
import { Phone } from "../screens"
import {
  AddNoteTypeScreen,
  AnkiWebScreen,
  FieldActionsScreen,
  FullSyncScreen,
  NoteTypeScreen,
  NoteTypesScreen,
  NotificationsScreen,
  ReminderEditScreen,
  RemindersCheckScreen,
  RemindersScreen,
  SyncAccountScreen,
  SyncConflictScreen,
  SyncErrorScreen,
  SyncServerScreen,
  SyncSignInScreen,
  TemplateMoreScreen,
  TemplatePreviewScreen,
  TemplateScreen,
  WelcomeScreen,
  WidgetConfigScreen,
  WidgetSizesScreen,
  WidgetsScreen,
} from "./screens"

import "./rest.css"

const langName: Record<Lang, string> = { en: "English", zh: "简体中文" }
const themes: Theme[] = ["light", "dark", "black"]
const themeName: Record<Theme, string> = {
  light: "Light",
  dark: "Dark",
  black: "Black",
}

type Surface = {
  name: string
  draw: (lang: Lang) => ReactNode
  /** what the three panels stand for, where it is not the app's own theme */
  themeNote?: string
}

export const surfaces = {
  "note-types": {
    name: "Note Types",
    draw: (lang) => <NoteTypesScreen lang={lang} />,
  },
  "note-type-add": {
    name: "Add a Note Type",
    draw: (lang) => <AddNoteTypeScreen lang={lang} />,
  },
  "note-type": {
    name: "A Note Type: its fields and Card Templates",
    draw: (lang) => <NoteTypeScreen lang={lang} />,
  },
  "field-actions": {
    name: "A field’s actions",
    draw: (lang) => <FieldActionsScreen lang={lang} />,
  },
  "full-sync": {
    name: "A change that needs a one-way sync",
    draw: (lang) => <FullSyncScreen lang={lang} />,
  },
  template: {
    name: "Card Template editor",
    draw: (lang) => <TemplateScreen lang={lang} />,
  },
  "template-more": {
    name: "Card Template editor, its actions",
    draw: (lang) => <TemplateMoreScreen lang={lang} />,
  },
  "template-preview": {
    name: "Card Template previewer",
    draw: (lang) => <TemplatePreviewScreen lang={lang} />,
  },
  "sync-server": {
    name: "Sync setup 1: the server address",
    draw: (lang) => <SyncServerScreen lang={lang} />,
  },
  "sync-sign-in": {
    name: "Sync setup 2: sign in to the server",
    draw: (lang) => <SyncSignInScreen lang={lang} />,
  },
  "sync-account": {
    name: "Sync: signed in, and how syncing behaves",
    draw: (lang) => <SyncAccountScreen lang={lang} />,
  },
  "sync-error": {
    name: "Sync error, in plain words",
    draw: (lang) => <SyncErrorScreen lang={lang} />,
  },
  "sync-conflict": {
    name: "Sync conflict: keep which collection",
    draw: (lang) => <SyncConflictScreen lang={lang} />,
  },
  welcome: {
    name: "First run",
    draw: (lang) => <WelcomeScreen lang={lang} />,
  },
  "welcome-ankiweb": {
    name: "First run: coming from AnkiWeb",
    draw: (lang) => <AnkiWebScreen lang={lang} />,
  },
  widgets: {
    name: "Home-screen widgets",
    draw: (lang) => <WidgetsScreen lang={lang} />,
    themeNote:
      "Widgets follow the app’s theme: Light by day, and Dark or Black (whichever the app is set to) by night.",
  },
  "widget-sizes": {
    name: "Widgets at their other sizes",
    draw: (lang) => <WidgetSizesScreen lang={lang} />,
    themeNote:
      "Widgets follow the app’s theme: Light by day, and Dark or Black (whichever the app is set to) by night.",
  },
  "widget-config": {
    name: "Choosing the Decks of the deck list widget",
    draw: (lang) => <WidgetConfigScreen lang={lang} />,
  },
  notifications: {
    name: "Notifications",
    draw: (lang) => <NotificationsScreen lang={lang} />,
    themeNote:
      "Android draws the shade in its own light or dark colours; Black shows the dark shade. Only the mark, name, accent and wording are the app’s.",
  },
  reminders: {
    name: "Review reminders",
    draw: (lang) => <RemindersScreen lang={lang} />,
  },
  "reminder-edit": {
    name: "Adding a review reminder",
    draw: (lang) => <ReminderEditScreen lang={lang} />,
  },
  "reminders-check": {
    name: "Checking that reminders can arrive",
    draw: (lang) => <RemindersCheckScreen lang={lang} />,
  },
} satisfies Record<string, Surface>

export type SurfaceKey = keyof typeof surfaces
const keys = Object.keys(surfaces) as SurfaceKey[]

function Header({ title, line }: { title: string; line: string }) {
  return (
    <div>
      <h2 className="text-[34px] leading-tight font-semibold text-neutral-900">
        {title}
      </h2>
      <p className="mt-1 text-[17px] text-neutral-600">{line}</p>
    </div>
  )
}

function Shot({
  lang,
  theme = "light",
  label,
  children,
}: {
  lang: Lang
  theme?: Theme
  label: string
  children: ReactNode
}) {
  return (
    <Phone
      direction="paper"
      lang={lang}
      screen="decks"
      theme={theme}
      label={label}
    >
      <div className="rest">{children}</div>
    </Phone>
  )
}

function ThemeBoard({ k, lang }: { k: SurfaceKey; lang: Lang }) {
  const s: Surface = surfaces[k]
  return (
    <section className="flex flex-col gap-8">
      <Header
        title={`${s.name} · ${langName[lang]}`}
        line={s.themeNote ?? "Quiet paper, in the three themes."}
      />
      <div className="flex gap-12">
        {themes.map((theme) => (
          <Shot key={theme} lang={lang} theme={theme} label={themeName[theme]}>
            {s.draw(lang)}
          </Shot>
        ))}
      </div>
    </section>
  )
}

function Overview({ lang }: { lang: Lang }) {
  return (
    <section className="flex flex-col gap-8">
      <Header
        title={`The rest of the app · ${langName[lang]}`}
        line="Every surface drawn for Milestone 5, in Quiet paper, Light theme."
      />
      <div className="grid grid-cols-[repeat(6,380px)] gap-x-12 gap-y-14">
        {keys.map((k) => (
          <Shot key={k} lang={lang} label={surfaces[k].name}>
            {surfaces[k].draw(lang)}
          </Shot>
        ))}
      </div>
    </section>
  )
}

type DecisionKey = "note-types" | "sync" | "welcome"

const decisions: Record<
  DecisionKey,
  {
    title: string
    line: string
    a: { label: string; shots: [string, (lang: Lang) => ReactNode][] }
    b: { label: string; shots: [string, (lang: Lang) => ReactNode][] }
  }
> = {
  "note-types": {
    title: "Note Types: where fields and Card Templates are reached",
    line: "Pick A or B. Both keep every action; only the way in differs.",
    a: {
      label: "A (recommended): a Note Type opens its own page",
      shots: [
        ["Note Types", (lang) => <NoteTypesScreen lang={lang} />],
        ["The Note Type’s page", (lang) => <NoteTypeScreen lang={lang} />],
      ],
    },
    b: {
      label: "B: as today, buttons on each row",
      shots: [
        [
          "Note Types",
          (lang) => <NoteTypesScreen lang={lang} style="buttons" />,
        ],
        [
          "Tapping a row: the fields",
          (lang) => <NoteTypeScreen lang={lang} templates={false} />,
        ],
      ],
    },
  },
  sync: {
    title: "Sync: one place, or account and settings apart",
    line: "Pick A or B. The setup steps (address, then sign-in) are the same in both.",
    a: {
      label: "A (recommended): one Sync page",
      shots: [
        ["English", (lang) => <SyncAccountScreen lang={lang} />],
        ["简体中文", () => <SyncAccountScreen lang="zh" />],
      ],
    },
    b: {
      label: "B: the account, with a row to Settings › Sync",
      shots: [
        ["English", (lang) => <SyncAccountScreen lang={lang} style="split" />],
        ["简体中文", () => <SyncAccountScreen lang="zh" style="split" />],
      ],
    },
  },
  welcome: {
    title: "First run: one way in first, or three equal ways",
    line: "Pick A or B. Both offer the same three choices and the AnkiWeb steps.",
    a: {
      label: "A (recommended): starting empty is the primary button",
      shots: [
        ["English", (lang) => <WelcomeScreen lang={lang} />],
        ["简体中文", () => <WelcomeScreen lang="zh" />],
      ],
    },
    b: {
      label: "B: three equal rows",
      shots: [
        ["English", (lang) => <WelcomeScreen lang={lang} style="three" />],
        ["简体中文", () => <WelcomeScreen lang="zh" style="three" />],
      ],
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
            <p className="text-[19px] font-semibold text-neutral-800">
              {option.label}
            </p>
            <div className="flex gap-9">
              {option.shots.map(([label, draw]) => (
                <Shot key={label} lang="en" label={label}>
                  {draw("en")}
                </Shot>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

export function RestBoard({ which, lang }: { which: string; lang: Lang }) {
  if (which === "overview") return <Overview lang={lang} />
  if (which.startsWith("decide-"))
    return <DecisionBoard which={which.slice(7) as DecisionKey} />
  if (which in surfaces)
    return <ThemeBoard k={which as SurfaceKey} lang={lang} />
  return <p>Unknown surface: {which}</p>
}
