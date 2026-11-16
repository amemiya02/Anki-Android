// Checks every colour pairing listed in the Direction's design values against
// the Quality Bar: 4.5:1 for text, 3:1 for marks that carry meaning.
// Usage: node scripts/check-contrast.mjs
import { readFileSync } from "node:fs"
import { fileURLToPath } from "node:url"

const file = fileURLToPath(
  new URL("../../docs/redesign/quiet-paper.values.json", import.meta.url)
)
const values = JSON.parse(readFileSync(file, "utf8"))

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
for (const [theme, colours] of Object.entries(values.colour)) {
  console.log(`\n${theme}`)
  for (const [kind, minimum] of [
    ["text", 4.5],
    ["marks", 3],
  ]) {
    for (const [fg, bg] of values.contrast[kind]) {
      const r = ratio(colours[fg], colours[bg])
      const ok = r >= minimum
      if (!ok) failures++
      console.log(
        `  ${ok ? "pass" : "FAIL"}  ${r.toFixed(2).padStart(5)}:1  (needs ${minimum})  ${fg} on ${bg}`
      )
    }
  }
}

console.log(
  failures ? `\n${failures} pairing(s) fail.` : "\nEvery pairing passes."
)
process.exit(failures ? 1 : 0)
