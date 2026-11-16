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

// Long Deck names in Chinese and Japanese, to show how names wrap while the
// counts stay in place. Parent counts are the sums of their sub-Decks.
export const longDecks: Deck[] = [
  {
    name: "唐诗三百首 · 全本精选（附注释、译文与赏析）",
    nameLang: "zh",
    counts: [0, 0, 0],
  },
  {
    name: "汉语水平考试 HSK 5 级核心词汇 2500 词（带例句与录音）",
    nameLang: "zh",
    counts: [15, 4, 62],
    expanded: true,
    children: [
      {
        name: "第 11–20 课：生词、固定搭配与易混淆词辨析",
        nameLang: "zh",
        counts: [15, 4, 38],
      },
      { name: "听力与阅读", nameLang: "zh", counts: [0, 0, 24] },
    ],
  },
  {
    name: "日本語能力試験 N2 文法・語彙・読解 総まとめ（2026年版）",
    nameLang: "ja",
    counts: [20, 5, 148],
    expanded: true,
    children: [
      {
        name: "第3章 敬語と複合動詞の使い分け（例文・音声付き）",
        nameLang: "ja",
        counts: [10, 5, 90],
      },
      { name: "読解練習", nameLang: "ja", counts: [10, 0, 58] },
    ],
  },
]

export const longTotals = { all: 254, studied: 31 }

/** The four ways a Deck can be finished, from what the Engine reports. */
export type FinishedState = "later" | "limit" | "buried" | "done"
export const finishedStates: FinishedState[] = [
  "later",
  "limit",
  "buried",
  "done",
]

/** Options the owner picks between where a Mockup settles something. */
export type CountStyle = "columns" | "total"
export type StudyBarStyle = "full" | "minimal"
export type ButtonPlace = "bottom" | "top"

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
    estimate: "about 6 min left",
    back: "Back",
    undo: "Undo",
    todayColumn: "Today",
    breakdown: (n: number, kind: string) => `${n} ${kind.toLowerCase()}`,
    todayLong: "254 cards to study today",
    studiedLong: "31 studied so far",

    // More
    settings: "Settings",
    settingsSub: "Appearance, studying, controls, backups",
    help: "Help",
    helpSub: "The manual and the community forum",
    support: "Support AnkiDroid",
    supportSub: "Help the project Hayaki is built on",
    builtOn: "Built on AnkiDroid",

    // Empty deck list, for a Newcomer
    emptyTitle: "No decks yet",
    emptyBody:
      "Get a shared deck to start studying straight away, or bring your own: import a deck file, or create a deck and add notes to it with the + button.",
    getShared: "Get shared decks",
    importFile: "Import a file",
    importFileSub: "A deck package (.apkg) or a collection (.colpkg)",
    createDeck: "Create a deck",
    createDeckSub: "Then add notes with the + button",

    // Finished screen
    finished: {
      later: {
        icon: "schedule",
        title: "Finished for now",
        body: "2 learning cards come back later today. The next one is ready in 12 minutes.",
      },
      limit: {
        icon: "hourglass_bottom",
        title: "Daily limit reached",
        body: "More new cards are waiting, but this deck’s daily limit for new cards has been reached. Custom study can extend today’s limit, or you can raise it in the deck options.",
      },
      buried: {
        icon: "inventory_2",
        title: "Finished for now",
        body: "Some related or buried cards were held back until a later session.",
      },
      done: {
        icon: "check",
        title: "Done for today",
        body: "Congratulations! You have finished this deck for today.",
      },
    },
    unbury: "Unbury and keep studying",
    customStudy: "Custom study",
    customStudySub: "Extend today’s limit, review ahead, study by tag",
    deckOptions: "Deck options",
    deckOptionsSub: "Daily limits, learning steps, display order",
    studiedToday: "Today: 25 cards studied in 14 minutes",
    backToDecks: "Back to decks",
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
    estimate: "约需 6 分钟",
    back: "返回",
    undo: "撤销",
    todayColumn: "今天",
    breakdown: (n: number, kind: string) => `${kind} ${n}`,
    todayLong: "今天要学 254 张卡片",
    studiedLong: "已学 31 张",

    // More
    settings: "设置",
    settingsSub: "外观、学习、操作方式、备份",
    help: "帮助",
    helpSub: "使用手册与社区论坛",
    support: "支持 AnkiDroid",
    supportSub: "支持 Hayaki 所基于的项目",
    builtOn: "基于 AnkiDroid",

    // Empty deck list, for a Newcomer
    emptyTitle: "还没有牌组",
    emptyBody:
      "获取一个共享牌组即可马上开始学习；也可以用自己的：导入牌组文件，或者创建牌组，再用 + 按钮添加笔记。",
    getShared: "获取共享牌组",
    importFile: "导入文件",
    importFileSub: "牌组包（.apkg）或集合包（.colpkg）",
    createDeck: "创建牌组",
    createDeckSub: "然后用 + 按钮添加笔记",

    // Finished screen
    finished: {
      later: {
        icon: "schedule",
        title: "暂时学完了",
        body: "今天稍后还有 2 张学习中的卡片，下一张 12 分钟后可以学习。",
      },
      limit: {
        icon: "hourglass_bottom",
        title: "已达到每日上限",
        body: "还有新卡片可学，但本牌组今天的新卡片已达到每日上限。可以用自定义学习扩大今天的上限，或在牌组选项中调高上限。",
      },
      buried: {
        icon: "inventory_2",
        title: "暂时学完了",
        body: "部分相关卡片或已搁置的卡片被推迟到之后再学。",
      },
      done: {
        icon: "check",
        title: "今天已完成",
        body: "恭喜！你已经完成了这个牌组今天的学习任务。",
      },
    },
    unbury: "取消搁置并继续学习",
    customStudy: "自定义学习",
    customStudySub: "扩大今天的上限、提前复习、按标签学习",
    deckOptions: "牌组选项",
    deckOptionsSub: "每日上限、学习步骤、显示顺序",
    studiedToday: "今天：14 分钟内学习了 25 张卡片",
    backToDecks: "返回牌组",
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
