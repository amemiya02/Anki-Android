import {
  directions,
  type Direction,
  type Lang,
  type Side,
  type Theme,
} from "@/mock/data"
import { IdentityBoard } from "@/mock/identity"
import { DeckListScreen, Phone, StudyScreen } from "@/mock/screens"

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

export function App() {
  const params = new URLSearchParams(window.location.search)
  const only = params.get("d") as Direction | null
  const compare = params.get("compare") as ScreenKey | null
  const themed = params.get("themes") as ScreenKey | null
  const theme = (params.get("theme") as Theme | null) ?? "light"
  const lang = (params.get("lang") as Lang | null) ?? "en"
  const identity = params.has("identity")

  let board
  if (identity) board = <IdentityBoard />
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
