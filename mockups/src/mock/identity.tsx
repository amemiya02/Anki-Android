import type { ReactNode } from "react"

/** Name candidates and launcher icon concepts for the Fork, in Quiet paper. */

const PAPER = "#F5F0E6"
const INK = "#211E19"
const CINNABAR = "#A63D25"
const CREAM = "#FFF9F2"

export const names = [
  {
    latin: "Fuda",
    cjk: "札",
    lang: "ja",
    meaning:
      "In Japanese, 札 (fuda) is a card, as in the cards of karuta. In Chinese, 札 as in 札记 means study notes.",
  },
  {
    latin: "Sumi",
    cjk: "墨",
    lang: "ja",
    meaning: "Ink, in both Japanese (sumi) and Chinese (mò).",
  },
  {
    latin: "Tanzaku",
    cjk: "短冊",
    lang: "ja",
    meaning: "The narrow paper cards that poems are written on in Japan.",
  },
] as const

/** A: two paper cards on a cinnabar ground. Works with any name. */
function IconCards() {
  return (
    <svg
      viewBox="0 0 108 108"
      role="img"
      aria-label="Two paper cards on cinnabar"
    >
      <rect width="108" height="108" fill={CINNABAR} />
      <rect
        x="38"
        y="30"
        width="36"
        height="48"
        rx="4"
        fill="none"
        stroke={CREAM}
        strokeWidth="2.5"
        transform="rotate(9 56 54)"
      />
      <g transform="rotate(-7 52 54)">
        <rect x="34" y="30" width="36" height="48" rx="4" fill={CREAM} />
        <rect x="41" y="40" width="22" height="2.5" rx="1.25" fill={INK} />
        <rect x="41" y="47" width="14" height="2.5" rx="1.25" fill={INK} />
        <circle cx="60" cy="68" r="3.2" fill={CINNABAR} />
      </g>
    </svg>
  )
}

/** B: a written card with a seal, on paper. Works with any name. */
function IconSealedCard() {
  return (
    <svg
      viewBox="0 0 108 108"
      role="img"
      aria-label="A written card with a red seal"
    >
      <rect width="108" height="108" fill={PAPER} />
      <rect
        x="35"
        y="27"
        width="38"
        height="54"
        rx="4"
        fill="#FFFDF8"
        stroke={INK}
        strokeWidth="2.5"
      />
      <rect x="42" y="37" width="24" height="2.5" rx="1.25" fill={INK} />
      <rect x="42" y="44" width="24" height="2.5" rx="1.25" fill={INK} />
      <rect x="42" y="51" width="15" height="2.5" rx="1.25" fill={INK} />
      <rect x="56" y="63" width="11" height="11" rx="2" fill={CINNABAR} />
    </svg>
  )
}

/** C: the name's character cut as a seal. Depends on the name. */
function IconSeal({ glyph }: { glyph: string }) {
  return (
    <svg
      viewBox="0 0 108 108"
      role="img"
      aria-label={`The character ${glyph} as a seal`}
    >
      <rect width="108" height="108" fill={CINNABAR} />
      <rect
        x="28"
        y="28"
        width="52"
        height="52"
        rx="7"
        fill="none"
        stroke={CREAM}
        strokeWidth="3"
      />
      <text
        x="54"
        y="55"
        textAnchor="middle"
        dominantBaseline="central"
        fontFamily="'Klee One', 'LXGW WenKai', serif"
        fontWeight="600"
        fontSize="36"
        fill={CREAM}
        lang="ja"
      >
        {glyph}
      </text>
    </svg>
  )
}

function Masks({ children, label }: { children: ReactNode; label: string }) {
  return (
    <figure className="flex flex-col items-center gap-4">
      <div className="flex items-center gap-6">
        <div className="size-[132px] overflow-hidden rounded-full shadow-[0_10px_24px_-12px_rgb(0_0_0/0.5)]">
          {children}
        </div>
        <div className="size-[132px] overflow-hidden rounded-[30%] shadow-[0_10px_24px_-12px_rgb(0_0_0/0.5)]">
          {children}
        </div>
        <div className="size-[64px] overflow-hidden rounded-full shadow-[0_6px_14px_-8px_rgb(0_0_0/0.5)]">
          {children}
        </div>
      </div>
      <figcaption className="max-w-[360px] text-center text-[15px] text-neutral-600">
        {label}
      </figcaption>
    </figure>
  )
}

export function IdentityBoard() {
  return (
    <section className="flex flex-col gap-12">
      <header>
        <h2 className="text-[34px] leading-tight font-semibold text-neutral-900">
          Name and launcher icon
        </h2>
        <p className="mt-1 text-[17px] text-neutral-600">
          The owner chose the name Hayaki and Icon A. The other candidates are
          kept below for the record. Each icon is shown as a circle, as a
          rounded square, and at launcher size.
        </p>
      </header>

      <div className="flex items-center gap-14">
        <div
          className="flex w-[420px] flex-col gap-3 rounded-[6px] border border-[#D8CFBE] bg-[#F5F0E6] p-8 text-[#211E19]"
          style={{
            fontFamily:
              "'Newsreader Variable', 'Klee One', 'LXGW WenKai', serif",
          }}
        >
          <p className="text-[13px] text-[#675F53]">The Fork's name</p>
          <p className="text-[64px] leading-none font-medium">Hayaki</p>
          <p className="text-[16px] leading-snug text-[#675F53]">
            Chosen by the owner. Written the same way in every language.
          </p>
        </div>
        <Masks label="The launcher icon: two paper cards on cinnabar.">
          <IconCards />
        </Masks>
      </div>

      <p className="text-[15px] text-neutral-500">Candidates not chosen</p>

      <div className="flex gap-8">
        {names.map((n, i) => (
          <div
            key={n.latin}
            className="flex w-[420px] flex-col gap-3 rounded-[6px] border border-[#D8CFBE] bg-[#F5F0E6] p-8 text-[#211E19]"
            style={{
              fontFamily:
                "'Newsreader Variable', 'Klee One', 'LXGW WenKai', serif",
            }}
          >
            <p className="text-[13px] text-[#675F53]">Name {i + 1}</p>
            <p className="flex items-baseline gap-4">
              <span className="text-[52px] leading-none font-medium">
                {n.latin}
              </span>
              <span className="text-[44px] leading-none" lang={n.lang}>
                {n.cjk}
              </span>
            </p>
            <p className="text-[16px] leading-snug text-[#675F53]">
              {n.meaning}
            </p>
          </div>
        ))}
      </div>

      <div className="flex gap-16">
        <Masks label="Icon A · Two paper cards on cinnabar. Works with any name.">
          <IconCards />
        </Masks>
        <Masks label="Icon B · A written card with a seal, on paper. Works with any name.">
          <IconSealedCard />
        </Masks>
        <Masks label="Icon C · The name's own character cut as a seal. Shown for Fuda.">
          <IconSeal glyph="札" />
        </Masks>
      </div>
    </section>
  )
}
