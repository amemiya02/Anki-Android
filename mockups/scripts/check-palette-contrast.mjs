// Checks the candidate Light palettes (src/mock/palette/palettes.json), and the
// current Light palette beside them, against the Quality Bar: every text and
// mark pairing the values file lists (4.5:1 text, 3:1 marks), the two the card
// browser adds (ink on the progress-track fill as text, a ticked box as a
// mark), and each flag colour as a mark (3:1) on the background. It also checks
// that each candidate gives every role of the values file's `light` block.
// Usage: node scripts/check-palette-contrast.mjs
import { readFileSync } from "node:fs"
import { fileURLToPath } from "node:url"

const read = (path) =>
  JSON.parse(
    readFileSync(fileURLToPath(new URL(path, import.meta.url)), "utf8")
  )

const values = read("../../docs/redesign/quiet-paper.values.json")
const flags = read("../src/mock/find/flags.json")
const { candidates } = read("../src/mock/palette/palettes.json")

function luminance(hex) {
  const n = parseInt(hex.slice(1), 16)
  const channel = (v) => {
    const c = v / 255
    return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  }
  return (
    0.2126 * channel((n >> 16) & 255) +
    0.7152 * channel((n >> 8) & 255) +
    0.0722 * channel(n & 255)
  )
}

function ratio(a, b) {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

const palettes = {
  current: {
    name: "Current Light palette (Plain paper)",
    colour: values.colour.light,
    flags: flags.light,
  },
  ...candidates,
}

const roles = Object.keys(values.colour.light)
let failures = 0

for (const [key, palette] of Object.entries(palettes)) {
  const c = palette.colour
  console.log(`\n${key} (${palette.name})`)
  const missing = roles.filter((role) => !/^#[0-9A-F]{6}$/i.test(c[role] ?? ""))
  if (missing.length) {
    failures += missing.length
    console.log(`  FAIL  missing or malformed roles: ${missing.join(", ")}`)
  }
  const pairs = [
    ...values.contrast.text.map(([fg, bg]) => [fg, bg, 4.5]),
    ...values.contrast.marks.map(([fg, bg]) => [fg, bg, 3]),
    ["onBackground", "progressTrack", 4.5],
    ["background", "emphasis", 3],
  ].map(([fg, bg, min]) => [`${fg} on ${bg}`, c[fg], c[bg], min])
  for (const [name, hex] of Object.entries(palette.flags)) {
    pairs.push([`flag ${name} on background`, hex, c.background, 3])
  }
  for (const [label, fg, bg, minimum] of pairs) {
    if (!fg || !bg) continue
    const r = ratio(fg, bg)
    const ok = r >= minimum
    if (!ok) failures++
    console.log(
      `  ${ok ? "pass" : "FAIL"}  ${r.toFixed(2).padStart(5)}:1  (needs ${minimum})  ${label}`
    )
  }
}

console.log(
  failures ? `\n${failures} pairing(s) fail.` : "\nEvery pairing passes."
)
process.exit(failures ? 1 : 0)
