import {
  directions,
  finishedStates,
  type ButtonPlace,
  type CountStyle,
  type Direction,
  type FinishedState,
  type Lang,
  type Side,
  type StudyBarStyle,
  type Theme,
} from "@/mock/data"
import { FindBoard } from "@/mock/find/boards"
import { IdentityBoard } from "@/mock/identity"
import { PaletteBoard } from "@/mock/palette/boards"
import { RestBoard } from "@/mock/rest/boards"
import {
  DeckListScreen,
  FinishedScreen,
  MoreScreen,
  Phone,
  StudyScreen,
} from "@/mock/screens"
import { SettingsBoard } from "@/mock/settings/boards"

const langName: Record<Lang, string> = { en: "English", zh: "简体中文" }
const themeName: Record<Theme, string> = {
  light: "Light",
  dark: "Dark",
  black: "Black",
}
const order: Direction[] = ["paper", "crisp", "soft"]
const themes: Theme[] = ["light", "dark", "black"]

type ScreenKey = "decks" | Side
const screenName: Record<ScreenKey, string> = {
  decks: "Deck list",
  question: "Study screen, question",
  answer: "Study screen, answer",
}

function Shot({
  direction,
  lang,
  screen,
  theme = "light",
  label,
}: {
  direction: Direction
  lang: Lang
  screen: ScreenKey
  theme?: Theme
  label: string
}) {
  return (
    <Phone
      direction={direction}
      lang={lang}
      screen={screen === "decks" ? "decks" : "study"}
      theme={theme}
      label={label}
    >
      {screen === "decks" ? (
        <DeckListScreen lang={lang} />
      ) : (
        <StudyScreen lang={lang} side={screen} />
      )}
    </Phone>
  )
}

function BoardHeader({
  title,
  line,
  extra,
}: {
  title: string
  line: string
  extra?: string
}) {
  return (
    <div>
      <h2 className="text-[34px] leading-tight font-semibold text-neutral-900">
        {title}
      </h2>
      <p className="mt-1 text-[17px] text-neutral-600">{line}</p>
      {extra && <p className="mt-2 text-[15px] text-neutral-500">{extra}</p>}
    </div>
  )
}

/** One Direction: each screen in English and Simplified Chinese side by side. */
function DirectionBoard({
  direction,
  theme = "light",
}: {
  direction: Direction
  theme?: Theme
}) {
  const d = directions[direction]
  const screens: ScreenKey[] = ["decks", "question", "answer"]
  return (
    <section className="flex flex-col gap-8">
      <header className="flex items-end justify-between gap-8">
        <BoardHeader
          title={theme === "light" ? d.name : `${d.name} · ${themeName[theme]}`}
          line={d.line}
          extra={`Typefaces: ${d.fonts}`}
        />
        <div className="flex gap-2" aria-hidden="true">
          {d.swatches.map((c) => (
            <span
              key={c}
              className="size-10 rounded-full ring-1 ring-black/10"
              style={{ background: c }}
            />
          ))}
        </div>
      </header>
      <div className="flex gap-12">
        {screens.map((screen) => (
          <div key={screen} className="flex gap-9">
            {(["en", "zh"] as Lang[]).map((lang) => (
              <Shot
                key={lang}
                direction={direction}
                lang={lang}
                screen={screen}
                theme={theme}
                label={`${screenName[screen]} · ${langName[lang]}`}
              />
            ))}
          </div>
        ))}
      </div>
    </section>
  )
}

/** One screen across all three Directions, for choosing between them. */
function CompareBoard({ screen, lang }: { screen: ScreenKey; lang: Lang }) {
  return (
    <section className="flex flex-col gap-8">
      <BoardHeader
        title={`${screenName[screen]} · ${langName[lang]}`}
        line="The same screen in each of the three Directions."
      />
      <div className="flex gap-12">
        {order.map((direction) => (
          <Shot
            key={direction}
            direction={direction}
            lang={lang}
            screen={screen}
            label={directions[direction].name}
          />
        ))}
      </div>
    </section>
  )
}

/** One screen of the chosen Direction in Light, Dark and Black. */
function ThemeBoard({ screen, lang }: { screen: ScreenKey; lang: Lang }) {
  return (
    <section className="flex flex-col gap-8">
      <BoardHeader
        title={`Quiet paper · ${screenName[screen]} · ${langName[lang]}`}
        line="The same screen in the three themes."
      />
      <div className="flex gap-12">
        {themes.map((theme) => (
          <Shot
            key={theme}
            direction="paper"
            lang={lang}
            screen={screen}
            theme={theme}
            label={themeName[theme]}
          />
        ))}
      </div>
    </section>
  )
}

// ---------- Core Loop (Milestone 2), Quiet paper only ----------

type CoreKey =
  | "more"
  | "decks"
  | "decks-empty"
  | "decks-long"
  | "question"
  | "answer"
  | `finished-${FinishedState}`

const coreKeys: CoreKey[] = [
  "more",
  "decks",
  "decks-empty",
  "decks-long",
  "question",
  "answer",
  ...finishedStates.map((s) => `finished-${s}` as const),
]

const coreName: Record<CoreKey, string> = {
  more: "Bottom bar and More",
  decks: "Deck list",
  "decks-empty": "Deck list, empty (a Newcomer)",
  "decks-long": "Deck list, long Chinese and Japanese names",
  question: "Study screen, question",
  answer: "Study screen, answer",
  "finished-later": "Finished: more Cards later today",
  "finished-limit": "Finished: daily limit reached",
  "finished-buried": "Finished: Cards buried",
  "finished-done": "Finished: nothing left",
}

interface CoreOptions {
  countStyle?: CountStyle
  bar?: StudyBarStyle
  buttons?: ButtonPlace
}

function CoreScreen({
  k,
  lang,
  options = {},
}: {
  k: CoreKey
  lang: Lang
  options?: CoreOptions
}) {
  if (k === "more") return <MoreScreen lang={lang} />
  if (k === "decks" || k === "decks-empty" || k === "decks-long")
    return (
      <DeckListScreen
        lang={lang}
        variant={
          k === "decks" ? "full" : k === "decks-empty" ? "empty" : "long"
        }
        countStyle={options.countStyle}
      />
    )
  if (k === "question" || k === "answer")
    return (
      <StudyScreen
        lang={lang}
        side={k}
        bar={options.bar}
        buttons={options.buttons}
      />
    )
  return (
    <FinishedScreen
      lang={lang}
      state={k.replace("finished-", "") as FinishedState}
    />
  )
}

function CoreShot({
  k,
  lang,
  theme = "light",
  label,
  options,
}: {
  k: CoreKey
  lang: Lang
  theme?: Theme
  label: string
  options?: CoreOptions
}) {
  return (
    <Phone
      direction="paper"
      lang={lang}
      screen={k === "question" || k === "answer" ? "study" : "decks"}
      theme={theme}
      label={label}
    >
      <CoreScreen k={k} lang={lang} options={options} />
    </Phone>
  )
}

/** One Core Loop surface in Light, Dark and Black. */
function CoreThemeBoard({ k, lang }: { k: CoreKey; lang: Lang }) {
  return (
    <section className="flex flex-col gap-8">
      <BoardHeader
        title={`${coreName[k]} · ${langName[lang]}`}
        line="Quiet paper, in the three themes."
      />
      <div className="flex gap-12">
        {themes.map((theme) => (
          <CoreShot
            key={theme}
            k={k}
            lang={lang}
            theme={theme}
            label={themeName[theme]}
          />
        ))}
      </div>
    </section>
  )
}

/** Every Core Loop surface in Light, for an overview. */
function CoreOverview({ lang }: { lang: Lang }) {
  return (
    <section className="flex flex-col gap-8">
      <BoardHeader
        title={`The Core Loop · ${langName[lang]}`}
        line="Every surface in Milestone 2, in Quiet paper, Light theme."
      />
      <div className="grid grid-cols-[repeat(5,380px)] gap-x-12 gap-y-14">
        {coreKeys.map((k) => (
          <CoreShot key={k} k={k} lang={lang} label={coreName[k]} />
        ))}
      </div>
    </section>
  )
}

type DecisionKey = "counts" | "studybar" | "buttons"

const decisions: Record<
  DecisionKey,
  {
    title: string
    line: string
    k: CoreKey
    a: { label: string; options: CoreOptions }
    b: { label: string; options: CoreOptions }
  }
> = {
  counts: {
    title: "Deck list: how the new, learning and due counts are shown",
    line: "Pick A or B. The colours are the same in both; only the arrangement differs.",
    k: "decks",
    a: {
      label: "A (recommended): three columns under a header",
      options: { countStyle: "columns" },
    },
    b: {
      label: "B: one total, with the kinds named under the Deck",
      options: { countStyle: "total" },
    },
  },
  studybar: {
    title: "Study screen: Deck title and time-remaining estimate",
    line: "Pick A or B. Both keep the counts, undo and the menu in the top bar.",
    k: "question",
    a: {
      label: "A (recommended): Deck title and estimate shown",
      options: { bar: "full" },
    },
    b: {
      label: "B: counts only, as the newer study screen has today",
      options: { bar: "minimal" },
    },
  },
  buttons: {
    title: "Study screen: where the answer buttons sit",
    line: "Pick A or B. With A there is no setting; with B the old screen’s choice of top or bottom is kept as a setting.",
    k: "answer",
    a: {
      label: "A (recommended): at the bottom, where Show answer was",
      options: { buttons: "bottom" },
    },
    b: {
      label: "B: at the top, above the Card (the old screen’s option)",
      options: { buttons: "top" },
    },
  },
}

/** A decision the owner makes: the recommended answer beside one alternative. */
function DecisionBoard({ which }: { which: DecisionKey }) {
  const d = decisions[which]
  return (
    <section className="flex flex-col gap-8">
      <BoardHeader title={d.title} line={d.line} />
      <div className="flex gap-20">
        {[d.a, d.b].map((option) => (
          <div key={option.label} className="flex flex-col gap-5">
            <p className="text-[19px] font-semibold text-neutral-800">
              {option.label}
            </p>
            <div className="flex gap-9">
              {(["en", "zh"] as Lang[]).map((lang) => (
                <CoreShot
                  key={lang}
                  k={d.k}
                  lang={lang}
                  label={langName[lang]}
                  options={option.options}
                />
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

export function App() {
  const params = new URLSearchParams(window.location.search)
  const only = params.get("d") as Direction | null
  const compare = params.get("compare") as ScreenKey | null
  const themed = params.get("themes") as ScreenKey | null
  const theme = (params.get("theme") as Theme | null) ?? "light"
  const lang = (params.get("lang") as Lang | null) ?? "en"
  const identity = params.has("identity")
  const core = params.get("core") as CoreKey | "overview" | null
  const decide = params.get("decide") as DecisionKey | null
  const find = params.get("find")
  const settings = params.get("settings")
  const restKey = params.get("rest")
  const palette = params.get("palette")

  let board
  if (palette) board = <PaletteBoard which={palette} lang={lang} />
  else if (settings) board = <SettingsBoard which={settings} lang={lang} />
  else if (restKey) board = <RestBoard which={restKey} lang={lang} />
  else if (identity) board = <IdentityBoard />
  else if (find) board = <FindBoard which={find} lang={lang} />
  else if (core === "overview") board = <CoreOverview lang={lang} />
  else if (core) board = <CoreThemeBoard k={core} lang={lang} />
  else if (decide) board = <DecisionBoard which={decide} />
  else if (compare) board = <CompareBoard screen={compare} lang={lang} />
  else if (themed) board = <ThemeBoard screen={themed} lang={lang} />
  else
    board = order
      .filter((d) => !only || d === only)
      .map((d) => <DirectionBoard key={d} direction={d} theme={theme} />)

  return (
    <main className="inline-flex min-h-svh min-w-full flex-col gap-24 bg-[#e7e7ea] p-14">
      {board}
    </main>
  )
}

export default App
