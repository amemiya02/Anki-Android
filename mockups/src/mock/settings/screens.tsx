import type { ReactNode } from "react"

import { DeckListScreen } from "@/mock/screens"

import { settingsText, type SLang } from "./text"
import "./settings.css"

/** A Material Symbols icon, drawn in Quiet paper's style. */
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

// ---------- Pieces of a settings page ----------

function PageBar({ title, lang }: { title: string; lang: SLang }) {
  return (
    <header className="s-bar">
      <button type="button" className="iconbtn s-iconbtn" aria-label={settingsText[lang].back}>
        <Icon name="arrow_back" />
      </button>
      <h1 className="s-title">{title}</h1>
    </header>
  )
}

function SectionTitle({ children }: { children: ReactNode }) {
  return <h2 className="s-section">{children}</h2>
}

function Switch({ on }: { on: boolean }) {
  return (
    <span className={`s-switch ${on ? "is-on" : ""}`} aria-hidden="true">
      <span className="s-switch-thumb" />
    </span>
  )
}

function Row({
  title,
  sub,
  value,
  trailing,
  icon,
  muted = false,
}: {
  title: ReactNode
  sub?: ReactNode
  value?: string
  trailing?: ReactNode
  icon?: string
  muted?: boolean
}) {
  return (
    <div className={`s-row ${sub || value ? "s-row-two" : ""} ${muted ? "is-muted" : ""}`}>
      {icon && (
        <span className="s-row-icon">
          <Icon name={icon} />
        </span>
      )}
      <span className="s-row-text">
        <span className="s-row-title">{title}</span>
        {sub && <span className="s-row-sub">{sub}</span>}
        {value && <span className="s-row-value">{value}</span>}
      </span>
      {trailing && <span className="s-row-trailing">{trailing}</span>}
    </div>
  )
}

function SwitchRow({ title, sub, on }: { title: string; sub?: string; on: boolean }) {
  return <Row title={title} sub={sub} trailing={<Switch on={on} />} />
}

function Slider({ value }: { value: number }) {
  return (
    <span className="s-slider" aria-hidden="true">
      <span className="s-slider-fill" style={{ width: `${value}%` }} />
      <span className="s-slider-thumb" style={{ left: `${value}%` }} />
    </span>
  )
}

function SearchField({ lang, query }: { lang: SLang; query?: string }) {
  const t = settingsText[lang]
  return (
    <div className={`s-search ${query ? "is-active" : ""}`} role="search">
      <Icon name="search" />
      {query ? (
        <span className="s-search-query">
          {query}
          <span className="s-caret" />
        </span>
      ) : (
        <span className="s-search-placeholder">{t.search}</span>
      )}
      {query && (
        <span className="s-search-clear">
          <Icon name="close" />
        </span>
      )}
    </div>
  )
}

// ---------- Settings surfaces ----------

/** Settings home: search first, then every group with what it holds. */
export function SettingsHome({ lang }: { lang: SLang }) {
  const t = settingsText[lang]
  return (
    <>
      <PageBar title={t.settings} lang={lang} />
      <div className="s-page">
        <SearchField lang={lang} />
        <div className="s-list s-list-home">
          {t.groups.map((g) => (
            <Row
              key={g.key}
              icon={g.icon}
              title={g.title}
              sub={g.sub}
              trailing={<Icon name="chevron_right" />}
            />
          ))}
        </div>
      </div>
    </>
  )
}

/** Search: results as the person types, each with where it lives. */
export function SettingsSearch({ lang }: { lang: SLang }) {
  const t = settingsText[lang]
  return (
    <>
      <PageBar title={t.settings} lang={lang} />
      <div className="s-page">
        <SearchField lang={lang} query={t.query} />
        <p className="s-count">{t.resultCount}</p>
        <div className="s-list">
          {t.results.map(([title, path]) => (
            <Row
              key={title}
              title={<Highlight text={title} query={t.query} />}
              sub={path}
              trailing={<Icon name="chevron_right" />}
            />
          ))}
        </div>
      </div>
      <Keyboard lang={lang} />
    </>
  )
}

/** The matching part of a result is set in a heavier weight, not a colour. */
function Highlight({ text, query }: { text: string; query: string }) {
  const at = text.toLowerCase().indexOf(query.toLowerCase())
  if (at < 0) return <>{text}</>
  return (
    <>
      {text.slice(0, at)}
      <b className="s-match">{text.slice(at, at + query.length)}</b>
      {text.slice(at + query.length)}
    </>
  )
}

function Keyboard({ lang }: { lang: SLang }) {
  const rows =
    lang === "zh"
      ? ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
      : ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
  return (
    <div className="s-keyboard" aria-hidden="true">
      {rows.map((r) => (
        <div key={r} className="s-keys">
          {[...r].map((k) => (
            <span key={k} className="s-key">
              {k}
            </span>
          ))}
        </div>
      ))}
      <div className="s-keys">
        <span className="s-key s-key-wide">{lang === "zh" ? "中/英" : "?123"}</span>
        <span className="s-key s-key-space">{lang === "zh" ? "拼音" : "space"}</span>
        <span className="s-key s-key-wide">
          <Icon name="search" />
        </span>
      </div>
    </div>
  )
}

export type ThemeStyle = "tiles" | "lists"

/** A small picture of the deck list in one theme, for choosing it. */
function ThemeThumb({ theme }: { theme: "light" | "dark" | "black" | "system" }) {
  return (
    <span className={`s-thumb s-thumb-${theme}`} aria-hidden="true">
      <span className="s-thumb-line s-thumb-title" />
      <span className="s-thumb-line" />
      <span className="s-thumb-line" />
      <span className="s-thumb-line s-thumb-short" />
    </span>
  )
}

/** Appearance: the theme, then the deck list background. */
export function SettingsAppearance({
  lang,
  style = "tiles",
}: {
  lang: SLang
  style?: ThemeStyle
}) {
  const t = settingsText[lang]
  const tiles = [
    ["system", t.followSystem],
    ["light", t.light],
    ["dark", t.dark],
    ["black", t.black],
  ] as const
  return (
    <>
      <PageBar title={t.appearance} lang={lang} />
      <div className="s-page">
        {style === "tiles" ? (
          <>
            <SectionTitle>{t.themes}</SectionTitle>
            <div className="s-tiles" role="radiogroup" aria-label={t.themes}>
              {tiles.map(([key, label]) => (
                <span
                  key={key}
                  className={`s-tile ${key === "system" ? "is-chosen" : ""}`}
                  role="radio"
                  aria-checked={key === "system"}
                >
                  <ThemeThumb theme={key} />
                  {key === "system" && (
                    <span className="s-tile-check">
                      <Icon name="check" />
                    </span>
                  )}
                  <span className="s-tile-label">{label}</span>
                </span>
              ))}
            </div>
            <p className="s-note">{t.themeNote}</p>
            <div className="s-list s-list-tight">
              <Row
                title={t.atNight}
                trailing={
                  <span className="s-segmented" role="radiogroup">
                    <span>{t.dark}</span>
                    <span className="is-chosen">
                      <Icon name="check" />
                      {t.black}
                    </span>
                  </span>
                }
              />
            </div>
          </>
        ) : (
          <>
            <SectionTitle>{t.themes}</SectionTitle>
            <div className="s-list">
              <Row title={t.themeOld} value={t.themeOldValue} />
              <Row title={t.nightOld} value={t.nightOldValue} />
            </div>
          </>
        )}
        <SectionTitle>{t.background}</SectionTitle>
        <div className="s-list">
          <Row
            icon="image"
            title={t.chooseImage}
            sub={t.chooseImageSub}
            trailing={<Icon name="chevron_right" />}
          />
          <Row icon="hide_image" title={t.removeImage} muted />
        </div>
      </div>
    </>
  )
}

/** Studying: scheduling, the study screen, answer buttons and toolbar. */
export function SettingsStudying({ lang }: { lang: SLang }) {
  const t = settingsText[lang]
  return (
    <>
      <PageBar title={t.studying} lang={lang} />
      <div className="s-page">
        <SectionTitle>{t.scheduling}</SectionTitle>
        <div className="s-list">
          <Row title={t.dayStart} value={t.dayStartValue} />
          <Row title={t.learnAhead} value={t.learnAheadValue} />
          <Row title={t.timebox} value={t.timeboxValue} trailing={<Icon name="chevron_right" />} />
        </div>
        <SectionTitle>{t.studyScreen}</SectionTitle>
        <div className="s-list">
          <SwitchRow title={t.keepScreenOn} sub={t.keepScreenOnSub} on />
          <SwitchRow title={t.centre} sub={t.centreSub} on={false} />
          <Row title={t.frame} value={t.frameValue} />
          <Row title={t.hideBars} value={t.hideBarsValue} />
          <SwitchRow title={t.cutout} on={false} />
          <SwitchRow title={t.feedback} on />
          <SwitchRow title={t.remaining} on />
        </div>
      </div>
    </>
  )
}

function Chips({ items }: { items: string[] }) {
  return (
    <span className="s-chips">
      {items.map((c) => (
        <span key={c} className="s-chip">
          {c}
        </span>
      ))}
    </span>
  )
}

/** Controls: gestures, then each action with its bindings. */
export function SettingsControls({ lang }: { lang: SLang }) {
  const t = settingsText[lang]
  return (
    <>
      <PageBar title={t.controls} lang={lang} />
      <div className="s-page">
        <div className="s-list s-list-top">
          <SwitchRow title={t.cornerTouch} sub={t.cornerTouchSub} on />
          <div className="s-row s-row-slider">
            <span className="s-row-text">
              <span className="s-row-title">{t.swipe}</span>
              <Slider value={50} />
            </span>
            <span className="s-row-trailing s-slider-value">100%</span>
          </div>
          <Row title={t.shortcuts} trailing={<Icon name="chevron_right" />} />
        </div>
        <div className="s-tabs" role="tablist">
          <span role="tab" aria-selected="true" className="is-current">
            {t.tabStudy}
          </span>
          <span role="tab" aria-selected="false">
            {t.tabPreview}
          </span>
        </div>
        <SectionTitle>{t.answerButtons}</SectionTitle>
        <div className="s-list">
          {t.bindings.map(([action, keys]) => (
            <Row key={action} title={action} trailing={<Chips items={keys} />} />
          ))}
        </div>
        <SectionTitle>{t.flags}</SectionTitle>
        <div className="s-list">
          {t.flagBindings.map(([action, keys]) => (
            <Row key={action} title={action} trailing={<Chips items={keys} />} />
          ))}
        </div>
      </div>
    </>
  )
}

function ActionRow({
  icon,
  label,
  dragging = false,
}: {
  icon: string
  label: string
  dragging?: boolean
}) {
  return (
    <div className={`s-row s-action ${dragging ? "is-dragging" : ""}`}>
      <span className="s-row-icon">
        <Icon name={icon} />
      </span>
      <span className="s-row-text">
        <span className="s-row-title">{label}</span>
      </span>
      <span className="s-row-trailing s-handle">
        <Icon name="drag_handle" />
      </span>
    </div>
  )
}

/** Toolbar actions: three lists; an action is dragged between them. */
export function SettingsToolbar({ lang }: { lang: SLang }) {
  const t = settingsText[lang]
  return (
    <>
      <PageBar title={t.toolbar} lang={lang} />
      <div className="s-page">
        <p className="s-lead">{t.toolbarLead}</p>
        <SectionTitle>{t.always}</SectionTitle>
        <div className="s-list">
          {t.actionsAlways.map(([icon, label]) => (
            <ActionRow key={icon} icon={icon} label={label} />
          ))}
          <div className="s-drop" aria-hidden="true" />
        </div>
        <ActionRow icon={t.dragging[0]} label={t.dragging[1]} dragging />
        <SectionTitle>{t.menuOnly}</SectionTitle>
        <div className="s-list">
          {t.actionsMenu.map(([icon, label]) => (
            <ActionRow key={icon} icon={icon} label={label} />
          ))}
        </div>
        <SectionTitle>{t.disabled}</SectionTitle>
        <div className="s-list">
          {t.actionsDisabled.map(([icon, label]) => (
            <ActionRow key={icon} icon={icon} label={label} />
          ))}
        </div>
      </div>
    </>
  )
}

/** Sync settings: drawn here only as the page behind the single-choice dialog. */
function SettingsSync({ lang }: { lang: SLang }) {
  const t = settingsText[lang]
  return (
    <>
      <PageBar title={t.sync} lang={lang} />
      <div className="s-page">
        <div className="s-list s-list-top">
          <Row title={t.server} value={t.serverValue} trailing={<Icon name="chevron_right" />} />
          <SwitchRow title={t.autoSync} sub={t.autoSyncSub} on />
          <SwitchRow title={t.status} on />
          <Row title={t.fetchMedia} value={t.fetchMediaValue} />
          <SwitchRow title={t.metered} on={false} />
        </div>
      </div>
    </>
  )
}

// ---------- Dialogs ----------

export type DialogKind =
  | "confirm"
  | "destructive"
  | "input"
  | "single"
  | "multiple"
  | "progress"
  | "error"

export type DestructiveStyle = "outlined" | "filled"

function Dialog({
  id,
  title,
  mark,
  markTone = "plain",
  children,
  actions,
}: {
  id: string
  title: string
  mark?: string
  markTone?: "plain" | "warn"
  children?: ReactNode
  actions?: ReactNode
}) {
  return (
    <div className="d-scrim">
      {/* A picture of a modal dialog. In the app this is a native dialog; on
          the web it would be <dialog> opened with showModal(). */}
      <section className="d-dialog" aria-labelledby={id}>
        {mark && (
          <span className={`d-mark d-mark-${markTone}`}>
            <Icon name={mark} />
          </span>
        )}
        <h2 className="d-title" id={id}>
          {title}
        </h2>
        {children}
        {actions && <div className="d-actions">{actions}</div>}
      </section>
    </div>
  )
}

function TextButton({ children }: { children: ReactNode }) {
  return (
    <button type="button" className="d-btn d-btn-text">
      {children}
    </button>
  )
}

function FilledButton({ children }: { children: ReactNode }) {
  return (
    <button type="button" className="d-btn d-btn-filled">
      {children}
    </button>
  )
}

function DangerButton({
  children,
  style,
}: {
  children: ReactNode
  style: DestructiveStyle
}) {
  return (
    <button
      type="button"
      className={`d-btn ${style === "outlined" ? "d-btn-danger" : "d-btn-filled"}`}
    >
      {style === "outlined" && <Icon name="warning" />}
      {children}
    </button>
  )
}

export function DialogScreen({
  lang,
  kind,
  destructive = "outlined",
}: {
  lang: SLang
  kind: DialogKind
  destructive?: DestructiveStyle
}) {
  const t = settingsText[lang]
  const page = kind === "single" ? <SettingsSync lang={lang} /> : <DeckListScreen lang={lang} />
  let dialog: ReactNode
  switch (kind) {
    case "confirm":
      dialog = (
        <Dialog
          id="d-confirm"
          title={t.confirm.title}
          mark="settings_backup_restore"
          actions={
            <>
              <TextButton>{t.confirm.no}</TextButton>
              <FilledButton>{t.confirm.yes}</FilledButton>
            </>
          }
        >
          <p className="d-body">{t.confirm.body}</p>
        </Dialog>
      )
      break
    case "destructive":
      dialog = (
        <Dialog
          id="d-destructive"
          title={t.destructive.title}
          mark={destructive === "outlined" ? "warning" : "history"}
          markTone={destructive === "outlined" ? "warn" : "plain"}
          actions={
            <>
              <TextButton>{t.cancel}</TextButton>
              <DangerButton style={destructive}>{t.destructive.yes}</DangerButton>
            </>
          }
        >
          <p className="d-body">{t.destructive.body}</p>
          <p className="d-detail">{t.destructive.detail}</p>
        </Dialog>
      )
      break
    case "input":
      dialog = (
        <Dialog
          id="d-input"
          title={t.input.title}
          actions={
            <>
              <TextButton>{t.cancel}</TextButton>
              <FilledButton>{t.input.yes}</FilledButton>
            </>
          }
        >
          <label className="d-field">
            <span className="d-field-label">{t.input.label}</span>
            <span className="d-field-box">
              <span lang="ja">{t.input.value}</span>
              <span className="s-caret" />
            </span>
          </label>
          <p className="d-help">{t.input.help}</p>
        </Dialog>
      )
      break
    case "single":
      dialog = (
        <Dialog
          id="d-single"
          title={t.single.title}
          actions={<TextButton>{t.cancel}</TextButton>}
        >
          <div className="d-choices" role="radiogroup">
            {t.single.options.map((o, i) => (
              <span key={o} className="d-choice" role="radio" aria-checked={i === t.single.chosen}>
                <span className={`d-radio ${i === t.single.chosen ? "is-on" : ""}`} />
                {o}
              </span>
            ))}
          </div>
          <p className="d-help">{t.single.note}</p>
        </Dialog>
      )
      break
    case "multiple":
      dialog = (
        <Dialog
          id="d-multiple"
          title={t.multiple.title}
          actions={
            <>
              <TextButton>{t.cancel}</TextButton>
              <FilledButton>{t.multiple.yes}</FilledButton>
            </>
          }
        >
          <p className="d-detail">{t.multiple.lead}</p>
          <div className="d-choices">
            {t.multiple.options.map(([o, sub], i) => (
              <span key={o} className="d-choice" role="checkbox" aria-checked={t.multiple.chosen[i]}>
                <span className={`d-check ${t.multiple.chosen[i] ? "is-on" : ""}`}>
                  {t.multiple.chosen[i] && <Icon name="check" />}
                </span>
                <span className="d-choice-text">
                  {o}
                  {sub && <span className="d-choice-sub">{sub}</span>}
                </span>
              </span>
            ))}
          </div>
        </Dialog>
      )
      break
    case "progress":
      dialog = (
        <Dialog id="d-progress" title={t.progress.title}>
          <p className="d-body">{t.progress.file}</p>
          <span
            className="d-progress"
            role="progressbar"
            aria-valuenow={62}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <span style={{ width: "62%" }} />
          </span>
          <p className="d-detail d-progress-count">
            <span>{t.progress.count}</span>
            <span>62%</span>
          </p>
          <p className="d-help">{t.progress.note}</p>
        </Dialog>
      )
      break
    case "error":
      dialog = (
        <Dialog
          id="d-error"
          title={t.error.title}
          mark="cloud_off"
          markTone="warn"
          actions={
            <>
              <TextButton>{t.error.no}</TextButton>
              <FilledButton>{t.error.yes}</FilledButton>
            </>
          }
        >
          <p className="d-body">{t.error.body}</p>
          <p className="d-disclosure">
            <Icon name="chevron_right" />
            {t.error.details}
          </p>
        </Dialog>
      )
      break
  }
  return (
    <>
      <div className="d-page" aria-hidden="true">
        {page}
      </div>
      {dialog}
    </>
  )
}
