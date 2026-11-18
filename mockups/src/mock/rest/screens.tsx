// Mockups of the rest of the app (ticket 41): Note Types, the Card Template
// editor, sync setup, the first-run screen, home-screen widgets and
// notifications, in Quiet paper. Shared pieces from ../screens.tsx are used
// read-only; everything here is drawn with the same classes and values.

import type { ReactNode } from "react"

import type { Lang } from "../data"
import { DeckListScreen } from "../screens"
import { rest, type RestText } from "./text"

const tx = (lang: Lang): RestText => rest[lang]

export function Icon({
  name,
  className = "",
}: {
  name: string
  className?: string
}) {
  return (
    <span className={`msym ${className}`} aria-hidden="true">
      {name}
    </span>
  )
}

function IconButton({ icon, label }: { icon: string; label: string }) {
  return (
    <button type="button" className="r-iconbtn" aria-label={label}>
      <Icon name={icon} />
    </button>
  )
}

/** A page reached from another: back, then the page title below it. */
function PageHead({
  lang,
  title,
  lead,
  actions = [],
}: {
  lang: Lang
  title: string
  lead?: ReactNode
  actions?: [string, string][]
}) {
  const t = tx(lang)
  return (
    <header className="r-pagehead">
      <div className="r-pagehead-bar">
        <IconButton icon="arrow_back" label={t.back} />
        <span className="r-spacer" />
        {actions.map(([icon, label]) => (
          <IconButton key={icon} icon={icon} label={label} />
        ))}
      </div>
      {title && <h1 className="r-pagetitle">{title}</h1>}
      {lead && <p className="r-lead">{lead}</p>}
    </header>
  )
}

/** A compact bar for working screens, where the content needs the room. */
function CompactBar({
  lang,
  title,
  sub,
  subLang,
  actions = [],
}: {
  lang: Lang
  title: string
  sub?: string
  subLang?: string
  actions?: [string, string][]
}) {
  const t = tx(lang)
  return (
    <header className="r-compact">
      <IconButton icon="arrow_back" label={t.back} />
      <div className="r-compact-titles">
        <p className="r-compact-title">{title}</p>
        {sub && (
          <p className="r-compact-sub" lang={subLang}>
            {sub}
          </p>
        )}
      </div>
      {actions.map(([icon, label]) => (
        <IconButton key={icon} icon={icon} label={label} />
      ))}
    </header>
  )
}

function Row({
  icon,
  title,
  sub,
  trailing = "chevron_right",
  emphasis = false,
  lead,
  titleLang,
  value,
}: {
  icon?: string
  title: ReactNode
  sub?: ReactNode
  trailing?: string | ReactNode
  emphasis?: boolean
  lead?: ReactNode
  titleLang?: string
  value?: string
}) {
  return (
    <div className={`r-row ${emphasis ? "r-row-emphasis" : ""}`}>
      {lead}
      {icon && (
        <span className="r-row-icon">
          <Icon name={icon} />
        </span>
      )}
      <span className="r-row-text">
        <span className="r-row-title" lang={titleLang}>
          {title}
        </span>
        {sub && <span className="r-row-sub">{sub}</span>}
      </span>
      {value && <span className="r-row-value">{value}</span>}
      {typeof trailing === "string" ? (
        trailing && (
          <span className="r-row-trailing">
            <Icon name={trailing} />
          </span>
        )
      ) : (
        <span className="r-row-trailing">{trailing}</span>
      )}
    </div>
  )
}

function Switch({ on, label }: { on: boolean; label: string }) {
  return (
    <span
      className={`r-switch ${on ? "is-on" : ""}`}
      role="switch"
      aria-checked={on}
      aria-label={label}
    />
  )
}

function SectionHead({
  children,
  action,
}: {
  children: ReactNode
  action?: ReactNode
}) {
  return (
    <div className="r-section">
      <h2>{children}</h2>
      {action}
    </div>
  )
}

function Primary({ children, icon }: { children: ReactNode; icon?: string }) {
  return (
    <button type="button" className="r-primary">
      {icon && <Icon name={icon} />}
      {children}
    </button>
  )
}

function Secondary({ children }: { children: ReactNode }) {
  return (
    <button type="button" className="r-secondary">
      {children}
    </button>
  )
}

function TextButton({
  children,
  icon,
}: {
  children: ReactNode
  icon?: string
}) {
  return (
    <button type="button" className="r-textbtn">
      {icon && <Icon name={icon} />}
      {children}
    </button>
  )
}

function Fab({ label }: { label: string }) {
  return (
    <button type="button" className="r-fab" aria-label={label}>
      <Icon name="add" />
    </button>
  )
}

/** A bottom sheet over a dimmed screen. */
function Sheet({
  behind,
  title,
  titleLang,
  sub,
  children,
}: {
  behind: ReactNode
  title: string
  titleLang?: string
  sub?: string
  children: ReactNode
}) {
  return (
    <>
      {behind}
      <div className="r-scrim" />
      <section className="r-sheet" role="dialog" aria-label={title}>
        <span className="r-sheet-handle" />
        <h2 className="r-sheet-title" lang={titleLang}>
          {title}
        </h2>
        {sub && <p className="r-sheet-sub">{sub}</p>}
        {children}
      </section>
    </>
  )
}

/** A centred dialog over a dimmed screen. */
function Dialog({
  behind,
  icon,
  title,
  children,
}: {
  behind: ReactNode
  icon?: string
  title: string
  children: ReactNode
}) {
  return (
    <>
      {behind}
      <div className="r-scrim" />
      <section className="r-dialog" role="alertdialog" aria-label={title}>
        {icon && (
          <span className="r-dialog-mark">
            <Icon name={icon} />
          </span>
        )}
        <h2 className="r-dialog-title">{title}</h2>
        {children}
      </section>
    </>
  )
}

/** Two paper cards in hairline ink, as on the empty deck list and the icon. */
function Mark({ size = 96 }: { size?: number }) {
  return (
    <svg
      className="r-mark"
      width={size}
      height={size}
      viewBox="0 0 96 96"
      aria-hidden="true"
    >
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

// ======================================================================
// Note Types
// ======================================================================

// Stock Note Types carry the names the Engine gave them in the collection's
// language; the last two are the person's own.
const noteTypesFor = (lang: Lang) => {
  const s = tx(lang).stock
  const l = lang === "zh" ? "zh-CN" : "en"
  return [
    { name: s[0], lang: l, notes: "2,318", templates: 1 },
    { name: s[1], lang: l, notes: "412", templates: 2 },
    { name: s[4], lang: l, notes: "96", templates: 1 },
    { name: s[5], lang: l, notes: "14", templates: 1 },
    { name: "汉字 HSK", lang: "zh-CN", notes: "1,530", templates: 2 },
    { name: "日本語 語彙", lang: "ja", notes: "1,204", templates: 2 },
  ]
}

/** Option A: a row opens the Note Type's own page. */
export function NoteTypesScreen({
  lang,
  style = "page",
}: {
  lang: Lang
  /** page: a row opens the Note Type's page (A); buttons: today's row buttons (B) */
  style?: "page" | "buttons"
}) {
  const t = tx(lang)
  return (
    <>
      <PageHead
        lang={lang}
        title={t.noteTypes}
        lead={t.noteTypesLead}
        actions={[["search", t.search]]}
      />
      <div className="r-list r-list-page">
        {noteTypesFor(lang).map((n) => (
          <Row
            key={n.name}
            title={n.name}
            titleLang={n.lang}
            sub={
              style === "page"
                ? `${t.notes(n.notes)} · ${t.templatesN(n.templates)}`
                : t.notes(n.notes)
            }
            trailing={
              style === "page" ? (
                "chevron_right"
              ) : (
                <span className="r-row-buttons">
                  <IconButton icon="style" label={t.editCards} />
                  <IconButton icon="more_vert" label={t.more} />
                </span>
              )
            }
          />
        ))}
      </div>
      <Fab label={t.addNoteType} />
    </>
  )
}

export function AddNoteTypeScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  const standard = t.stock
  return (
    <Sheet
      behind={<NoteTypesScreen lang={lang} />}
      title={t.addNoteType}
      sub={t.addNoteTypeLead}
    >
      <p className="r-sheet-label">{t.standard}</p>
      <div className="r-choices" role="radiogroup">
        {standard.map((s, i) => (
          <span
            key={s}
            className={`r-choice ${i === 1 ? "is-on" : ""}`}
            role="radio"
            aria-checked={i === 1}
          >
            {s}
          </span>
        ))}
      </div>
      <p className="r-sheet-label">{t.yours}</p>
      <div className="r-choices">
        <span className="r-choice" lang="zh-CN">
          汉字 HSK
        </span>
        <span className="r-choice" lang="ja">
          日本語 語彙
        </span>
      </div>
      <label className="r-field">
        <span className="r-field-label">{t.name}</span>
        <span className="r-field-box">
          {t.stock[1]} 2<span className="r-caret" />
        </span>
      </label>
      <div className="r-sheet-buttons">
        <Secondary>{t.cancel}</Secondary>
        <Primary>{t.add}</Primary>
      </div>
    </Sheet>
  )
}

const fields = [
  { name: "Expression", sort: true, kb: "日本語" },
  { name: "Reading", kb: "日本語" },
  { name: "Meaning" },
  { name: "Example" },
  { name: "Audio" },
]

/** The Note Type's page: its fields, then its Card Templates (option A). */
export function NoteTypeScreen({
  lang,
  templates = true,
  active,
}: {
  lang: Lang
  /** false: today's field editor alone (option B) */
  templates?: boolean
  active?: string
}) {
  const t = tx(lang)
  return (
    <>
      {templates ? (
        <PageHead lang={lang} title="" actions={[["more_vert", t.more]]} />
      ) : (
        <PageHead lang={lang} title="" />
      )}
      <div className="r-nt-head">
        <p className="r-overline">{templates ? t.noteType : t.fields}</p>
        <h1 className="r-pagetitle r-pagetitle-tight" lang="ja">
          日本語 語彙
        </h1>
        <p className="r-caption">
          {t.notes("1,204")} · {t.templatesN(2)}
        </p>
      </div>
      <div className="r-scroll">
        <SectionHead action={<TextButton icon="add">{t.addField}</TextButton>}>
          {t.fields}
        </SectionHead>
        <div className="r-list">
          {fields.map((f) => (
            <Row
              key={f.name}
              lead={
                <span className="r-handle" aria-label={t.dragToReorder}>
                  <Icon name="drag_indicator" />
                </span>
              }
              title={f.name}
              titleLang="en"
              emphasis={active === f.name}
              sub={
                f.sort || f.kb ? (
                  <>
                    {f.sort && (
                      <span className="r-tag">
                        <Icon name="sort" />
                        {t.sortField}
                      </span>
                    )}
                    {f.sort && f.kb && " · "}
                    {f.kb && t.keyboard(f.kb)}
                  </>
                ) : undefined
              }
              trailing="more_vert"
            />
          ))}
        </div>
        {templates && (
          <>
            <SectionHead>{t.cardTemplates}</SectionHead>
            <div className="r-list">
              <Row
                icon="style"
                title="Recognition"
                titleLang="en"
                sub="Expression › Reading, Meaning"
              />
              <Row
                icon="style"
                title="Recall"
                titleLang="en"
                sub="Meaning › Expression, Reading"
              />
            </div>
          </>
        )}
      </div>
    </>
  )
}

export function FieldActionsScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  const a = t.fieldActions
  return (
    <Sheet
      behind={<NoteTypeScreen lang={lang} active="Reading" />}
      title="Reading"
      titleLang="en"
    >
      <div className="r-list r-list-sheet">
        <Row icon="edit" title={a.rename} trailing="" />
        <Row icon="swap_vert" title={a.move} trailing="" />
        <Row icon="sort" title={a.sort} sub={a.sortSub} trailing="" />
        <Row icon="keyboard" title={a.language} value={a.languageSub} />
        <Row
          icon="delete"
          title={a.delete}
          sub={a.deleteSub}
          trailing=""
          emphasis
        />
      </div>
    </Sheet>
  )
}

export function FullSyncScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <Dialog
      behind={<NoteTypeScreen lang={lang} active="Example" />}
      icon="sync_problem"
      title={t.fullSyncTitle}
    >
      <p className="r-dialog-body">{t.fullSyncBody}</p>
      <div className="r-dialog-buttons">
        <TextButton>{t.cancel}</TextButton>
        <button type="button" className="r-primary r-primary-small">
          {t.fullSyncConfirm}
        </button>
      </div>
    </Dialog>
  )
}

// ======================================================================
// Card Template editor
// ======================================================================

type Part = "front" | "back" | "styling"

const code: Record<Part, string[]> = {
  front: [
    '<div class="expression">{{Expression}}</div>',
    "",
    "{{#Audio}}",
    '  <div class="audio">{{Audio}}</div>',
    "{{/Audio}}",
  ],
  back: [
    "{{FrontSide}}",
    "",
    '<hr id="answer">',
    '<div class="reading">{{furigana:Reading}}</div>',
    '<div class="meaning">{{Meaning}}</div>',
    "",
    "{{#Example}}",
    '  <p class="example">{{Example}}</p>',
    "{{/Example}}",
  ],
  styling: [
    ".card {",
    "  font-family: 'Klee One', serif;",
    "  font-size: 22px;",
    "  text-align: center;",
    "}",
    "",
    ".expression { font-size: 44px; }",
    ".reading { color: #675F53; }",
  ],
}

/** Code with the fields picked out, so a Card Template reads at a glance. */
function CodeLine({ line }: { line: string }) {
  const parts = line.split(/(\{\{[^}]+\}\})/g)
  return (
    <>
      {parts.map((p, i) =>
        p.startsWith("{{") ? (
          <span key={i} className="r-code-field">
            {p}
          </span>
        ) : (
          <span key={i}>{p}</span>
        )
      )}
    </>
  )
}

export function TemplateScreen({
  lang,
  part = "back",
}: {
  lang: Lang
  part?: Part
}) {
  const t = tx(lang)
  const parts: [Part, string][] = [
    ["front", t.front],
    ["back", t.backSide],
    ["styling", t.styling],
  ]
  return (
    <>
      <CompactBar
        lang={lang}
        title={t.cardTemplates}
        sub="日本語 語彙"
        subLang="ja"
        actions={[
          ["visibility", t.preview],
          ["check", t.save],
          ["more_vert", t.more],
        ]}
      />
      <nav className="r-tabs" aria-label={t.cardTemplates}>
        <span className="r-tab is-current" lang="en">
          Recognition
        </span>
        <span className="r-tab" lang="en">
          Recall
        </span>
      </nav>
      <div className="r-segments" role="tablist">
        {parts.map(([k, label]) => (
          <span
            key={k}
            role="tab"
            aria-selected={k === part}
            className={`r-segment ${k === part ? "is-current" : ""}`}
          >
            {label}
          </span>
        ))}
      </div>
      <div className="r-code" lang="en">
        {code[part].map((line, i) => (
          <div key={i} className="r-code-line">
            <span className="r-code-no">{i + 1}</span>
            <span className="r-code-text">
              <CodeLine line={line} />
              {i === code[part].length - 1 && <span className="r-caret" />}
            </span>
          </div>
        ))}
      </div>
      <div className="r-insert" aria-label={t.insertField}>
        <span className="r-insert-label">
          <Icon name="data_object" />
          {t.insertField}
        </span>
        <span className="r-chips" lang="en">
          {["Expression", "Reading", "Meaning", "Example", "Audio"].map((f) => (
            <span key={f} className="r-chip">
              {f}
            </span>
          ))}
        </span>
      </div>
      <Keyboard />
    </>
  )
}

/** A plain stand-in for the on-screen keyboard. */
function Keyboard() {
  const rows = ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
  return (
    <div className="r-keyboard" aria-hidden="true">
      {rows.map((r) => (
        <div key={r} className="r-keyrow">
          {[...r].map((k) => (
            <span key={k} className="r-key">
              {k}
            </span>
          ))}
        </div>
      ))}
      <div className="r-keyrow">
        <span className="r-key r-key-wide">?123</span>
        <span className="r-key r-key-space" />
        <span className="r-key r-key-wide">⏎</span>
      </div>
    </div>
  )
}

export function TemplateMoreScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  const m = t.templateMore
  return (
    <Sheet
      behind={<TemplateScreen lang={lang} />}
      title="Recognition"
      titleLang="en"
    >
      <div className="r-list r-list-sheet r-list-dense">
        <Row icon="add" title={m.add} sub={m.addSub} trailing="" />
        <Row icon="edit" title={m.rename} trailing="" />
        <Row icon="swap_vert" title={m.reposition} trailing="" />
        <Row
          icon="move_to_inbox"
          title={m.deckOverride}
          sub={m.deckOverrideSub}
          trailing=""
        />
        <Row icon="table_rows" title={m.browser} sub={m.browserSub} />
        <Row icon="content_copy" title={m.copy} trailing="" />
        <Row icon="restart_alt" title={m.restore} trailing="" />
        <Row
          icon="delete"
          title={m.delete}
          sub={m.deleteSub}
          trailing=""
          emphasis
        />
      </div>
    </Sheet>
  )
}

export function TemplatePreviewScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <>
      <CompactBar
        lang={lang}
        title={t.previewTitle}
        sub="日本語 語彙"
        subLang="ja"
      />
      <nav className="r-tabs" aria-label={t.cardTemplates}>
        <span className="r-tab is-current" lang="en">
          Recognition
        </span>
        <span className="r-tab" lang="en">
          Recall
        </span>
      </nav>
      {/* Card Content, as the study screen draws it: the Card Template's own
          styling on the card sheet. */}
      <div className="r-sheetcard">
        <div className="r-cardcontent" lang="ja">
          <div className="r-cc-expression">熟語</div>
          <hr />
          <div className="r-cc-reading">じゅくご</div>
          <div className="r-cc-meaning" lang="en">
            idiom; compound word
          </div>
        </div>
      </div>
      <p className="r-preview-note">{t.sampleNote}</p>
      <div className="r-bottom">
        <Secondary>{t.hideAnswer}</Secondary>
      </div>
    </>
  )
}

// ======================================================================
// Sync
// ======================================================================

function Steps({ lang, n }: { lang: Lang; n: 1 | 2 }) {
  const t = tx(lang)
  return (
    <p className="r-steps">
      <span className={`r-step ${n >= 1 ? "is-on" : ""}`} />
      <span className={`r-step ${n >= 2 ? "is-on" : ""}`} />
      <span>{t.step(n)}</span>
    </p>
  )
}

export function SyncServerScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <>
      <PageHead lang={lang} title={t.syncServer} />
      <div className="r-form">
        <Steps lang={lang} n={1} />
        <p className="r-lead r-lead-flush">{t.serverLead}</p>
        <label className="r-field">
          <span className="r-field-label">{t.serverAddress}</span>
          <span className="r-field-box is-focused" lang="en">
            https://anki.example.home:8080/
            <span className="r-caret" />
          </span>
          <span className="r-field-help">{t.serverHelp}</span>
        </label>
        <div className="r-list">
          <Row
            icon="verified_user"
            title={t.certificate}
            sub={t.certificateSub}
            value={t.optional}
          />
          <Row icon="menu_book" title={t.learnMore} trailing="open_in_new" />
          <Row icon="cloud_off" title={t.fromAnkiWeb} sub={t.fromAnkiWebSub} />
        </div>
      </div>
      <div className="r-bottom">
        <Primary>{t.continue}</Primary>
      </div>
    </>
  )
}

export function SyncSignInScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <>
      <PageHead lang={lang} title={t.signInTo} />
      <div className="r-form">
        <Steps lang={lang} n={2} />
        <p className="r-server">
          <Icon name="dns" />
          <span lang="en">anki.example.home:8080</span>
          <TextButton>{t.changeServer}</TextButton>
        </p>
        <label className="r-field">
          <span className="r-field-label">{t.username}</span>
          <span className="r-field-box" lang="en">
            rin
          </span>
        </label>
        <label className="r-field">
          <span className="r-field-label">{t.password}</span>
          <span className="r-field-box is-focused">
            <span className="r-dots">••••••••••</span>
            <span className="r-caret" />
            <span className="r-field-end">
              <Icon name="visibility" />
            </span>
          </span>
          <span className="r-field-help">{t.passwordNote}</span>
        </label>
      </div>
      <div className="r-bottom">
        <Primary>{t.signIn}</Primary>
      </div>
    </>
  )
}

function SyncStatus({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <div className="r-status">
      <span className="r-status-mark">
        <Icon name="cloud_done" />
      </span>
      <div className="r-status-text">
        <p className="r-caption">{t.signedInAs}</p>
        <p className="r-status-user" lang="en">
          rin
        </p>
        <p className="r-caption" lang="en">
          https://anki.example.home:8080/
        </p>
      </div>
    </div>
  )
}

/** Sync: who is signed in, to which server, and how syncing behaves. */
export function SyncAccountScreen({
  lang,
  style = "one",
}: {
  lang: Lang
  /** one: account and sync settings on one page (A); split: account only (B) */
  style?: "one" | "split"
}) {
  const t = tx(lang)
  return (
    <>
      <PageHead lang={lang} title={t.sync} />
      <div className="r-scroll r-scroll-pad">
        <SyncStatus lang={lang} />
        <p className="r-caption r-lastsync">{t.lastSync}</p>
        <Primary icon="sync">{t.syncNow}</Primary>
        <div className="r-list r-gap">
          <Row icon="dns" title={t.changeServer} />
          <Row
            icon="logout"
            title={t.signOut}
            sub={t.signOutSub}
            trailing=""
            emphasis
          />
        </div>
        {style === "one" ? (
          <>
            <SectionHead>{t.whenToSync}</SectionHead>
            <div className="r-list">
              <Row
                title={t.autoSync}
                sub={t.autoSyncSub}
                trailing={<Switch on label={t.autoSync} />}
              />
              <Row
                title={t.syncStatus}
                trailing={<Switch on label={t.syncStatus} />}
              />
              <Row
                title={t.metered}
                sub={t.meteredSub}
                trailing={<Switch on={false} label={t.metered} />}
              />
              <Row title={t.media} value={t.mediaValue} />
            </div>
            <SectionHead>{t.advanced}</SectionHead>
            <div className="r-list">
              <Row title={t.timeout} value={t.timeoutValue} />
              <Row title={t.certificate} value={t.optional} />
              <Row title={t.oneWay} sub={t.oneWaySub} trailing="" />
            </div>
          </>
        ) : (
          <div className="r-list r-gap">
            <Row icon="tune" title={t.syncSettings} sub={t.syncSettingsSub} />
          </div>
        )}
      </div>
    </>
  )
}

export function SyncErrorScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <Dialog
      behind={<DeckListScreen lang={lang} />}
      icon="cloud_off"
      title={t.errorTitle}
    >
      <p className="r-dialog-body">{t.errorBody}</p>
      <div className="r-dialog-buttons">
        <TextButton>{t.notNow}</TextButton>
        <button type="button" className="r-primary r-primary-small">
          {t.tryAgain}
        </button>
      </div>
    </Dialog>
  )
}

export function SyncConflictScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <Dialog
      behind={<DeckListScreen lang={lang} />}
      icon="call_split"
      title={t.conflictTitle}
    >
      <p className="r-dialog-body">{t.conflictBody}</p>
      <div className="r-list r-list-dialog">
        <Row
          icon="smartphone"
          title={t.keepPhone}
          sub={
            <>
              {t.keepPhoneSub}
              <br />
              {t.lastChanged(lang === "zh" ? "今天 09:40" : "today, 09:40")}
            </>
          }
        />
        <Row
          icon="dns"
          title={t.keepServer}
          sub={
            <>
              {t.keepServerSub}
              <br />
              {t.lastChanged(lang === "zh" ? "昨天 22:05" : "yesterday, 22:05")}
            </>
          }
        />
      </div>
      <div className="r-dialog-buttons">
        <TextButton>{t.cancel}</TextButton>
      </div>
    </Dialog>
  )
}

// ======================================================================
// First run
// ======================================================================

export function WelcomeScreen({
  lang,
  style = "one",
}: {
  lang: Lang
  /** one: one primary button and two rows (A); three: three equal rows (B) */
  style?: "one" | "three"
}) {
  const t = tx(lang)
  return (
    <>
      <div className="r-welcome">
        <Mark size={88} />
        <h1 className="r-welcome-title">{t.welcomeTitle}</h1>
        <p className="r-lead r-lead-flush">{t.welcomeLead}</p>
        {style === "one" ? (
          <>
            <Primary>{t.startEmpty}</Primary>
            <div className="r-list">
              <Row
                icon="file_open"
                title={t.importColl}
                sub={t.importCollSub}
              />
              <Row icon="dns" title={t.setUpSync} sub={t.setUpSyncSub} />
            </div>
          </>
        ) : (
          <div className="r-list">
            <Row icon="add_circle" title={t.startEmpty} sub={t.startEmptySub} />
            <Row icon="file_open" title={t.importColl} sub={t.importCollSub} />
            <Row icon="dns" title={t.setUpSync} sub={t.setUpSyncSub} />
          </div>
        )}
        <TextButton icon="cloud_off">{t.fromAnkiWeb}</TextButton>
      </div>
      <p className="r-welcome-foot">{t.unofficial}</p>
    </>
  )
}

export function AnkiWebScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <>
      <PageHead lang={lang} title={t.ankiWebTitle} lead={t.ankiWebLead} />
      <ol className="r-howto">
        {t.ankiWebSteps.map(([title, body], i) => (
          <li key={title}>
            <span className="r-howto-n">{i + 1}</span>
            <span className="r-row-text">
              <span className="r-row-title">{title}</span>
              <span className="r-howto-body">{body}</span>
            </span>
          </li>
        ))}
      </ol>
      <div className="r-bottom">
        <Primary icon="file_open">{t.importFile}</Primary>
      </div>
    </>
  )
}

// ======================================================================
// Home-screen widgets
// ======================================================================

type C3 = [number, number, number]
const widgetDecks: { name: string; lang: string; c: C3 }[] = [
  { name: "日本語", lang: "ja", c: [12, 3, 11] },
  { name: "中文", lang: "zh-CN", c: [8, 2, 6] },
  { name: "English", lang: "en", c: [7, 0, 9] },
  { name: "Anatomy", lang: "en", c: [0, 0, 0] },
  { name: "N3 語彙", lang: "ja", c: [6, 3, 7] },
]

function WCounts({ c }: { c: C3 }) {
  return (
    <span className="w-counts">
      {(["new", "learn", "due"] as const).map((k, i) => (
        <span
          key={k}
          className={`w-count w-count-${k} ${c[i] === 0 ? "is-zero" : ""}`}
        >
          {c[i]}
        </span>
      ))}
    </span>
  )
}

function SmallWidget({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <div className="w w-1x1 w-small">
      <span className="w-small-n">26</span>
      <span className="w-small-l">{t.widgetDue}</span>
      <span className="w-small-m">{t.widgetMin}</span>
    </div>
  )
}

function AddNoteWidget({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <div className="w w-1x1 w-add">
      <span className="w-add-btn">
        <Icon name="add" />
      </span>
      <span className="w-add-l">{t.addNote}</span>
    </div>
  )
}

function DeckListWidget({ lang, rows = 4 }: { lang: Lang; rows?: number }) {
  const t = tx(lang)
  return (
    <div className={`w w-4x${rows <= 1 ? 1 : rows >= 5 ? 3 : 2} w-decks`}>
      <div className="w-decks-head">
        <span className="w-decks-title">Hayaki</span>
        <span className="w-decks-sum">{t.widgetDecks}</span>
      </div>
      {rows > 1 && (
        <div className="w-decks-list">
          {widgetDecks.slice(0, rows).map((d) => (
            <div key={d.name} className="w-deck">
              <span className="w-deck-name" lang={d.lang}>
                {d.name}
              </span>
              <WCounts c={d.c} />
            </div>
          ))}
        </div>
      )}
      {rows <= 1 && (
        <div className="w-deck w-deck-only">
          <span className="w-deck-name" lang="ja">
            日本語
          </span>
          <WCounts c={[12, 3, 11]} />
        </div>
      )}
    </div>
  )
}

function OneDeckWidget({
  lang,
  size = "2x2",
  missing = false,
}: {
  lang: Lang
  size?: "2x2" | "2x1"
  missing?: boolean
}) {
  const t = tx(lang)
  if (missing)
    return (
      <div className={`w w-${size} w-one w-missing`}>
        <Icon name="error" />
        <span className="w-one-name">{t.missingDeck}</span>
        <span className="w-one-sub">{t.missingDeckSub}</span>
      </div>
    )
  return (
    <div className={`w w-${size} w-one`}>
      <span className="w-one-name" lang="zh-CN">
        中文 › HSK 4
      </span>
      {size === "2x2" ? (
        <div className="w-one-grid">
          {(
            [
              ["new", t.new, 8],
              ["learn", t.learning, 2],
              ["due", t.due, 6],
            ] as const
          ).map(([k, label, n]) => (
            <span key={k} className="w-one-cell">
              <span className={`w-one-n w-count-${k}`}>{n}</span>
              <span className="w-one-l">{label}</span>
            </span>
          ))}
        </div>
      ) : (
        <WCounts c={[8, 2, 6]} />
      )}
    </div>
  )
}

function HomeIcons({ n }: { n: number }) {
  return (
    <div className="home-icons" aria-hidden="true">
      {Array.from({ length: n }, (_, i) => (
        <span key={i} className="home-icon" />
      ))}
    </div>
  )
}

/** A home screen holding every widget the app has. */
export function WidgetsScreen({ lang }: { lang: Lang }) {
  return (
    <div className="home">
      <p className="home-clock">9:41</p>
      <DeckListWidget lang={lang} rows={4} />
      <div className="home-row">
        <OneDeckWidget lang={lang} />
        <div className="home-col">
          <div className="home-row">
            <SmallWidget lang={lang} />
            <AddNoteWidget lang={lang} />
          </div>
          <HomeIcons n={2} />
        </div>
      </div>
      <div className="home-dock">
        <HomeIcons n={4} />
      </div>
    </div>
  )
}

/** The resizable widgets at their other sizes, and a Deck that is gone. */
export function WidgetSizesScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <div className="home">
      <p className="home-label">{t.sizes.deckSmall}</p>
      <DeckListWidget lang={lang} rows={1} />
      <p className="home-label">{t.sizes.deckLarge}</p>
      <DeckListWidget lang={lang} rows={5} />
      <div className="home-row">
        <div className="home-col">
          <p className="home-label">{t.sizes.oneSmall}</p>
          <OneDeckWidget lang={lang} size="2x1" />
        </div>
        <div className="home-col">
          <p className="home-label">{t.sizes.oneEmpty}</p>
          <OneDeckWidget lang={lang} size="2x1" missing />
        </div>
      </div>
    </div>
  )
}

export function WidgetConfigScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <>
      <PageHead
        lang={lang}
        title={t.widgetConfigTitle}
        lead={t.widgetConfigLead}
      />
      <p className="r-caption r-pad">{t.chosen(4)}</p>
      <div className="r-list r-list-page">
        {widgetDecks.slice(0, 4).map((d) => (
          <Row
            key={d.name}
            lead={
              <span className="r-handle" aria-label={t.dragToReorder}>
                <Icon name="drag_indicator" />
              </span>
            }
            title={d.name}
            titleLang={d.lang}
            trailing={<IconButton icon="close" label={t.remove} />}
          />
        ))}
        <Row icon="add" title={t.addDeck} trailing="" emphasis />
      </div>
      <div className="r-bottom">
        <Primary>{t.save}</Primary>
      </div>
    </>
  )
}

// ======================================================================
// Notifications
// ======================================================================

function Notice({
  lang,
  title,
  body,
  when,
  actions = [],
  progress,
  titleLang,
}: {
  lang: Lang
  title: string
  body: string
  when: string
  actions?: string[]
  progress?: number
  titleLang?: string
}) {
  void lang
  return (
    <div className="n-card">
      <div className="n-head">
        <span className="n-icon">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect
              x="6"
              y="4.5"
              width="10"
              height="14"
              rx="1.5"
              transform="rotate(-7 11 11.5)"
            />
            <circle cx="15" cy="16.4" r="1.6" />
          </svg>
        </span>
        <span className="n-app">Hayaki</span>
        <span className="n-when">· {when}</span>
      </div>
      <p className="n-title" lang={titleLang}>
        {title}
      </p>
      <p className="n-body">{body}</p>
      {progress !== undefined && (
        <span className="n-progress">
          <span style={{ width: `${progress}%` }} />
        </span>
      )}
      {actions.length > 0 && (
        <div className="n-actions">
          {actions.map((a) => (
            <span key={a}>{a}</span>
          ))}
        </div>
      )}
    </div>
  )
}

/** The notification shade, drawn as Android draws it: only the app's mark,
 *  name, accent colour and wording are the Fork's. */
export function NotificationsScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <div className="shade">
      <p className="shade-clock">9:41</p>
      <p className="shade-date">
        {lang === "zh" ? "11月16日 星期一" : "Monday, 16 November"}
      </p>
      <div className="shade-tiles" aria-hidden="true">
        {["wifi", "bluetooth", "do_not_disturb_on", "flashlight_on"].map(
          (i) => (
            <span key={i} className="shade-tile">
              <Icon name={i} />
            </span>
          )
        )}
      </div>
      <Notice
        lang={lang}
        title={t.remindTitle}
        body={t.remindBody}
        when={t.now}
        actions={[t.snooze5, t.snooze1h]}
      />
      <Notice
        lang={lang}
        title={t.remindDeckTitle}
        body={t.remindDeckBody}
        when={t.min(20)}
        actions={[t.snooze5, t.snooze1h]}
      />
      <Notice
        lang={lang}
        title={t.syncingTitle}
        body={t.syncingBody}
        when={t.now}
        progress={64}
        actions={[t.cancel]}
      />
      <Notice
        lang={lang}
        title={t.syncFailTitle}
        body={t.syncFailBody}
        when={t.min(45)}
      />
    </div>
  )
}

const reminders = [
  { time: "08:00", deck: "all", sub: "threshold", on: true },
  { time: "12:30", deck: "日本語", lang: "ja", sub: "none", on: true },
  { time: "21:30", deck: "all", sub: "notStudied", on: false },
  { time: "19:00", deck: "gone", sub: "none", on: true },
]

export function RemindersScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <>
      <PageHead lang={lang} title={t.reminders} lead={t.remindersLead} />
      <div className="r-list r-list-page">
        {reminders.map((r) => (
          <div
            key={r.time}
            className={`r-row r-reminder ${r.deck === "gone" ? "is-gone" : ""} ${r.on ? "" : "is-off"}`}
          >
            <span className="r-row-text">
              <span className="r-time">{r.time}</span>
              <span className="r-row-sub">
                <span lang={r.lang}>
                  {r.deck === "all"
                    ? t.allDecks
                    : r.deck === "gone"
                      ? t.deckGone
                      : r.deck}
                </span>
                {r.sub === "threshold" && ` · ${t.whenAtLeast(10)}`}
                {r.sub === "notStudied" && ` · ${t.onlyNotStudied}`}
              </span>
            </span>
            <Switch on={r.on && r.deck !== "gone"} label={r.time} />
          </div>
        ))}
      </div>
      <div className="r-list r-list-page r-gap">
        <Row
          icon="health_and_safety"
          title={t.checkReminders}
          sub={t.checkRemindersSub}
        />
        <Row
          icon="notifications"
          title={t.systemSettings}
          sub={t.systemSettingsSub}
          trailing="open_in_new"
        />
      </div>
      <Fab label={t.addReminder} />
    </>
  )
}

export function ReminderEditScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <Sheet behind={<RemindersScreen lang={lang} />} title={t.editTitle}>
      <div className="r-list r-list-sheet">
        <Row title={t.time} value="08:00" trailing="schedule" />
        <Row title={t.deck} value={t.allDecks} />
      </div>
      <p className="r-sheet-label">{t.threshold}</p>
      <div className="r-threshold">
        <span>{t.atLeast}</span>
        <span className="r-stepper">
          <Icon name="remove" />
          <span className="r-stepper-n">10</span>
          <Icon name="add" />
        </span>
        <span>{t.cards}</span>
      </div>
      <div className="r-countthese">
        <span className="r-caption">{t.countThese}</span>
        <span className="r-filter is-on">
          <Icon name="check" />
          {t.new}
        </span>
        <span className="r-filter is-on">
          <Icon name="check" />
          {t.learning}
        </span>
        <span className="r-filter is-on">
          <Icon name="check" />
          {t.due}
        </span>
      </div>
      <div className="r-check">
        <span className="r-checkbox" role="checkbox" aria-checked="false" />
        {t.notStudied}
      </div>
      <div className="r-sheet-buttons">
        <Secondary>{t.cancel}</Secondary>
        <Primary>{t.save}</Primary>
      </div>
    </Sheet>
  )
}

export function RemindersCheckScreen({ lang }: { lang: Lang }) {
  const t = tx(lang)
  return (
    <>
      <PageHead lang={lang} title={t.checkTitle} />
      <div className="r-checksummary">
        <span className="r-status-mark">
          <Icon name="battery_alert" />
        </span>
        <p className="r-summary">{t.checkOk}</p>
      </div>
      <div className="r-list r-list-page">
        {t.checks.map(([name, state, kind]) => (
          <div
            key={name}
            className={`r-row r-checkrow ${kind === "warn" ? "is-warn" : ""}`}
          >
            <span className="r-row-icon">
              <Icon name={kind === "warn" ? "error" : "check_circle"} />
            </span>
            <span className="r-row-text">
              <span className="r-row-title">{name}</span>
              {kind === "warn" && (
                <>
                  <span className="r-row-sub">{t.batteryHelp}</span>
                  <TextButton>{t.batteryFix}</TextButton>
                </>
              )}
            </span>
            <span className="r-row-value">{state}</span>
          </div>
        ))}
      </div>
      <p className="r-caption r-pad r-foot">{t.checkFooter}</p>
      <div className="r-pad">
        <TextButton icon="content_copy">{t.copyDetails}</TextButton>
      </div>
    </>
  )
}
