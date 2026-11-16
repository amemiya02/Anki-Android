import { StrictMode } from "react"
import { createRoot } from "react-dom/client"

import "./index.css"

// Typefaces for the three Directions. All are under the SIL Open Font License.
// Quiet paper
import "@fontsource-variable/newsreader/index.css"
import "lxgw-wenkai-webfont/lxgwwenkai-regular.css"
import "lxgw-wenkai-webfont/lxgwwenkai-bold.css"
import "@fontsource/klee-one/400.css"
import "@fontsource/klee-one/600.css"
// Crisp tool
import "@ibm/plex-sans/css/ibm-plex-sans-default.css"
import "@ibm/plex-sans-sc/css/ibm-plex-sans-sc-default.css"
import "@ibm/plex-sans-jp/css/ibm-plex-sans-jp-default.css"
import "@fontsource/ibm-plex-mono/400.css"
import "@fontsource/ibm-plex-mono/600.css"
// Soft and friendly
import "@fontsource-variable/nunito/index.css"
import "@fontsource/noto-sans-sc/400.css"
import "@fontsource/noto-sans-sc/500.css"
import "@fontsource/noto-sans-sc/700.css"
import "@fontsource/noto-sans-sc/900.css"
import "@fontsource/zen-maru-gothic/400.css"
import "@fontsource/zen-maru-gothic/500.css"
import "@fontsource/zen-maru-gothic/700.css"
import "@fontsource/zen-maru-gothic/900.css"
// Icons: one Material Symbols style per Direction
import "@fontsource-variable/material-symbols-outlined/full.css"
import "@fontsource-variable/material-symbols-sharp/full.css"
import "@fontsource-variable/material-symbols-rounded/full.css"

import "./mock/phone.css"
import App from "./App.tsx"

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
