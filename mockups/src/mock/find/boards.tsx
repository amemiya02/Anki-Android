// Boards for the Milestone 3 Mockups (ticket 25): each surface in Light, Dark
// and Black, an overview, and the decisions the owner makes.
// URLs: /?find=<surface>&lang=en|zh, /?find=overview&lang=en,
// /?find=decide-<decision>.

import type { ReactNode } from "react"

import type { Lang, Theme } from "../data"

import {
  BrowseScreen,
  ColumnsScreen,
  EditorScreen,
  FindPhone,
  FlagsSheetScreen,
  PreviewScreen,
  SearchScreen,
  SelectScreen,
  SortSheetScreen,
  TemplatePreviewScreen,
} from "./screens"

const langName: Record<Lang, string> = { en: "English", zh: "简体中文" }
const themes: [Theme, string][] = [
  ["light", "Light"],
  ["dark", "Dark"],
  ["black", "Black"],
]

export const findSurfaces = {
  browse: ["Card browser: results", (l: Lang) => <BrowseScreen lang={l} />],
  "browse-search": [
    "Card browser: the search page",
    (l: Lang) => <SearchScreen lang={l} />,
  ],
  "browse-flags": [
    "Card browser: the Flags filter",
    (l: Lang) => <FlagsSheetScreen lang={l} />,
  ],
  "browse-sort": [
    "Card browser: sort order",
    (l: Lang) => <SortSheetScreen lang={l} />,
  ],
  "browse-menu": [
    "Card browser: the menu",
    (l: Lang) => <BrowseScreen lang={l} menu />,
  ],
  "browse-select": [
    "Card browser: selecting Cards",
    (l: Lang) => <SelectScreen lang={l} />,
  ],
  "browse-actions": [
    "Card browser: every bulk action",
    (l: Lang) => <SelectScreen lang={l} sheet />,
  ],
  "browse-columns": [
    "Card browser: columns",
    (l: Lang) => <ColumnsScreen lang={l} />,
  ],
  "editor-add": [
    "Note editor: adding a Note",
    (l: Lang) => <EditorScreen lang={l} />,
  ],
  "editor-typing": [
    "Note editor: typing a cloze, with the keyboard",
    (l: Lang) => <EditorScreen lang={l} cloze keyboard />,
  ],
  "editor-duplicate": [
    "Note editor: duplicate warning",
    (l: Lang) => <EditorScreen lang={l} duplicate />,
  ],
  "editor-attach": [
    "Note editor: attaching to a field",
    (l: Lang) => <EditorScreen lang={l} attach />,
  ],
  "editor-edit": [
    "Note editor: editing a Note, with its menu",
    (l: Lang) => <EditorScreen lang={l} mode="edit" menu />,
  ],
  "preview-question": [
    "Previewer: question",
    (l: Lang) => <PreviewScreen lang={l} side="question" />,
  ],
  "preview-answer": [
    "Previewer: answer",
    (l: Lang) => <PreviewScreen lang={l} side="answer" />,
  ],
  "preview-templates": [
    "Previewer from the note editor",
    (l: Lang) => <TemplatePreviewScreen lang={l} />,
  ],
} satisfies Record<string, [string, (l: Lang) => ReactNode]>

export type FindKey = keyof typeof findSurfaces
const findKeys = Object.keys(findSurfaces) as FindKey[]

function BoardHeader({ title, line }: { title: string; line: string }) {
  return (
    <div>
      <h2 className="text-[34px] leading-tight font-semibold text-neutral-900">
        {title}
      </h2>
      <p className="mt-1 text-[17px] text-neutral-600">{line}</p>
    </div>
  )
}

function FindThemeBoard({ k, lang }: { k: FindKey; lang: Lang }) {
  const [name, draw] = findSurfaces[k]
  return (
    <section className="flex flex-col gap-8">
      <BoardHeader
        title={`${name} · ${langName[lang]}`}
        line="Quiet paper, in the three themes."
      />
      <div className="flex gap-12">
        {themes.map(([theme, label]) => (
          <FindPhone key={theme} lang={lang} theme={theme} label={label}>
            {draw(lang)}
          </FindPhone>
        ))}
      </div>
    </section>
  )
}

function FindOverview({ lang }: { lang: Lang }) {
  return (
    <section className="flex flex-col gap-8">
      <BoardHeader
        title={`Finding and adding · ${langName[lang]}`}
        line="Every surface in Milestone 3, in Quiet paper, Light theme."
      />
      <div className="grid grid-cols-[repeat(6,380px)] gap-x-12 gap-y-14">
        {findKeys.map((k) => (
          <FindPhone key={k} lang={lang} label={findSurfaces[k][0]}>
            {findSurfaces[k][1](lang)}
          </FindPhone>
        ))}
      </div>
    </section>
  )
}

const decisions = {
  rows: {
    title: "Card browser: how a result row shows its columns",
    line: "Pick A or B. Both keep the chosen columns and their order; only the arrangement differs.",
    a: [
      "A (recommended): two lines per row",
      (l: Lang) => <BrowseScreen lang={l} />,
    ],
    b: [
      "B: a table of columns, as today",
      (l: Lang) => <BrowseScreen lang={l} rows="table" />,
    ],
  },
  search: {
    title: "Card browser: Upstream’s unfinished search tools",
    line: "Pick A or B. Both adopt and finish Upstream’s search view (the search page and the filter chips).",
    a: [
      "A (recommended): drop the tools, link to the manual",
      (l: Lang) => <SearchScreen lang={l} />,
    ],
    b: [
      "B: finish the tools as a translated syntax list",
      (l: Lang) => <SearchScreen lang={l} help="builder" />,
    ],
  },
  save: {
    title: "Note editor: where Add and Save sit",
    line: "Pick A or B.",
    a: [
      "A (recommended): in the top bar",
      (l: Lang) => <EditorScreen lang={l} />,
    ],
    b: [
      "B: a wide button above the toolbar",
      (l: Lang) => <EditorScreen lang={l} save="bottom" />,
    ],
  },
} satisfies Record<
  string,
  {
    title: string
    line: string
    a: [string, (l: Lang) => ReactNode]
    b: [string, (l: Lang) => ReactNode]
  }
>

export type FindDecision = keyof typeof decisions

function FindDecisionBoard({ which }: { which: FindDecision }) {
  const d = decisions[which]
  return (
    <section className="flex flex-col gap-8">
      <BoardHeader title={d.title} line={d.line} />
      <div className="flex gap-20">
        {[d.a, d.b].map(([label, draw]) => (
          <div key={label} className="flex flex-col gap-5">
            <p className="text-[19px] font-semibold text-neutral-800">
              {label}
            </p>
            <div className="flex gap-9">
              {(["en", "zh"] as Lang[]).map((lang) => (
                <FindPhone key={lang} lang={lang} label={langName[lang]}>
                  {draw(lang)}
                </FindPhone>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

/** The board for ?find=…: a surface, the overview, or a decision. */
export function FindBoard({ which, lang }: { which: string; lang: Lang }) {
  if (which === "overview") return <FindOverview lang={lang} />
  if (which.startsWith("decide-"))
    return <FindDecisionBoard which={which.slice(7) as FindDecision} />
  return <FindThemeBoard k={which as FindKey} lang={lang} />
}
