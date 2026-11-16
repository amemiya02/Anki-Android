import { directions, type Direction, type Lang, type Side } from "@/mock/data"
import { DeckListScreen, Phone, StudyScreen } from "@/mock/screens"

const langName: Record<Lang, string> = { en: "English", zh: "简体中文" }
const order: Direction[] = ["paper", "crisp", "soft"]

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
  label,
}: {
  direction: Direction
  lang: Lang
  screen: ScreenKey
  label: string
}) {
  return (
    <Phone
      direction={direction}
      lang={lang}
      screen={screen === "decks" ? "decks" : "study"}
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

/** One Direction: each screen in English and Simplified Chinese side by side. */
function DirectionBoard({ direction }: { direction: Direction }) {
  const d = directions[direction]
  const screens: ScreenKey[] = ["decks", "question", "answer"]
  return (
    <section className="flex flex-col gap-8">
      <header className="flex items-end justify-between gap-8">
        <div>
          <h2 className="text-[34px] leading-tight font-semibold text-neutral-900">
            {d.name}
          </h2>
          <p className="mt-1 text-[17px] text-neutral-600">{d.line}</p>
          <p className="mt-2 text-[15px] text-neutral-500">
            Typefaces: {d.fonts}
          </p>
        </div>
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
      <header>
        <h2 className="text-[34px] leading-tight font-semibold text-neutral-900">
          {screenName[screen]} · {langName[lang]}
        </h2>
        <p className="mt-1 text-[17px] text-neutral-600">
          The same screen in each of the three Directions.
        </p>
      </header>
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

export function App() {
  const params = new URLSearchParams(window.location.search)
  const only = params.get("d") as Direction | null
  const compare = params.get("compare") as ScreenKey | null
  const lang = (params.get("lang") as Lang | null) ?? "en"

  return (
    <main className="inline-flex min-h-svh min-w-full flex-col gap-24 bg-[#e7e7ea] p-14">
      {compare ? (
        <CompareBoard screen={compare} lang={lang} />
      ) : (
        order
          .filter((d) => !only || d === only)
          .map((d) => <DirectionBoard key={d} direction={d} />)
      )}
    </main>
  )
}

export default App
