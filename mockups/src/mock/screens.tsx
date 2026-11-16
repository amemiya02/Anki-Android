import type { CSSProperties, ReactNode } from "react"

import values from "../../../docs/redesign/quiet-paper.values.json"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Separator } from "@/components/ui/separator"

import {
  decks,
  longDecks,
  longTotals,
  session,
  text,
  totals,
  type ButtonPlace,
  type Counts,
  type CountStyle,
  type Deck,
  type Direction,
  type FinishedState,
  type Lang,
  type Side,
  type StudyBarStyle,
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

/** Option B for the counts: one total, with the kinds named under the Deck. */
function CountBreakdown({ counts, lang }: { counts: Counts; lang: Lang }) {
  const t = text[lang]
  const kinds = [
    ["new", t.new],
    ["learn", t.learning],
    ["due", t.due],
  ] as const
  return (
    <span className="deck-breakdown">
      {kinds
        .map(([kind, label], i) => ({ kind, label, n: counts[i] }))
        .filter(({ n }) => n > 0)
        .map(({ kind, label, n }, i) => (
          <span key={kind}>
            {i > 0 && <span className="breakdown-sep"> · </span>}
            <span className={`breakdown-${kind}`}>{t.breakdown(n, label)}</span>
          </span>
        ))}
    </span>
  )
}

function DeckRow({
  deck,
  lang,
  depth = 0,
  active = false,
  countStyle = "columns",
}: {
  deck: Deck
  lang: Lang
  depth?: number
  active?: boolean
  countStyle?: CountStyle
}) {
  const t = text[lang]
  const finished = deck.counts.every((n) => n === 0)
  const hasChildren = !!deck.children?.length
  const total = deck.counts.reduce((a, b) => a + b, 0)
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
      {countStyle === "total" ? (
        <span className="deck-main">
          <span className="deck-name" lang={deck.nameLang}>
            {deck.name}
          </span>
          {!finished && <CountBreakdown counts={deck.counts} lang={lang} />}
        </span>
      ) : (
        <span className="deck-name" lang={deck.nameLang}>
          {deck.name}
        </span>
      )}
      {finished ? (
        <span className="deck-done">
          <Icon name="check_circle" />
          {t.done}
        </span>
      ) : countStyle === "total" ? (
        <span className="deck-total">{total}</span>
      ) : (
        <CountCells counts={deck.counts} lang={lang} />
      )}
    </div>
  )
}

type Tab = "decks" | "browse" | "stats" | "more"

function BottomBar({ lang, current = "decks" }: { lang: Lang; current?: Tab }) {
  const t = text[lang]
  const items = [
    ["stacks", t.decks, current === "decks"],
    ["search", t.browse, current === "browse"],
    ["bar_chart", t.stats, current === "stats"],
    ["more_horiz", t.more, current === "more"],
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

function DeckListBar({ lang }: { lang: Lang }) {
  const t = text[lang]
  return (
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
  )
}

function AddButton({ lang }: { lang: Lang }) {
  const t = text[lang]
  return (
    <Button className="fab" aria-label={t.add}>
      <Icon name="add" />
      <span className="fab-label">{t.add}</span>
    </Button>
  )
}

export function DeckListScreen({
  lang,
  variant = "full",
  countStyle = "columns",
}: {
  lang: Lang
  /** full: the sample Decks; long: long Chinese and Japanese names; empty: a Newcomer */
  variant?: "full" | "long" | "empty"
  countStyle?: CountStyle
}) {
  const t = text[lang]
  if (variant === "empty") return <EmptyDeckList lang={lang} />
  const list = variant === "long" ? longDecks : decks
  const studied = variant === "long" ? longTotals.studied : totals.studied
  const all = variant === "long" ? longTotals.all : totals.all
  return (
    <>
      <DeckListBar lang={lang} />

      <Card className="today">
        <div className="today-text">
          <p className="today-main">
            {variant === "long" ? t.todayLong : t.today}
          </p>
          <p className="today-sub">
            {variant === "long" ? t.studiedLong : t.studied}
          </p>
        </div>
        <Progress
          className="today-progress"
          value={(studied / (studied + all)) * 100}
          aria-label={t.studied}
        />
      </Card>

      {countStyle === "columns" ? (
        <div className="colhead" aria-hidden="true">
          <span>{t.new}</span>
          <span>{t.learning}</span>
          <span>{t.due}</span>
        </div>
      ) : (
        <div className="colhead colhead-total" aria-hidden="true">
          <span>{t.todayColumn}</span>
        </div>
      )}
      <Separator className="colhead-rule" />

      <div className="decks">
        {list.map((deck) => (
          <div className="deck-group" key={deck.name}>
            <DeckRow deck={deck} lang={lang} countStyle={countStyle} />
            {deck.expanded &&
              deck.children?.map((child, i) => (
                <DeckRow
                  key={child.name}
                  deck={child}
                  lang={lang}
                  depth={1}
                  active={i === 0}
                  countStyle={countStyle}
                />
              ))}
          </div>
        ))}
      </div>

      <AddButton lang={lang} />
      <BottomBar lang={lang} />
    </>
  )
}

/** Two paper cards drawn in hairline ink, echoing the launcher icon. */
function EmptyMark() {
  return (
    <svg className="empty-mark" viewBox="0 0 96 96" aria-hidden="true">
      <rect
        x="30"
        y="18"
        width="40"
        height="54"
        rx="4"
        transform="rotate(9 50 45)"
        className="mark-back"
      />
      <g transform="rotate(-7 46 48)">
        <rect
          x="24"
          y="22"
          width="40"
          height="54"
          rx="4"
          className="mark-front"
        />
        <rect
          x="32"
          y="33"
          width="24"
          height="2.5"
          rx="1.25"
          className="mark-ink"
        />
        <rect
          x="32"
          y="41"
          width="15"
          height="2.5"
          rx="1.25"
          className="mark-ink"
        />
        <circle cx="56" cy="66" r="3.4" className="mark-seal" />
      </g>
    </svg>
  )
}

function ListRow({
  icon,
  title,
  sub,
  trailing = "chevron_right",
  emphasis = false,
}: {
  icon: string
  title: string
  sub?: string
  trailing?: string
  emphasis?: boolean
}) {
  return (
    <a className={`list-row ${emphasis ? "list-row-emphasis" : ""}`}>
      <span className="list-row-icon">
        <Icon name={icon} />
      </span>
      <span className="list-row-text">
        <span className="list-row-title">{title}</span>
        {sub && <span className="list-row-sub">{sub}</span>}
      </span>
      <span className="list-row-trailing">
        <Icon name={trailing} />
      </span>
    </a>
  )
}

function EmptyDeckList({ lang }: { lang: Lang }) {
  const t = text[lang]
  return (
    <>
      <DeckListBar lang={lang} />
      <div className="empty">
        <EmptyMark />
        <h2 className="empty-title">{t.emptyTitle}</h2>
        <p className="empty-body">{t.emptyBody}</p>
        <Button className="primary-btn">
          <Icon name="download" />
          {t.getShared}
        </Button>
        <div className="list">
          <ListRow
            icon="file_open"
            title={t.importFile}
            sub={t.importFileSub}
          />
          <ListRow
            icon="create_new_folder"
            title={t.createDeck}
            sub={t.createDeckSub}
          />
        </div>
      </div>
      <AddButton lang={lang} />
      <BottomBar lang={lang} />
    </>
  )
}

/** More: settings, help and support, which the side drawer held before. */
export function MoreScreen({ lang }: { lang: Lang }) {
  const t = text[lang]
  return (
    <>
      <header className="appbar appbar-plain">
        <h1 className="appbar-title">{t.more}</h1>
      </header>
      <div className="list list-page">
        <ListRow icon="settings" title={t.settings} sub={t.settingsSub} />
        <ListRow
          icon="help"
          title={t.help}
          sub={t.helpSub}
          trailing="open_in_new"
        />
        <ListRow
          icon="volunteer_activism"
          title={t.support}
          sub={t.supportSub}
          trailing="open_in_new"
        />
      </div>
      <p className="more-footer">
        <span className="more-name">Hayaki</span>
        <span>{t.builtOn}</span>
      </p>
      <BottomBar lang={lang} current="more" />
    </>
  )
}

function DeckPath() {
  return (
    <>
      {session.deckPath.map((part, i) => (
        <span key={part.name} lang={part.nameLang}>
          {i > 0 && <span className="studybar-sep"> › </span>}
          {part.name}
        </span>
      ))}
    </>
  )
}

export function StudyScreen({
  lang,
  side,
  bar = "full",
  buttons = "bottom",
}: {
  lang: Lang
  side: Side
  /** full: Deck title, counts and time-remaining estimate; minimal: counts only */
  bar?: StudyBarStyle
  buttons?: ButtonPlace
}) {
  const t = text[lang]
  const answers = [
    ["again", t.again],
    ["hard", t.hard],
    ["good", t.good],
    ["easy", t.easy],
  ] as const
  const answerArea =
    side === "question" ? (
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
    )
  return (
    <>
      <header className={`studybar studybar-${bar}`}>
        <Button
          variant="ghost"
          size="icon"
          className="iconbtn"
          aria-label={t.back}
        >
          <Icon name="arrow_back" />
        </Button>
        <div className="studybar-titles">
          {bar === "full" && (
            <p className="studybar-deck">
              <DeckPath />
            </p>
          )}
          <p className="studybar-line">
            <CountCells counts={session.remaining} lang={lang} />
            {bar === "full" && (
              <span className="studybar-estimate">{t.estimate}</span>
            )}
          </p>
        </div>
        <Button
          variant="ghost"
          size="icon"
          className="iconbtn"
          aria-label={t.undo}
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

      {buttons === "top" && <div className="answer-top">{answerArea}</div>}

      {/* Card Content is drawn as its Card Template styles it, not in the
          Direction's typefaces: Anki's default template is centred 20px text
          on white. Only the Card Frame around it belongs to the Direction. */}
      <Card className={`sheet ${buttons === "top" ? "sheet-last" : ""}`}>
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

      {buttons === "bottom" && answerArea}
    </>
  )
}

/** The finished screen, in each state the Engine can report. */
export function FinishedScreen({
  lang,
  state,
}: {
  lang: Lang
  state: FinishedState
}) {
  const t = text[lang]
  const f = t.finished[state]
  return (
    <>
      <header className="studybar">
        <Button
          variant="ghost"
          size="icon"
          className="iconbtn"
          aria-label={t.back}
        >
          <Icon name="arrow_back" />
        </Button>
        <div className="studybar-titles">
          <p className="studybar-deck">
            <DeckPath />
          </p>
        </div>
      </header>

      <div className="finished">
        <span className={`finished-mark finished-mark-${state}`}>
          <Icon name={f.icon} />
        </span>
        <h1 className="finished-title">{f.title}</h1>
        <p className="finished-body">{f.body}</p>
        {state === "buried" && (
          <Button variant="outline" className="secondary-btn">
            <Icon name="replay" />
            {t.unbury}
          </Button>
        )}
        <div className="list">
          <ListRow
            icon="auto_stories"
            title={t.customStudy}
            sub={t.customStudySub}
            emphasis={state === "limit"}
          />
          <ListRow icon="tune" title={t.deckOptions} sub={t.deckOptionsSub} />
        </div>
        <p className="finished-today">{t.studiedToday}</p>
      </div>

      <div className="answer-area">
        <Button className="show-answer">{t.backToDecks}</Button>
      </div>
    </>
  )
}
