export type Direction = "paper" | "crisp" | "soft"
export type Lang = "en" | "zh"
export type Side = "question" | "answer"
export type Theme = "light" | "dark" | "black"

/** new, learning, due */
export type Counts = [number, number, number]

export interface Deck {
  name: string
  /** language of the Deck's own name, so the right glyph forms are used */
  nameLang: "en" | "zh" | "ja"
  counts: Counts
  children?: Deck[]
  expanded?: boolean
}

// The same sample Decks as the "before" pictures in docs/redesign/before.
// Counts are fuller than in those pictures so that all three kinds of count
// can be judged.
export const decks: Deck[] = [
  { name: "Anatomy", nameLang: "en", counts: [0, 0, 0] },
  {
    name: "English",
    nameLang: "en",
    counts: [7, 0, 9],
    children: [{ name: "GRE Words", nameLang: "en", counts: [7, 0, 9] }],
  },
  {
    name: "中文",
    nameLang: "zh",
    counts: [8, 2, 6],
    children: [{ name: "HSK 4", nameLang: "zh", counts: [8, 2, 6] }],
  },
  {
    name: "日本語",
    nameLang: "ja",
    counts: [12, 3, 11],
    expanded: true,
    children: [
      { name: "N3 語彙", nameLang: "ja", counts: [6, 3, 7] },
      { name: "文法", nameLang: "ja", counts: [6, 0, 4] },
    ],
  },
]

export const totals = { new: 27, learning: 5, due: 26, all: 58, studied: 9 }

export const session = {
  deckPath: [
    { name: "中文", nameLang: "zh" as const },
    { name: "HSK 4", nameLang: "zh" as const },
  ],
  remaining: [8, 2, 6] as Counts,
  done: 9,
  total: 25,
  question: "熟悉",
  answer: "shúxī — familiar with",
}

export const text = {
  en: {
    decks: "Decks",
    browse: "Browse",
    stats: "Stats",
    more: "More",
    date: "Monday, 16 November",
    today: "58 cards to study today",
    todayShort: "58 today",
    studied: "9 studied so far",
    new: "New",
    learning: "Learning",
    due: "Due",
    done: "Done for today",
    add: "Add",
    sync: "Sync",
    showAnswer: "Show answer",
    again: "Again",
    hard: "Hard",
    good: "Good",
    easy: "Easy",
    times: ["<1 min", "<6 min", "<10 min", "3 days"],
    left: "16 left",
  },
  zh: {
    decks: "牌组",
    browse: "浏览",
    stats: "统计",
    more: "更多",
    date: "11月16日 星期一",
    today: "今天要学 58 张卡片",
    todayShort: "今天 58 张",
    studied: "已学 9 张",
    new: "新卡片",
    learning: "学习中",
    due: "待复习",
    done: "今天已完成",
    add: "添加",
    sync: "同步",
    showAnswer: "显示答案",
    again: "重来",
    hard: "困难",
    good: "良好",
    easy: "简单",
    times: ["<1 分钟", "<6 分钟", "<10 分钟", "3 天"],
    left: "还剩 16 张",
  },
} as const

export const directions: Record<
  Direction,
  { name: string; line: string; fonts: string; swatches: string[] }
> = {
  paper: {
    name: "Quiet paper",
    line: "Warm paper, ink-dark text, one cinnabar accent. Editorial and calm.",
    fonts: "Newsreader · 霞鹜文楷 LXGW WenKai · クレー Klee One",
    swatches: ["#F5F0E6", "#211E19", "#A63D25", "#2F4A6B", "#3E5B34"],
  },
  crisp: {
    name: "Crisp tool",
    line: "Cool neutrals, sharp sans, denser rows, one vivid accent.",
    fonts:
      "IBM Plex Sans · IBM Plex Sans SC · IBM Plex Sans JP · Plex Mono figures",
    swatches: ["#FFFFFF", "#0D1117", "#4338F5", "#F4F5F7", "#57606A"],
  },
  soft: {
    name: "Soft and friendly",
    line: "Rounder shapes, more colour, larger elements.",
    fonts: "Nunito · Noto Sans SC · Zen Maru Gothic",
    swatches: ["#F3F0FB", "#2A2440", "#6447E8", "#DCEBFF", "#D9F5E1"],
  },
}
