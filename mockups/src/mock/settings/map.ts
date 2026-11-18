// Every setting that exists today, in the group the Redesign gives it.
// Found in AnkiDroid/src/main/res/xml/preference*.xml and the fragments in
// AnkiDroid/src/main/java/com/ichi2/anki/preferences/ (settings whose titles
// are set in code are included). Notes say where a setting comes from when it
// moves, and which owner decision of 2026-11-17 changes it.

import type { SLang } from "./text"

/** [title, note]; the note is empty when nothing changes but the look. */
export type MapItem = [string, string]
export interface MapSection {
  title?: string
  items: MapItem[]
}
export interface MapGroup {
  title: string
  sections: MapSection[]
}

const en: { groups: MapGroup[]; dropped: MapItem[]; flagged: MapItem[] } = {
  groups: [
    {
      title: "General",
      sections: [
        {
          items: [
            ["Language", ""],
            ["Deck for new cards", ""],
            ["Paste clipboard images as PNG", ""],
            ["Press back twice to go back/exit", ""],
            ["Display filenames in card browser", "from Appearance"],
            ["‘Anki card’ menu", "system-wide"],
            ["‘Card browser’ menu", "system-wide"],
            ["Full screen navigation drawer", "tablets only"],
          ],
        },
      ],
    },
    {
      title: "Appearance",
      sections: [
        {
          items: [
            ["Theme: Follow system, Light, Dark, Black", "Theme and Night theme as one"],
            ["Night theme: Dark, Black", "with Follow system"],
            ["Deck list background: choose, remove", ""],
          ],
        },
      ],
    },
    {
      title: "Studying",
      sections: [
        {
          title: "Scheduling",
          items: [
            ["Start of next day", "from Reviewing"],
            ["Learn ahead limit", "from Reviewing"],
            ["Timebox time limit", "from Reviewing"],
          ],
        },
        {
          title: "Study screen",
          items: [
            ["Keep screen on", "from Reviewing"],
            ["Center align", "built, #11"],
            ["Frame style", ""],
            ["Hide system bars", ""],
            ["Ignore display cutout", ""],
            ["Show answer feedback", ""],
            ["Show remaining card count", ""],
            ["Show play buttons on cards with audio", ""],
          ],
        },
        {
          title: "Answer buttons",
          items: [
            ["Show answer buttons", ""],
            ["Show next review time above answer buttons", ""],
            ["Hide ‘Hard’ and ‘Easy’ buttons", ""],
          ],
        },
        {
          title: "Toolbar",
          items: [
            ["Toolbar actions (drag and drop)", ""],
            ["Toolbar position", ""],
          ],
        },
      ],
    },
    {
      title: "Controls",
      sections: [
        {
          items: [
            ["9-point touch", ""],
            ["Swipe sensitivity", ""],
            ["Show keyboard shortcuts", ""],
            ["Study screen: 52 actions, each with taps, gestures, keys and controller buttons", "answer, flags, card, note, navigation, media, whiteboard, other, user actions 1–9"],
            ["Previewer: 14 actions", ""],
          ],
        },
      ],
    },
    {
      title: "Accessibility",
      sections: [
        {
          items: [
            ["Card zoom", ""],
            ["Image zoom", "built, #12"],
            ["Answer button size", "old value carried over, #16"],
            ["Card browser font scaling", ""],
            ["Show answer long-press time", "built, #4"],
            ["Double tap time interval", "built, #5"],
            ["Remove animations", ""],
          ],
        },
      ],
    },
    {
      title: "Sync",
      sections: [
        {
          items: [
            ["Sync server: URL, custom root certificate", "ticket 44"],
            ["Sync account", "ticket 44"],
            ["Automatic synchronization", ""],
            ["Display synchronization status", ""],
            ["Fetch media on sync", ""],
            ["Allow sync on metered connections", ""],
            ["Network timeout", ""],
            ["One-way sync", ""],
          ],
        },
      ],
    },
    {
      title: "Notifications",
      sections: [
        {
          items: [
            ["Notify when", ""],
            ["Vibrate", ""],
            ["Blink light", ""],
          ],
        },
      ],
    },
    {
      title: "Backups",
      sections: [
        {
          items: [
            ["Minutes between automatic backups", ""],
            ["Daily backups to keep", ""],
            ["Weekly backups to keep", ""],
            ["Monthly backups to keep", ""],
          ],
        },
      ],
    },
    {
      title: "Advanced",
      sections: [
        {
          items: [["Hayaki directory", ""]],
        },
        {
          title: "Workarounds",
          items: [
            ["Disable card hardware render", ""],
            ["Type answer into the card", ""],
            ["Disable single-field edit mode", ""],
            ["Replace newlines with HTML", ""],
            ["Focus ‘type in answer’", ""],
            ["Allow all files in media imports", ""],
            ["Allow templates to record audio", ""],
          ],
        },
        {
          title: "Plugins",
          items: [
            ["Enable AnkiDroid API", ""],
            ["Third-party API apps", ""],
          ],
        },
        {
          title: "Security",
          items: [
            ["Globally allow dangerous JavaScript APIs", ""],
            ["Allow cards to launch other apps", ""],
          ],
        },
      ],
    },
    {
      title: "About",
      sections: [
        {
          items: [
            ["Version, build date, Engine and FSRS versions", ""],
            ["Unofficial fork, source, licence", ""],
            ["Changelog, copy debug info", ""],
          ],
        },
      ],
    },
    {
      title: "Developer options",
      sections: [
        {
          items: [
            ["Shown once switched on from About; kept as it is, in English", ""],
          ],
        },
      ],
    },
  ],
  dropped: [
    ["New study screen", "one study screen (ADR-0004)"],
    ["Enable gestures", "#17"],
    ["App bar buttons (35 settings)", "#29"],
    ["Fullscreen mode", "#9"],
    ["Show top bar", "#10"],
    ["Answer buttons position", "#3"],
    ["Show large answer buttons", "#15"],
    ["Show ETA, Show deck title", "always shown, #7 and #8"],
    ["Text to speech, Reset languages", "#2 and #19"],
    ["Double scrolling", "#18"],
    ["Bindings: save voice, pen colour", "#21 and #24"],
  ],
  flagged: [
    ["Review reminders", "replaces Notifications behind the ‘Enable new notifications’ developer option"],
    ["Switch profile", "behind its developer option"],
  ],
}

const zh: typeof en = {
  groups: [
    {
      title: "常规",
      sections: [
        {
          items: [
            ["语言", ""],
            ["新卡片的默认牌组", ""],
            ["将剪贴板图片粘贴为 PNG", ""],
            ["按两次返回键返回/退出", ""],
            ["在卡片浏览器中显示文件名", "来自外观"],
            ["“Anki 卡片”菜单", "全局"],
            ["“卡片浏览器”菜单", "全局"],
            ["全屏导航抽屉", "仅平板"],
          ],
        },
      ],
    },
    {
      title: "外观",
      sections: [
        {
          items: [
            ["主题：跟随系统、亮色、深色、黑色", "主题与夜间主题合一"],
            ["夜间主题：深色、黑色", "配合跟随系统"],
            ["牌组列表背景：选择、删除", ""],
          ],
        },
      ],
    },
    {
      title: "学习",
      sections: [
        {
          title: "计划任务",
          items: [
            ["次日开始时间", "来自复习"],
            ["提前学习的限制", "来自复习"],
            ["限制每次学习的时长", "来自复习"],
          ],
        },
        {
          title: "学习界面",
          items: [
            ["保持屏幕常亮", "来自复习"],
            ["居中对齐", "新做，#11"],
            ["框架样式", ""],
            ["隐藏系统栏", ""],
            ["忽略显示屏刘海", ""],
            ["显示回答反馈", ""],
            ["显示剩余卡片数", ""],
            ["在有音频的卡片上显示播放按钮", ""],
          ],
        },
        {
          title: "回答按钮",
          items: [
            ["显示回答按钮", ""],
            ["在回答按钮上方显示下次复习时间", ""],
            ["隐藏“困难”和“简单”按钮", ""],
          ],
        },
        {
          title: "工具栏",
          items: [
            ["工具栏操作（拖放）", ""],
            ["工具栏位置", ""],
          ],
        },
      ],
    },
    {
      title: "控制",
      sections: [
        {
          items: [
            ["9 点触摸", ""],
            ["滑动灵敏度", ""],
            ["显示键盘快捷键", ""],
            ["学习界面：52 个操作，各可绑定轻按、手势、按键和手柄按钮", "回答、旗标、卡片、笔记、导航、媒体、白板、其他、用户动作 1–9"],
            ["预览：14 个操作", ""],
          ],
        },
      ],
    },
    {
      title: "无障碍功能",
      sections: [
        {
          items: [
            ["卡片缩放", ""],
            ["图片缩放", "新做，#12"],
            ["回答按钮大小", "沿用旧值，#16"],
            ["卡片浏览器字体缩放", ""],
            ["“显示答案”按钮长按时间", "新做，#4"],
            ["两次轻按时间间隔", "新做，#5"],
            ["删除动画", ""],
          ],
        },
      ],
    },
    {
      title: "同步",
      sections: [
        {
          items: [
            ["同步服务器：URL、自定义根证书", "工单 44"],
            ["同步帐户", "工单 44"],
            ["自动同步", ""],
            ["显示同步状态", ""],
            ["同步时获取媒体文件", ""],
            ["允许通过按流量计费连接同步", ""],
            ["网络超时", ""],
            ["单向同步", ""],
          ],
        },
      ],
    },
    {
      title: "通知",
      sections: [
        {
          items: [
            ["通知类型", ""],
            ["振动", ""],
            ["呼吸灯", ""],
          ],
        },
      ],
    },
    {
      title: "备份",
      sections: [
        {
          items: [
            ["自动备份时间间隔（以分钟计）", ""],
            ["要保留的每日备份", ""],
            ["要保留的每周备份", ""],
            ["要保留的每月备份", ""],
          ],
        },
      ],
    },
    {
      title: "高级设置",
      sections: [
        {
          items: [["Hayaki 路径", ""]],
        },
        {
          title: "调试选项",
          items: [
            ["禁用卡牌硬件渲染", ""],
            ["在卡片中输入答案", ""],
            ["禁用单字段编辑模式", ""],
            ["用 HTML 替换换行", ""],
            ["定位答案输入框", ""],
            ["导入所有媒体文件", ""],
            ["允许模板录制音频", ""],
          ],
        },
        {
          title: "插件",
          items: [
            ["启用 AnkiDroid API", ""],
            ["第三方 API 应用", ""],
          ],
        },
        {
          title: "安全性",
          items: [
            ["全局允许危险的 JavaScript APIs", ""],
            ["允许卡片启动其他应用", ""],
          ],
        },
      ],
    },
    {
      title: "关于",
      sections: [
        {
          items: [
            ["版本、构建日期、引擎与 FSRS 版本", ""],
            ["非官方分支、源代码、许可证", ""],
            ["更新日志、复制调试信息", ""],
          ],
        },
      ],
    },
    {
      title: "开发者选项",
      sections: [
        {
          items: [["在“关于”中开启后显示；保持原样，仅英文", ""]],
        },
      ],
    },
  ],
  dropped: [
    ["新学习屏", "只有一个学习界面（ADR-0004）"],
    ["启用手势", "#17"],
    ["顶栏按钮（35 项设置）", "#29"],
    ["全屏显示", "#9"],
    ["显示顶部栏", "#10"],
    ["显示答案按钮位置", "#3"],
    ["增加答题按钮大小", "#15"],
    ["显示剩余时间、显示牌组标题", "始终显示，#7、#8"],
    ["文本转语音、重置语言", "#2、#19"],
    ["双滚动", "#18"],
    ["绑定：保存录音、画笔颜色", "#21、#24"],
  ],
  flagged: [
    ["复习提醒", "开启开发者选项“启用新通知”后取代通知"],
    ["切换配置文件", "在其开发者选项之后"],
  ],
}

export const settingsMap: Record<SLang, typeof en> = { en, zh }
