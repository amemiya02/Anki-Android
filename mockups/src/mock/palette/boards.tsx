// Candidate Light palettes, drawn because the owner asked whether the Light
// colour scheme can change. Each row shows the same five Light screens with
// only the --v-* colour roles (and the flag colours) replaced.
// The owner chose 白纸 Plain paper on 2026-11-18; it is now the values file's
// Light palette, drawn as "current".
// URLs: /?palette=compare&lang=en|zh, /?palette=<name>&lang=en|zh.

import type { CSSProperties, ReactNode } from "react"

import values from "../../../../docs/redesign/quiet-paper.values.json"

import type { Lang } from "../data"
import flags from "../find/flags.json"
import { BrowseScreen } from "../find/screens"
import { DeckListScreen, Phone, StudyScreen } from "../screens"
import { DialogScreen, SettingsHome } from "../settings/screens"

import palettes from "./palettes.json"

interface Palette {
  name: string
  line: string
  colour: Record<string, string>
  flags: Record<string, string>
}

export const paletteKeys = ["current", "indigo", "celadon", "ink"] as const
type PaletteKey = (typeof paletteKeys)[number]

const all: Record<PaletteKey, Palette> = {
  current: {
    name: "白纸 Plain paper (current)",
    line: "The current Light palette, chosen by the owner on 2026-11-18: clean near-white paper with ink text; cinnabar stays the one accent.",
    colour: values.colour.light,
    flags: flags.light,
  },
  ...palettes.candidates,
}

const langName: Record<Lang, string> = { en: "English", zh: "简体中文" }

const screens: [string, "decks" | "study", (l: Lang) => ReactNode][] = [
  ["Deck list", "decks", (l) => <DeckListScreen lang={l} />],
  [
    "Study screen, answer",
    "study",
    (l) => <StudyScreen lang={l} side="answer" />,
  ],
  ["Card browser", "decks", (l) => <BrowseScreen lang={l} />],
  ["Settings", "decks", (l) => <SettingsHome lang={l} />],
  [
    "Destructive confirmation",
    "decks",
    (l) => <DialogScreen lang={l} kind="destructive" />,
  ],
]

/** The add button's shadow is the primary colour at 55% opacity. */
function shadowOf(primary: string) {
  return `0 8px 18px -8px color-mix(in oklab, ${primary} 55%, transparent)`
}

function Swatches({ p }: { p: Palette }) {
  const shown: [string, string][] = [
    ["background", "Background"],
    ["onBackground", "Ink"],
    ["primary", "Primary"],
    ["emphasis", "Emphasis"],
  ]
  return (
    <ul className="flex flex-col gap-2">
      {shown.map(([role, label]) => (
        <li key={role} className="flex items-center gap-3">
          <span
            className="size-8 shrink-0 rounded-full ring-1 ring-black/15"
            style={{ background: p.colour[role] }}
          />
          <span className="text-[14px] text-neutral-700">
            {label}{" "}
            <span className="font-mono text-neutral-500">{p.colour[role]}</span>
          </span>
        </li>
      ))}
      <li className="mt-1 flex gap-1.5" aria-label="Counts and answers">
        {["countNew", "countLearning", "countDue", "answerHard"].map((role) => (
          <span
            key={role}
            className="size-5 rounded-full"
            style={{ background: p.colour[role] }}
          />
        ))}
      </li>
    </ul>
  )
}

function PaletteRow({ k, lang }: { k: PaletteKey; lang: Lang }) {
  const p = all[k]
  const flagVars = Object.fromEntries(
    Object.entries(p.flags).map(([name, hex]) => [`--flag-${name}`, hex])
  )
  const extra = {
    ...flagVars,
    "--fab-shadow": shadowOf(p.colour.primary),
    display: "contents",
  } as CSSProperties
  return (
    <section className="flex gap-12">
      <header className="flex w-[260px] shrink-0 flex-col gap-4 pt-2">
        <h3 className="text-[28px] leading-tight font-semibold text-neutral-900">
          {p.name}
        </h3>
        <p className="text-[15px] leading-snug text-neutral-600">{p.line}</p>
        <Swatches p={p} />
      </header>
      {screens.map(([label, screen, draw]) => (
        <Phone
          key={label}
          direction="paper"
          lang={lang}
          screen={screen}
          label={label}
          palette={p.colour}
        >
          <div className="f-contents" style={extra}>
            {draw(lang)}
          </div>
        </Phone>
      ))}
    </section>
  )
}

export function PaletteBoard({ which, lang }: { which: string; lang: Lang }) {
  const keys = paletteKeys.filter((k) => which === "compare" || k === which)
  return (
    <section className="flex flex-col gap-14">
      <div>
        <h2 className="text-[34px] leading-tight font-semibold text-neutral-900">
          {which === "compare"
            ? `Light palettes · ${langName[lang]}`
            : `Light palette candidate · ${langName[lang]}`}
        </h2>
        <p className="mt-1 text-[17px] text-neutral-600">
          The same five Light screens in each palette. Only colour changes; Dark
          and Black stay as they are.
        </p>
      </div>
      {keys.map((k) => (
        <PaletteRow key={k} k={k} lang={lang} />
      ))}
    </section>
  )
}
