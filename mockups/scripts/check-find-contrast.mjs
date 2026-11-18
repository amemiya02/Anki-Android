// Checks the colour pairings the Milestone 3 Mockups (src/mock/find) add to
// those in the values file, against the Quality Bar: each proposed flag colour
// (src/mock/find/flags.json) is a mark and must reach 3:1 against the
// background; ink on the progress-track fill (an active chip, a selected row)
// is text and must reach 4.5:1; a ticked box (the background on emphasis) is a
// mark and must reach 3:1.
// Usage: node scripts/check-find-contrast.mjs
import { readFileSync } from "node:fs"
import { fileURLToPath } from "node:url"

const read = (path) =>
  JSON.parse(
    readFileSync(fileURLToPath(new URL(path, import.meta.url)), "utf8")
  )

const values = read("../../docs/redesign/quiet-paper.values.json")
const flags = read("../src/mock/find/flags.json")

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

let failures = 0
function check(label, fg, bg, minimum) {
  const r = ratio(fg, bg)
  const ok = r >= minimum
  if (!ok) failures++
  console.log(
    `  ${ok ? "pass" : "FAIL"}  ${r.toFixed(2).padStart(5)}:1  (needs ${minimum})  ${label}`
  )
}

for (const theme of ["light", "dark", "black"]) {
  console.log(`\n${theme}`)
  const c = values.colour[theme]
  const background = c.background
  check("onBackground on progressTrack", c.onBackground, c.progressTrack, 4.5)
  check("background on emphasis", c.background, c.emphasis, 3)
  for (const [name, hex] of Object.entries(flags[theme])) {
    check(`flag ${name} on background`, hex, background, 3)
  }
}

console.log(
  failures ? `\n${failures} pairing(s) fail.` : "\nEvery pairing passes."
)
process.exit(failures ? 1 : 0)
