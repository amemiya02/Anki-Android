// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ichi2.anki.ComposeScreenshotTest
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.junit.Test

/**
 * The type scale as Compose draws it, with a wrapped paragraph in each language: proof that line
 * height suits Chinese and Japanese as well as Latin.
 *
 * ```sh
 * ./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot --tests 'com.ichi2.compose.theme.TypeScaleScreenshotTest'
 * ```
 */
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
class TypeScaleScreenshotTest : ComposeScreenshotTest() {
    @Test
    fun type_scale() =
        capture("type_scale") {
            val type = MaterialTheme.foundation.type
            val (title, paragraph) = sample(language)
            Column(Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(20.dp)) {
                Text(title, style = type.screenTitle)
                Text(title, style = type.pageTitle)
                Text(paragraph, style = type.lead)
                Text("$title 骨 直 Deck 1234", style = type.deckName)
                Text("$title 骨 直 Deck 1234", style = type.deckNameNested)
                Text("12 · 34 · 567", style = type.count)
                Text(paragraph, style = type.body)
                Text(paragraph, style = type.caption)
                Text(title, style = type.label)
            }
        }

    private fun sample(language: LanguageConfig) =
        when (language) {
            LanguageConfig.ENGLISH -> "Decks" to "Today you studied 24 cards in 6 minutes. Tomorrow 31 cards are due, including 8 new ones."
            SIMPLIFIED_CHINESE -> "牌组" to "今天你在 6 分钟内学习了 24 张卡片。明天有 31 张卡片到期，其中包括 8 张新卡片。骨直角。"
            JAPANESE -> "デッキ" to "今日は 6 分で 24 枚のカードを学習しました。明日は新しいカード 8 枚を含む 31 枚が期日です。骨直角。"
            ARABIC -> "مجموعات" to "درست اليوم 24 بطاقة في 6 دقائق. غدًا تستحق 31 بطاقة، منها 8 بطاقات جديدة."
        }
}
