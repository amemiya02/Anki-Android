import type { CSSProperties, ReactNode } from "react"

import type { Lang, Theme } from "../data"
import { text } from "../data"
import { Phone } from "../screens"

import {
  cards,
  editorText,
  findText,
  flagOrder,
  previewText,
  type SampleCard,
} from "./data"
import flags from "./flags.json"
import "./find.css"

// ---------- Small shared pieces (copies of the Core Loop's, kept local so
// this Mockup never changes the shared screens) ----------

function Icon({ name, className = "" }: { name: string; className?: string }) {
  return (
    <span className={`msym ${className}`} aria-hidden="true">
      {name}
    </span>
  )
}

function IconButton({
  icon,
  label,
  className = "",
}: {
  icon: string
  label: string
  className?: string
}) {
  return (
    <button
      type="button"
      className={`f-iconbtn ${className}`}
      aria-label={label}
    >
      <Icon name={icon} />
    </button>
  )
}

type Tab = "decks" | "browse" | "stats" | "more"

function BottomBar({ lang, current }: { lang: Lang; current: Tab }) {
  const t = text[lang]
  const items = [
    ["stacks", t.decks, "decks"],
    ["search", t.browse, "browse"],
    ["bar_chart", t.stats, "stats"],
    ["more_horiz", t.more, "more"],
  ] as const
  return (
    <nav className="bottombar" aria-label="Primary">
      {items.map(([icon, label, key]) => (
        <a
          key={icon}
          className={`bottombar-item ${key === current ? "is-current" : ""}`}
          aria-current={key === current ? "page" : undefined}
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

function AddButton({ lang }: { lang: Lang }) {
  return (
    <button type="button" className="fab" aria-label={findText[lang].add}>
      <Icon name="add" />
    </button>
  )
}

/** A phone in Quiet paper, with the proposed flag colours for its theme. */
export function FindPhone({
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
  const flagVars = Object.fromEntries(
    Object.entries(flags[theme]).map(([name, hex]) => [`--flag-${name}`, hex])
  ) as CSSProperties
  return (
    <Phone
      direction="paper"
      lang={lang}
      screen="decks"
      theme={theme}
      label={label}
    >
      <div className="f-contents" style={flagVars}>
        {children}
      </div>
    </Phone>
  )
}

// ---------- Card browser ----------

function BrowseHeader({ lang }: { lang: Lang }) {
  const f = findText[lang]
  return (
    <header className="f-browse-head">
      <h1 className="appbar-title">{f.browse}</h1>
      <IconButton icon="more_vert" label={f.more} />
    </header>
  )
}

function SearchField({
  lang,
  query,
  focused = false,
}: {
  lang: Lang
  query?: string
  focused?: boolean
}) {
  const f = findText[lang]
  return (
    <div className={`f-search ${focused ? "is-focused" : ""}`} role="search">
      {!focused && <Icon name="search" />}
      <span className={`f-search-text ${query ? "f-query" : "f-search-hint"}`}>
        {query ?? f.searchHint}
        {focused && <span className="f-caret" />}
      </span>
      {query && (
        <IconButton icon="close" label={f.clear} className="is-muted" />
      )}
    </div>
  )
}

function Chips({ lang, active = false }: { lang: Lang; active?: boolean }) {
  const f = findText[lang]
  // When a query names a Deck or a state, the chips show it (finishing
  // Upstream's search view, which left them empty).
  const chips: [string, boolean][] = active
    ? [
        ["中文", true],
        [lang === "zh" ? "待复习" : "Due", true],
        [f.flags, false],
        [f.tags, false],
      ]
    : [
        [f.allDecks, false],
        [f.cardState, false],
        [f.flags, false],
        [f.tags, false],
      ]
  return (
    <div className="f-chips">
      {chips.map(([label, on]) => (
        <span key={label} className={`f-chip ${on ? "is-on" : ""}`}>
          {on && <Icon name="check" />}
          <span>{label}</span>
          <Icon name="arrow_drop_down" />
        </span>
      ))}
    </div>
  )
}

function ResultHead({ lang }: { lang: Lang }) {
  const f = findText[lang]
  return (
    <div className="f-resulthead">
      <span className="f-resultcount">{f.resultCount}</span>
      <span className="f-sortbtn">
        <Icon name="arrow_upward" />
        {f.sortDue}
      </span>
      <span className="f-seg" role="radiogroup">
        <span className="is-on">{f.cardsMode}</span>
        <span>{f.notesMode}</span>
      </span>
    </div>
  )
}

function RowMarks({ card, lang }: { card: SampleCard; lang: Lang }) {
  const f = findText[lang]
  return (
    <span className="f-row-marks">
      {card.suspended && <span className="f-row-status">{f.suspended}</span>}
      {card.buried && <span className="f-row-status">{f.buried}</span>}
      {card.marked && (
        <span aria-label={f.marked}>
          <Icon name="bookmark" className="f-row-mark-muted f-flag" />
        </span>
      )}
      {card.flag && (
        <span aria-label={`${f.flag}: ${f.flagNames[card.flag]}`}>
          <Icon name="flag" className={`f-flag f-flag-${card.flag}`} />
        </span>
      )}
    </span>
  )
}

function ResultRow({
  card,
  lang,
  selecting = false,
  selected = false,
}: {
  card: SampleCard
  lang: Lang
  selecting?: boolean
  selected?: boolean
}) {
  const dim = card.suspended || card.buried
  return (
    <div
      className={`f-row ${dim ? "is-dim" : ""} ${selected ? "is-selected" : ""}`}
    >
      {selecting && (
        <span className={`f-check ${selected ? "is-on" : ""}`}>
          {selected && <Icon name="check" />}
        </span>
      )}
      <span className="f-row-main">
        <span className="f-row-title" lang={card.frontLang}>
          {card.front}
        </span>
        <span className="f-row-sub">
          {card.card[lang]}
          <span className="f-row-sep">·</span>
          {card.due[lang]}
          <span className="f-row-sep">·</span>
          <span lang="zh">{card.deck[lang]}</span>
        </span>
      </span>
      <RowMarks card={card} lang={lang} />
    </div>
  )
}

function ResultTable({ lang }: { lang: Lang }) {
  const f = findText[lang]
  return (
    <div className="f-table">
      <div className="f-table-row f-table-head">
        <span />
        <span>{f.colSortField}</span>
        <span>{f.colCard}</span>
        <span>{f.colDue}</span>
        <span>{f.colDeck}</span>
      </div>
      {cards.map((card) => (
        <div
          key={card.front}
          className={`f-table-row ${card.suspended || card.buried ? "is-dim" : ""}`}
        >
          <span
            className={`f-table-flag ${card.flag ? `f-flag-${card.flag}` : ""}`}
          />
          <span lang={card.frontLang}>{card.front}</span>
          <span>{card.card[lang]}</span>
          <span>{card.due[lang]}</span>
          <span lang="zh">{card.deck[lang]}</span>
        </div>
      ))}
    </div>
  )
}

export type RowStyle = "lines" | "table"

/** Browse: the results of a search, the way Browse opens. */
export function BrowseScreen({
  lang,
  rows = "lines",
  menu = false,
}: {
  lang: Lang
  rows?: RowStyle
  menu?: boolean
}) {
  return (
    <>
      <BrowseHeader lang={lang} />
      <SearchField lang={lang} />
      <Chips lang={lang} />
      <ResultHead lang={lang} />
      <div className="f-rule" />
      {rows === "lines" ? (
        <div className="f-results">
          {cards.map((card) => (
            <ResultRow key={card.front} card={card} lang={lang} />
          ))}
        </div>
      ) : (
        <ResultTable lang={lang} />
      )}
      <AddButton lang={lang} />
      <BottomBar lang={lang} current="browse" />
      {menu && <BrowseMenu lang={lang} />}
    </>
  )
}

function BrowseMenu({ lang }: { lang: Lang }) {
  const f = findText[lang]
  return (
    <div className="f-menu" role="menu">
      {f.menuItems.map(([icon, label], i) => (
        <div key={icon}>
          {(i === 2 || i === 5) && <div className="f-menu-rule" />}
          <div className="f-menu-row" role="menuitem">
            <Icon name={icon} />
            <span>{label}</span>
          </div>
        </div>
      ))}
    </div>
  )
}

export type SearchHelp = "manual" | "builder"

/** The search page: Browse's search view, finished. */
export function SearchScreen({
  lang,
  help = "manual",
}: {
  lang: Lang
  help?: SearchHelp
}) {
  const f = findText[lang]
  const recent = (
    <>
      <div className="f-section">
        <span>{f.recent}</span>
        <span className="f-section-action">{f.seeMore}</span>
      </div>
      <div>
        {f.recentItems.map((q) => (
          <div key={q} className="f-qrow">
            <Icon name="history" />
            <span className="f-qrow-text">
              <span className="f-qrow-title f-query" lang="zh">
                {q}
              </span>
            </span>
            <Icon name="north_west" className="f-qrow-trailing" />
          </div>
        ))}
      </div>
    </>
  )
  const saved = (
    <>
      <div className="f-section">
        <span>{f.saved}</span>
        <span className="f-section-action">{f.manage}</span>
      </div>
      <div>
        {f.savedItems.map(([name, q]) => (
          <div key={q} className="f-qrow">
            <Icon name="bookmark" />
            <span className="f-qrow-text">
              <span className="f-qrow-title">{name}</span>
              <span className="f-qrow-sub f-query" lang="zh">
                {q}
              </span>
            </span>
          </div>
        ))}
      </div>
    </>
  )
  return (
    <>
      <div className="f-searchbar">
        <IconButton icon="arrow_back" label={f.back} />
        <SearchField lang={lang} query={f.searchQuery} focused />
      </div>
      <Chips lang={lang} active />
      <div className="f-searchpage">
        {recent}
        {help === "builder" ? (
          <>
            <div className="f-section">
              <span>{f.syntaxSection}</span>
            </div>
            <div>
              {f.syntaxRows.map(([name, example]) => (
                <div key={name} className="f-qrow">
                  <Icon name="add_circle" />
                  <span className="f-qrow-text">
                    <span className="f-qrow-title">{name}</span>
                    <span className="f-qrow-sub f-query" lang="zh">
                      {example}
                    </span>
                  </span>
                  <Icon name="chevron_right" className="f-qrow-trailing" />
                </div>
              ))}
            </div>
          </>
        ) : (
          <>
            {saved}
            <div className="f-section" />
            <div>
              <div className="f-qrow">
                <Icon name="help" />
                <span className="f-qrow-text">
                  <span className="f-qrow-title">{f.syntaxHelp}</span>
                  <span className="f-qrow-sub">{f.syntaxHelpSub}</span>
                </span>
                <Icon name="open_in_new" className="f-qrow-trailing" />
              </div>
            </div>
          </>
        )}
      </div>
    </>
  )
}

function Sheet({ children }: { children: ReactNode }) {
  return (
    <>
      <div className="f-scrim" />
      <div className="f-sheet" role="dialog">
        <span className="f-handle" />
        {children}
      </div>
    </>
  )
}

/** The Flags chip's sheet: any of several flags, finished as multi-select. */
export function FlagsSheetScreen({ lang }: { lang: Lang }) {
  const f = findText[lang]
  const on = new Set(["red", "blue"])
  return (
    <>
      <BrowseScreen lang={lang} />
      <Sheet>
        <div className="f-sheet-head">
          <div>
            <p className="f-sheet-title">{f.flagsTitle}</p>
            <p className="f-sheet-sub">{f.flagsSub}</p>
          </div>
        </div>
        <div className="f-sheet-list">
          <div className="f-sheet-row">
            <span className="f-check" />
            <Icon name="outlined_flag" className="f-row-mark-muted" />
            <span className="f-grow">{f.noFlag}</span>
          </div>
          {flagOrder.map((flag) => (
            <div key={flag} className="f-sheet-row">
              <span className={`f-check ${on.has(flag) ? "is-on" : ""}`}>
                {on.has(flag) && <Icon name="check" />}
              </span>
              <Icon name="flag" className={`f-flag f-flag-${flag}`} />
              <span className="f-grow">{f.flagNames[flag]}</span>
            </div>
          ))}
          <div
            className="f-sheet-row"
            style={{ borderTop: "1px solid var(--rule)" }}
          >
            <Icon name="edit" className="f-row-mark-muted" />
            <span className="f-grow">{f.renameFlags}</span>
          </div>
        </div>
        <button type="button" className="f-primary" style={{ marginTop: 12 }}>
          {f.done}
        </button>
      </Sheet>
    </>
  )
}

/** The sort sheet, opened from the sort button on the result line. */
export function SortSheetScreen({ lang }: { lang: Lang }) {
  const f = findText[lang]
  return (
    <>
      <BrowseScreen lang={lang} />
      <Sheet>
        <div className="f-sheet-head">
          <p className="f-sheet-title">{f.sortTitle}</p>
        </div>
        <div className="f-sheet-list">
          <div className="f-sheet-row">
            <span className="f-radio" />
            <span className="f-grow">{f.noSorting}</span>
            <span className="f-sheet-row-sub">{f.noSortingSub}</span>
          </div>
          {f.sortColumns.map(([name, direction], i) => (
            <div key={name}>
              <div className="f-sheet-row">
                <span className={`f-radio ${i === 1 ? "is-on" : ""}`} />
                <span
                  className="f-grow"
                  style={i === 1 ? { fontWeight: 600 } : {}}
                >
                  {name}
                </span>
                {i !== 1 && (
                  <span className="f-sheet-row-sub">{direction}</span>
                )}
              </div>
              {i === 1 && (
                <div style={{ padding: "0 0 10px 38px" }}>
                  <span className="f-seg">
                    <span className="is-on">{f.ascending}</span>
                    <span>{f.descending}</span>
                  </span>
                </div>
              )}
            </div>
          ))}
        </div>
        <p className="f-sheet-sub" style={{ marginTop: 10 }}>
          {f.sortUnavailable}
        </p>
      </Sheet>
    </>
  )
}

/** Selecting: tick boxes on every row, and the common actions at the bottom. */
export function SelectScreen({
  lang,
  sheet = false,
}: {
  lang: Lang
  sheet?: boolean
}) {
  const f = findText[lang]
  const selected = new Set([0, 1, 4])
  const actions = [
    ["drive_file_move", f.changeDeck],
    ["flag", f.flagAction],
    ["sell", f.tagsAction],
    ["pause_circle", f.suspend],
    ["more_horiz", f.moreActions],
  ] as const
  return (
    <>
      <header className="f-bar">
        <IconButton icon="close" label={f.close} />
        <h1 className="f-bar-title">{f.selected}</h1>
        <IconButton icon="select_all" label={f.selectAll} />
        <IconButton icon="more_vert" label={f.more} />
      </header>
      <SearchField lang={lang} />
      <Chips lang={lang} />
      <ResultHead lang={lang} />
      <div className="f-rule" />
      <div className="f-results">
        {cards.map((card, i) => (
          <ResultRow
            key={card.front}
            card={card}
            lang={lang}
            selecting
            selected={selected.has(i)}
          />
        ))}
      </div>
      <nav className="f-selectbar" aria-label={f.selected}>
        {actions.map(([icon, label]) => (
          <span key={icon}>
            <Icon name={icon} />
            {label}
          </span>
        ))}
      </nav>
      {sheet && (
        <Sheet>
          <div className="f-sheet-head">
            <p className="f-sheet-title">{f.actionsTitle}</p>
          </div>
          <div className="f-sheet-list">
            {f.actionGroups.map((group, g) => (
              <div key={g} className="f-sheet-group">
                {group.map(([icon, label, sub]) => (
                  <div
                    key={icon}
                    className={`f-sheet-row ${icon === "delete" ? "f-danger" : ""}`}
                  >
                    <Icon
                      name={icon}
                      className={icon === "delete" ? "" : "f-row-mark-muted"}
                    />
                    <span className="f-grow">{label}</span>
                    {sub && <span className="f-sheet-row-sub">{sub}</span>}
                  </div>
                ))}
              </div>
            ))}
          </div>
        </Sheet>
      )}
    </>
  )
}

/** Columns: which columns the rows show, and in what order. */
export function ColumnsScreen({ lang }: { lang: Lang }) {
  const f = findText[lang]
  return (
    <>
      <header className="f-bar">
        <IconButton icon="arrow_back" label={f.back} />
        <h1 className="f-bar-title">{f.columnsTitle}</h1>
        <span className="f-seg" style={{ marginRight: 14 }}>
          <span className="is-on">{f.cardsMode}</span>
          <span>{f.notesMode}</span>
        </span>
      </header>
      <div className="f-page">
        <p className="f-lead">{f.columnsLead}</p>
        <div className="f-section" style={{ paddingTop: 0 }}>
          <span>{f.shown}</span>
        </div>
        <div style={{ borderTop: "1px solid var(--rule)" }}>
          {f.columnsShown.map(([name, sub], i) => (
            <div key={name} className={`f-colrow ${i === 0 ? "is-title" : ""}`}>
              <Icon name="drag_indicator" />
              <span className="f-grow">
                <span>{name}</span>
                {sub && <span className="f-colrow-sub">{sub}</span>}
              </span>
              <Icon name="remove_circle_outline" />
            </div>
          ))}
        </div>
        <div className="f-section">
          <span>{f.available}</span>
        </div>
        <div style={{ borderTop: "1px solid var(--rule)" }}>
          {f.columnsAvailable.map((name) => (
            <div key={name} className="f-colrow">
              <Icon name="add_circle_outline" className="f-colrow-add" />
              <span className="f-grow">{name}</span>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}

// ---------- Note editor ----------

function Keyboard() {
  const rows = [10, 9, 7]
  return (
    <div className="f-keyboard" aria-hidden="true">
      {rows.map((n, r) => (
        <div key={r} className="f-keyrow">
          {Array.from({ length: n }, (_, i) => (
            <span key={i} />
          ))}
        </div>
      ))}
      <div className="f-keyrow">
        <span />
        <span />
        <span className="is-wide" />
        <span />
        <span />
      </div>
    </div>
  )
}

function FormatToolbar({
  lang,
  cloze = false,
}: {
  lang: Lang
  cloze?: boolean
}) {
  const e = editorText[lang]
  return (
    <div className="f-toolbar" role="toolbar">
      {cloze && (
        <>
          <span className="f-toolbar-cloze" aria-label={e.clozeNew}>
            [c+]
          </span>
          <span className="f-toolbar-cloze" aria-label={e.clozeSame}>
            [c]
          </span>
          <span className="f-toolbar-sep" />
        </>
      )}
      <IconButton icon="format_bold" label={e.bold} />
      <IconButton icon="format_italic" label={e.italic} />
      <IconButton icon="format_underlined" label={e.underline} />
      <IconButton icon="functions" label={e.mathjax} />
      <IconButton icon="title" label={e.heading} />
      <IconButton icon="format_size" label={e.fontSize} />
      <IconButton icon="horizontal_rule" label={e.rule} />
      <span className="f-toolbar-sep" />
      <span className="f-toolbar-custom" aria-label={e.custom}>
        1
      </span>
      <span className="f-toolbar-custom" aria-label={e.custom}>
        2
      </span>
      <IconButton icon="add" label={e.addCustom} />
    </div>
  )
}

function Field({
  label,
  value,
  lang,
  keep,
  focused = false,
  warning,
  cloze = false,
}: {
  label: string
  value: string
  lang: Lang
  /** add mode: the pin that keeps the text for the next Note */
  keep?: boolean
  focused?: boolean
  warning?: ReactNode
  cloze?: boolean
}) {
  const e = editorText[lang]
  const parts = cloze ? value.split(/(\{\{c1::[^}]*\}\})/) : [value]
  return (
    <div>
      <div className="f-field-head">
        <span className="f-field-label">{label}</span>
        {keep !== undefined && (
          <IconButton
            icon="keep"
            label={e.keep}
            className={keep ? "is-on" : ""}
          />
        )}
        <IconButton icon="attach_file" label={e.attach} />
      </div>
      <div
        className={`f-field ${focused ? "is-focused" : ""} ${warning ? "is-warning" : ""}`}
        lang="zh"
      >
        {parts.map((p, i) =>
          p.startsWith("{{") ? (
            <span key={i} className="f-cloze">
              {p}
            </span>
          ) : (
            <span key={i}>{p}</span>
          )
        )}
        {focused && <span className="f-caret" />}
      </div>
      {warning && (
        <p className="f-field-note">
          <Icon name="warning" />
          {warning}
        </p>
      )}
    </div>
  )
}

export type SavePlace = "bar" | "bottom"

/** The note editor, adding or editing a Note. */
export function EditorScreen({
  lang,
  mode = "add",
  save = "bar",
  cloze = false,
  duplicate = false,
  keyboard = false,
  menu = false,
  attach = false,
}: {
  lang: Lang
  mode?: "add" | "edit"
  save?: SavePlace
  cloze?: boolean
  duplicate?: boolean
  keyboard?: boolean
  menu?: boolean
  attach?: boolean
}) {
  const e = editorText[lang]
  const adding = mode === "add"
  return (
    <>
      <header className="f-bar">
        <IconButton
          icon={adding ? "close" : "arrow_back"}
          label={adding ? e.close : e.back}
        />
        <h1 className="f-bar-title">{adding ? e.addTitle : e.editTitle}</h1>
        <IconButton icon="visibility" label={e.preview} />
        {save === "bar" && (
          <button type="button" className="f-save">
            {adding ? e.add : e.save}
          </button>
        )}
        <IconButton icon="more_vert" label={e.more} />
      </header>
      <div className="f-editor">
        <div className="f-pickers">
          <div className="f-picker">
            <span className="f-picker-text">
              <span className="f-picker-label">{e.noteType}</span>
              <span className="f-picker-value">
                {cloze ? e.noteTypeCloze : e.noteTypeBasic}
              </span>
            </span>
            <Icon name={adding ? "arrow_drop_down" : "swap_horiz"} />
          </div>
          <div className="f-picker">
            <span className="f-picker-text">
              <span className="f-picker-label">{e.deck}</span>
              <span className="f-picker-value" lang="zh">
                {e.deckName}
              </span>
            </span>
            <Icon name="arrow_drop_down" />
          </div>
        </div>
        <div className="f-fields">
          {cloze ? (
            <>
              <Field
                label={e.fieldText}
                value={e.clozeValue}
                lang={lang}
                keep={adding ? false : undefined}
                focused
                cloze
              />
              <Field
                label={e.fieldExtra}
                value={e.clozeExtra}
                lang={lang}
                keep={adding ? false : undefined}
              />
            </>
          ) : (
            <>
              <Field
                label={e.fieldFront}
                value={e.frontValue}
                lang={lang}
                keep={adding ? false : undefined}
                focused={!duplicate && !attach}
                warning={
                  duplicate ? (
                    <>
                      {e.duplicate}
                      <a>{e.showDuplicates}</a>
                    </>
                  ) : undefined
                }
              />
              <Field
                label={e.fieldBack}
                value={e.backValue}
                lang={lang}
                keep={adding ? false : undefined}
                focused={attach}
              />
            </>
          )}
        </div>
        {!keyboard && (
          <>
            <div className="f-meta">
              <Icon name="sell" />
              <span className="f-meta-body">
                <span className="f-meta-label">{e.tags}</span>
                {e.tagValues.map((tag) => (
                  <span key={tag} className="f-tag">
                    {tag}
                  </span>
                ))}
              </span>
              <Icon name="add" className="f-meta-trailing" />
            </div>
            <div className="f-meta">
              <Icon name="style" />
              <span className="f-meta-body">
                <span className="f-meta-label">{e.cards}</span>
                {cloze ? e.cardsValueCloze : e.cardsValue}
              </span>
              <Icon name="chevron_right" className="f-meta-trailing" />
            </div>
          </>
        )}
      </div>
      {save === "bottom" && (
        <div className="f-bottom-add">
          <button type="button" className="f-primary">
            {adding ? e.add : e.save}
          </button>
        </div>
      )}
      <FormatToolbar lang={lang} cloze={cloze} />
      {keyboard ? <Keyboard /> : <div style={{ height: 22, flex: "none" }} />}
      {menu && (
        <div className="f-menu" role="menu" style={{ top: 52 }}>
          {e.menuEdit.map(([icon, label], i) => (
            <div key={icon}>
              {i === 2 && <div className="f-menu-rule" />}
              <div className="f-menu-row" role="menuitem">
                <Icon name={icon} />
                <span style={{ flex: 1 }}>{label}</span>
                {icon === "text_fields" && (
                  <span className="f-check is-on">
                    <Icon name="check" />
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
      {attach && (
        <Sheet>
          <div className="f-sheet-head">
            <p className="f-sheet-title">{e.attachTitle}</p>
          </div>
          <div className="f-sheet-list">
            {e.attachItems.map(([icon, label, sub]) => (
              <div key={icon} className="f-sheet-row" style={{ minHeight: 56 }}>
                <Icon name={icon} className="f-row-mark-muted" />
                <span className="f-grow">{label}</span>
                {sub && <span className="f-sheet-row-sub">{sub}</span>}
              </div>
            ))}
          </div>
        </Sheet>
      )}
    </>
  )
}

// ---------- Previewer ----------

/** The previewer: the study screen's Card Frame, with browsing tools. */
export function PreviewScreen({
  lang,
  side,
}: {
  lang: Lang
  side: "question" | "answer"
}) {
  const p = previewText[lang]
  const card = cards[0]
  const at = 3 / 25
  return (
    <>
      <header className="studybar f-preview-bar">
        <IconButton icon="arrow_back" label={p.back} />
        <div className="f-bar-titles">
          <p className="studybar-deck" lang="zh">
            {card.deck[lang]}
          </p>
          <p className="f-bar-sub">{p.position}</p>
        </div>
        <button type="button" className="f-iconbtn" aria-label={p.flag}>
          <Icon name="flag" className="f-flag f-flag-red" />
        </button>
        <IconButton icon="bookmark" label={p.mark} />
        <IconButton icon="edit" label={p.edit} />
        <IconButton icon="more_vert" label={p.more} />
      </header>
      <div
        className="f-slider"
        role="slider"
        aria-valuenow={3}
        aria-valuemax={25}
      >
        <span className="f-slider-track" />
        <span className="f-slider-fill" style={{ width: `${at * 100}%` }} />
        <span className="f-slider-thumb" style={{ left: `${at * 100}%` }} />
      </div>
      <div
        className="sheet f-sheet-card"
        style={{ display: "flex", flexDirection: "column" }}
      >
        <div className="card-content" lang="zh-CN">
          <div>{card.front}</div>
          {side === "answer" && (
            <>
              <hr />
              <div lang="en">{card.back}</div>
            </>
          )}
        </div>
      </div>
      <div className="f-preview-actions">
        <button type="button" className="f-outline-btn" aria-label={p.previous}>
          <Icon name="chevron_left" />
        </button>
        <button type="button" className="f-primary">
          {side === "question" ? p.showAnswer : p.next}
          {side === "answer" && <Icon name="chevron_right" />}
        </button>
      </div>
    </>
  )
}

/** The note editor's preview: one tab per Card the Note makes. */
export function TemplatePreviewScreen({ lang }: { lang: Lang }) {
  const p = previewText[lang]
  return (
    <>
      <header className="f-bar">
        <IconButton icon="arrow_back" label={p.back} />
        <h1 className="f-bar-title">{p.templatesTitle}</h1>
      </header>
      <div className="f-tabs" role="tablist">
        <span className="f-tab is-current" role="tab">
          {p.templates[0]}
        </span>
        <span className="f-tab" role="tab">
          {p.templates[1]}
        </span>
      </div>
      <div className="sheet" style={{ marginTop: 14 }}>
        <div className="card-content" lang="zh-CN">
          <div>熟悉</div>
        </div>
      </div>
      <div className="f-preview-actions">
        <button type="button" className="f-primary">
          {p.showAnswer}
        </button>
      </div>
    </>
  )
}
