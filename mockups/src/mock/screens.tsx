import type { CSSProperties, ReactNode } from "react"

import values from "../../../docs/redesign/quiet-paper.values.json"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Separator } from "@/components/ui/separator"

import {
  decks,
  session,
  text,
  totals,
  type Counts,
  type Deck,
  type Direction,
  type Lang,
  type Side,
  type Theme,
} from "./data"

/** A Material Symbols icon, drawn in the style the Direction chooses. */
function Icon({ name, className = "" }: { name: string; className?: string }) {
  return (
    <span className={`msym ${className}`} aria-hidden="true">
      {name}
    </span>
  )
}

function StatusBar() {
  return (
    <div className="statusbar">
      <span>9:41</span>
      <span className="statusbar-icons">
        <Icon name="signal_cellular_alt" />
        <Icon name="wifi" />
        <Icon name="battery_full" />
      </span>
    </div>
  )
}

export function Phone({
  direction,
  lang,
  screen,
  theme = "light",
  label,
  children,
}: {
  direction: Direction
  lang: Lang
  screen: "decks" | "study"
  theme?: Theme
  label: string
  children: ReactNode
}) {
  // The chosen Direction takes its colours from the design values file, so
  // the Mockups and the Foundation can never disagree about a colour.
  const colours =
    direction === "paper"
      ? (Object.fromEntries(
          Object.entries(values.colour[theme]).map(([role, hex]) => [
            `--v-${role}`,
            hex,
          ])
        ) as CSSProperties)
      : undefined
  return (
    <figure className="flex flex-col items-center gap-4">
      <div
        className="phone"
        data-direction={direction}
        data-screen={screen}
        data-theme={theme}
        style={colours}
        lang={lang === "zh" ? "zh-CN" : "en"}
      >
        <div className="screen">
          <StatusBar />
          {children}
        </div>
      </div>
      <figcaption className="text-[13px] text-neutral-500">{label}</figcaption>
    </figure>
  )
}

function CountCells({ counts, lang }: { counts: Counts; lang: Lang }) {
  const t = text[lang]
  const kinds = [
    ["new", t.new],
    ["learn", t.learning],
    ["due", t.due],
  ] as const
  return (
    <span className="counts">
      {kinds.map(([kind, label], i) => (
        <Badge
          key={kind}
          variant="secondary"
          className={`count count-${kind} ${counts[i] === 0 ? "count-zero" : ""}`}
          aria-label={`${label}: ${counts[i]}`}
        >
          {counts[i]}
        </Badge>
      ))}
    </span>
  )
}

function DeckRow({
  deck,
  lang,
  depth = 0,
  active = false,
}: {
  deck: Deck
  lang: Lang
  depth?: number
  active?: boolean
}) {
  const t = text[lang]
  const finished = deck.counts.every((n) => n === 0)
  const hasChildren = !!deck.children?.length
  return (
    <div
      className={`deck-row ${depth > 0 ? "deck-child" : "deck-top"} ${active ? "deck-active" : ""}`}
    >
      <span className="deck-chevron">
        {hasChildren && (
          <Icon
            name={deck.expanded ? "keyboard_arrow_down" : "chevron_right"}
          />
        )}
      </span>
      {depth === 0 && (
        <span className="deck-avatar" lang={deck.nameLang} aria-hidden="true">
          {[...deck.name][0]}
        </span>
      )}
      <span className="deck-name" lang={deck.nameLang}>
        {deck.name}
      </span>
      {finished ? (
        <span className="deck-done">
          <Icon name="check_circle" />
          {t.done}
        </span>
      ) : (
        <CountCells counts={deck.counts} lang={lang} />
      )}
    </div>
  )
}

function BottomBar({ lang }: { lang: Lang }) {
  const t = text[lang]
  const items = [
    ["stacks", t.decks, true],
    ["search", t.browse, false],
    ["bar_chart", t.stats, false],
    ["more_horiz", t.more, false],
  ] as const
  return (
    <nav className="bottombar" aria-label="Primary">
      {items.map(([icon, label, current]) => (
        <a
          key={icon}
          className={`bottombar-item ${current ? "is-current" : ""}`}
          aria-current={current ? "page" : undefined}
        >
          <span className="bottombar-icon">
            <Icon name={icon} />
          </span>
          <span className="bottombar-label">{label}</span>
        </a>
      ))}
    </nav>
  )
}

export function DeckListScreen({ lang }: { lang: Lang }) {
  const t = text[lang]
  return (
    <>
      <header className="appbar">
        <div className="appbar-titles">
          <p className="appbar-date">{t.date}</p>
          <h1 className="appbar-title">{t.decks}</h1>
        </div>
        <div className="appbar-actions">
          <Button
            variant="ghost"
            size="icon"
            className="iconbtn"
            aria-label={t.sync}
          >
            <Icon name="sync" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="iconbtn"
            aria-label={t.more}
          >
            <Icon name="more_vert" />
          </Button>
        </div>
      </header>

      <Card className="today">
        <div className="today-text">
          <p className="today-main">{t.today}</p>
          <p className="today-sub">{t.studied}</p>
        </div>
        <Progress
          className="today-progress"
          value={(totals.studied / (totals.studied + totals.all)) * 100}
          aria-label={t.studied}
        />
      </Card>

      <div className="colhead" aria-hidden="true">
        <span>{t.new}</span>
        <span>{t.learning}</span>
        <span>{t.due}</span>
      </div>
      <Separator className="colhead-rule" />

      <div className="decks">
        {decks.map((deck) => (
          <div className="deck-group" key={deck.name}>
            <DeckRow deck={deck} lang={lang} />
            {deck.expanded &&
              deck.children?.map((child, i) => (
                <DeckRow
                  key={child.name}
                  deck={child}
                  lang={lang}
                  depth={1}
                  active={i === 0}
                />
              ))}
          </div>
        ))}
      </div>

      <Button className="fab" aria-label={t.add}>
        <Icon name="add" />
        <span className="fab-label">{t.add}</span>
      </Button>

      <BottomBar lang={lang} />
    </>
  )
}

export function StudyScreen({ lang, side }: { lang: Lang; side: Side }) {
  const t = text[lang]
  const answers = [
    ["again", t.again],
    ["hard", t.hard],
    ["good", t.good],
    ["easy", t.easy],
  ] as const
  return (
    <>
      <header className="studybar">
        <Button
          variant="ghost"
          size="icon"
          className="iconbtn"
          aria-label="Back"
        >
          <Icon name="arrow_back" />
        </Button>
        <div className="studybar-titles">
          <p className="studybar-deck">
            {session.deckPath.map((part, i) => (
              <span key={part.name} lang={part.nameLang}>
                {i > 0 && <span className="studybar-sep"> › </span>}
                {part.name}
              </span>
            ))}
          </p>
          <CountCells counts={session.remaining} lang={lang} />
        </div>
        <Button
          variant="ghost"
          size="icon"
          className="iconbtn"
          aria-label="Undo"
        >
          <Icon name="undo" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="iconbtn"
          aria-label={t.more}
        >
          <Icon name="more_vert" />
        </Button>
      </header>
      <Progress
        className="study-progress"
        value={(session.done / session.total) * 100}
        aria-label={t.left}
      />

      {/* Card Content is drawn as its Card Template styles it, not in the
          Direction's typefaces: Anki's default template is centred 20px text
          on white. Only the Card Frame around it belongs to the Direction. */}
      <Card className="sheet">
        <div className="card-content" lang="zh-CN">
          <div>{session.question}</div>
          {side === "answer" && (
            <>
              <hr />
              <div lang="en">{session.answer}</div>
            </>
          )}
        </div>
      </Card>

      {side === "question" ? (
        <div className="answer-area">
          <Button className="show-answer">{t.showAnswer}</Button>
        </div>
      ) : (
        <div className="answer-area answer-grid">
          {answers.map(([kind, label], i) => (
            <Button
              key={kind}
              variant="outline"
              className={`answer answer-${kind}`}
            >
              <span className="answer-time">{t.times[i]}</span>
              <span className="answer-label">{label}</span>
            </Button>
          ))}
        </div>
      )}
    </>
  )
}
