// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2020 Mani infinyte01@gmail.com

package com.ichi2.anki

import android.content.Context
import anki.scheduler.CardAnswer.Rating
import com.github.zafarkhaja.semver.Version
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_BURT_NOTE
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_BURY_CARD
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_ERROR
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_FLAG_CARD
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_MARK_CARD
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_SET_DUE
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_SUSPEND_CARD
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_SUSPEND_NOTE
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.browser.search.SearchString
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.common.utils.ext.stringIterable
import com.ichi2.anki.libanki.Card
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.Decks
import com.ichi2.anki.libanki.Note
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.libanki.SortOrder
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.reviewer.FullScreenMode
import com.ichi2.anki.security.AppPermissions
import com.ichi2.anki.security.DangerousJsApiPermission
import com.ichi2.anki.security.DangerousJsPermissionDeniedException
import com.ichi2.utils.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.net.URLDecoder

typealias JvmBoolean = Boolean
typealias JvmInt = Int
typealias JvmFloat = Float
typealias JvmLong = Long
typealias JvmString = String

class AnkiDroidJsAPI internal constructor(
    private val context: Context,
    private val showMessage: (String) -> Unit,
    private val cardApi: CardApi,
    private val showDeveloperMessage: (String) -> Unit,
) {
    /**
     Javascript Interface class for calling Java function from AnkiDroid WebView
     see js-api.js for available functions
     */

    private val permissions by lazy { AppPermissions(context, showMessage) }

    // Text to speech
    private val talker by lazy { JavaScriptTTS() }

    // Speech to Text
    private val speechRecognizerDelegate = lazy { JavaScriptSTT(context) }
    private val speechRecognizer by speechRecognizerDelegate

    internal fun cancelSpeechRecognition() {
        if (speechRecognizerDelegate.isInitialized()) speechRecognizer.cancel()
    }

    private fun convertToByteArray(
        apiContract: ApiContract,
        boolean: Boolean,
    ): ByteArray = ApiResult.Boolean(apiContract.isValid, boolean).toString().toByteArray()

    private fun convertToByteArray(
        apiContract: ApiContract,
        int: Int,
    ): ByteArray = ApiResult.Integer(apiContract.isValid, int).toString().toByteArray()

    private fun convertToByteArray(
        apiContract: ApiContract,
        long: Long,
    ): ByteArray = ApiResult.Long(apiContract.isValid, long).toString().toByteArray()

    private fun convertToByteArray(
        apiContract: ApiContract,
        string: String,
    ): ByteArray = ApiResult.String(apiContract.isValid, string).toString().toByteArray()

    /**
     * The method parse json data and return api contract object
     * @param byteArray
     * @return apiContract or null
     */
    private fun parseJsApiContract(byteArray: ByteArray): ApiContract? {
        try {
            val data = JSONObject(byteArray.decodeToString())
            val cardSuppliedApiVersion = data.optString("version", "")
            val cardSuppliedDeveloperContact = data.optString("developer", "")
            val cardSuppliedData = data.optString("data", "")
            val isValid = requireApiVersion(cardSuppliedApiVersion, cardSuppliedDeveloperContact)
            return ApiContract(isValid, cardSuppliedDeveloperContact, cardSuppliedData)
        } catch (j: JSONException) {
            Timber.w(j)
            showMessage(context.getString(CommonString.invalid_json_data, j.localizedMessage))
        }
        return null
    }

    /*
     * see 02-strings.xml
     * Show Error code when mark card or flag card unsupported
     * 1 - mark card
     * 2 - flag card
     *
     * show developer contact if js api used in card is deprecated
     */
    private fun showDeveloperContact(
        errorCode: Int,
        apiDevContact: String,
    ) {
        val errorMsg: String = context.getString(CommonString.anki_js_error_code, errorCode)
        val snackbarMsg: String = context.getString(CommonString.api_version_developer_contact, apiDevContact, errorMsg)

        showDeveloperMessage(snackbarMsg)
    }

    /**
     * Supplied api version must be equal to current api version to call mark card, toggle flag functions etc.
     */
    private fun requireApiVersion(
        apiVer: String,
        apiDevContact: String,
    ): Boolean {
        try {
            if (apiDevContact.isEmpty() || apiVer.isEmpty()) {
                showMessage(context.getString(CommonString.invalid_json_data, ""))
                return false
            }
            val versionCurrent = Version.parse(AnkiDroidJsAPIConstants.CURRENT_JS_API_VERSION)
            val versionSupplied = Version.parse(apiVer)

            /*
             * if api major version equals to supplied major version then return true and also check for minor version and patch version
             * show toast for update and contact developer if need updates
             * otherwise return false
             */
            return when {
                versionSupplied == versionCurrent -> {
                    true
                }
                versionSupplied.isLowerThan(versionCurrent) -> {
                    showMessage(context.getString(CommonString.update_js_api_version, apiDevContact))
                    versionSupplied.isHigherThanOrEquivalentTo(Version.parse(AnkiDroidJsAPIConstants.MINIMUM_JS_API_VERSION))
                }
                else -> {
                    showMessage(context.getString(CommonString.valid_js_api_version, apiDevContact))
                    false
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "requireApiVersion::exception")
        }
        return false
    }

    /** Handles a request from an initialized study Card Template. */
    @NeedsTest("setNoteTags: Test that tags are set for all edge cases")
    suspend fun handleJsApiRequest(
        methodName: String,
        bytes: ByteArray,
    ) = withContext(Dispatchers.Main) {
        // the method will call to set the card supplied data and is valid version for each api request
        val apiContract = parseJsApiContract(bytes)!!
        if (!apiContract.isValid) {
            return@withContext convertToByteArray(apiContract, -1)
        }

        handleCardRequest(methodName, apiContract)?.let { return@withContext it }

        val apiParams = apiContract.cardSuppliedData

        return@withContext try {
            when (methodName) {
                "toggleFlag" -> {
                    val flag = Flag.entries.firstOrNull { it.name.lowercase() == apiParams }
                    if (flag == null) {
                        showDeveloperContact(ANKI_JS_ERROR_CODE_FLAG_CARD, apiContract.cardSuppliedDeveloperContact)
                        return@withContext convertToByteArray(apiContract, false)
                    }
                    convertToByteArray(apiContract, cardApi.toggleFlag(flag))
                }

                "markCard" -> processAction(ViewerAction.MARK, apiContract, ANKI_JS_ERROR_CODE_MARK_CARD)
                "buryCard" -> processAction(ViewerAction.BURY_CARD, apiContract, ANKI_JS_ERROR_CODE_BURY_CARD)
                "buryNote" -> processAction(ViewerAction.BURY_NOTE, apiContract, ANKI_JS_ERROR_CODE_BURT_NOTE)
                "suspendCard" -> processAction(ViewerAction.SUSPEND_CARD, apiContract, ANKI_JS_ERROR_CODE_SUSPEND_CARD)
                "suspendNote" -> processAction(ViewerAction.SUSPEND_NOTE, apiContract, ANKI_JS_ERROR_CODE_SUSPEND_NOTE)

                "setCardDue" -> {
                    val days = apiParams.toIntOrNull()
                    if (days == null || days !in 0..9999) {
                        showDeveloperContact(ANKI_JS_ERROR_CODE_SET_DUE, apiContract.cardSuppliedDeveloperContact)
                        return@withContext convertToByteArray(apiContract, false)
                    }
                    convertToByteArray(apiContract, cardApi.setCardDue(days))
                }

                "resetProgress" -> convertToByteArray(apiContract, cardApi.resetProgress())

                "isActiveNetworkMetered" ->
                    convertToByteArray(
                        apiContract,
                        NetworkUtils.isActiveNetworkMetered(),
                    )

                "ttsSetLanguage" -> convertToByteArray(apiContract, talker.setLanguage(apiParams))
                "ttsSpeak" -> {
                    val jsonObject = JSONObject(apiParams)
                    val text = jsonObject.getString("text")
                    val queueMode = jsonObject.getInt("queueMode")
                    convertToByteArray(apiContract, talker.speak(text, queueMode))
                }

                "ttsIsSpeaking" -> convertToByteArray(apiContract, talker.isSpeaking)
                "ttsSetPitch" ->
                    convertToByteArray(
                        apiContract,
                        talker.setPitch(apiParams.toFloat()),
                    )

                "ttsSetSpeechRate" ->
                    convertToByteArray(
                        apiContract,
                        talker.setSpeechRate(apiParams.toFloat()),
                    )

                "ttsFieldModifierIsAvailable" -> {
                    // Know if {{tts}} is supported - issue #10443
                    // Return false for now
                    convertToByteArray(apiContract, false)
                }

                "ttsStop" -> convertToByteArray(apiContract, talker.stop())
                "searchCard" -> convertToByteArray(apiContract, cardApi.searchCard(apiParams))
                "searchCardWithCallback" -> ankiSearchCardWithCallback(apiContract)
                "addTagToCard" -> convertToByteArray(apiContract, cardApi.performAction(ViewerAction.TAG))

                "isInFullscreen" -> convertToByteArray(apiContract, cardApi.isFullscreen)
                "setFullscreenButtonsAndMenu" ->
                    convertToByteArray(apiContract, cardApi.setFullscreenMode(FullScreenMode.BUTTONS_AND_MENU))
                "setFullscreenButtonsOnly" ->
                    convertToByteArray(apiContract, cardApi.setFullscreenMode(FullScreenMode.BUTTONS_ONLY))
                "setFullscreenAllGone" ->
                    convertToByteArray(apiContract, cardApi.setFullscreenMode(FullScreenMode.FULLSCREEN_ALL_GONE))
                "isTopbarShown" -> convertToByteArray(apiContract, cardApi.isTopbarShown)
                "isInNightMode" -> convertToByteArray(apiContract, cardApi.isInNightMode)
                "enableHorizontalScrollbar" -> convertToByteArray(apiContract, cardApi.setScrollbar(true, apiParams.toBoolean()))
                "enableVerticalScrollbar" -> convertToByteArray(apiContract, cardApi.setScrollbar(false, apiParams.toBoolean()))
                "showNavigationDrawer" -> convertToByteArray(apiContract, cardApi.showNavigation())
                "showOptionsMenu" -> convertToByteArray(apiContract, cardApi.showOptionsMenu())

                "showToast" -> {
                    val jsonObject = JSONObject(apiParams)
                    val text = jsonObject.getString("text")
                    val shortLength = jsonObject.optBoolean("shortLength", true)
                    val msgDecode = decodeToast(text)
                    showThemedToast(context, msgDecode, shortLength)
                    convertToByteArray(apiContract, true)
                }

                "addTagToNote" -> {
                    val jsonObject = JSONObject(apiParams)
                    val noteId = jsonObject.getLong("noteId")
                    val tag = jsonObject.getString("tag")
                    val card = cardApi.getCard()
                    if (noteId != card.nid) {
                        permissions.requirePermission(DangerousJsApiPermission.MODIFY_TAGS)
                    }
                    val updated = cardApi.updateNoteTags(card, noteId) { note -> note.addTag(tag) }
                    convertToByteArray(apiContract, updated)
                }

                "setNoteTags" -> {
                    val card = cardApi.getCard()
                    val tags = JSONObject(apiParams).getJSONArray("tags")
                    val sanitizedTags = tags.stringIterable().map { it.trim() }
                    val spaces = "\\s|\u3000".toRegex()
                    require(sanitizedTags.none { it.contains(spaces) }) { "Tags cannot contain spaces" }
                    val updated =
                        cardApi.updateNoteTags(card, card.nid) { note ->
                            note.setTagsFromStr(this, this.tags.join(sanitizedTags))
                        }
                    convertToByteArray(apiContract, updated)
                }

                "sttSetLanguage" ->
                    convertToByteArray(
                        apiContract,
                        speechRecognizer.setLanguage(apiParams),
                    )

                "sttStart" -> {
                    val emit = cardApi.captureCallback()
                    val callback =
                        object : JavaScriptSTT.SpeechRecognitionCallback {
                            override fun onResult(results: List<String>) {
                                val data = Json.encodeToString(ListSerializer(String.serializer()), results)
                                emit("ankiSttResult(${JSONObject.quote(ApiResult.success(data).toString())})")
                            }

                            override fun onError(errorMessage: String) {
                                emit("ankiSttResult(${JSONObject.quote(ApiResult.failure(errorMessage).toString())})")
                            }
                        }
                    if (!cardApi.canStartSpeechRecognition()) {
                        callback.onError("Audio recording is disabled for card templates.")
                        return@withContext convertToByteArray(apiContract, false)
                    }
                    convertToByteArray(apiContract, speechRecognizer.start(callback))
                }

                "sttStop" -> convertToByteArray(apiContract, speechRecognizer.stop())
                else -> {
                    showDeveloperContact(
                        ANKI_JS_ERROR_CODE_ERROR,
                        apiContract.cardSuppliedDeveloperContact,
                    )
                    throw Exception("unhandled request: $methodName")
                }
            }
        } catch (_: DangerousJsPermissionDeniedException) {
            return@withContext convertToByteArray(apiContract, false)
        }
    }

    private suspend fun handleCardRequest(
        methodName: String,
        contract: ApiContract,
    ): ByteArray? =
        when (methodName) {
            "init" -> convertToByteArray(contract, true)
            "newCardCount" -> convertToByteArray(contract, cardApi.getCardData().newCardCount)
            "lrnCardCount" -> convertToByteArray(contract, cardApi.getCardData().lrnCardCount)
            "revCardCount" -> convertToByteArray(contract, cardApi.getCardData().revCardCount)
            "eta" -> convertToByteArray(contract, cardApi.getCardData(includeEta = true).eta)
            "nextTime1" -> convertToByteArray(contract, cardApi.getCardData().nextTime1)
            "nextTime2" -> convertToByteArray(contract, cardApi.getCardData().nextTime2)
            "nextTime3" -> convertToByteArray(contract, cardApi.getCardData().nextTime3)
            "nextTime4" -> convertToByteArray(contract, cardApi.getCardData().nextTime4)

            "cardMark" ->
                convertToByteArray(
                    contract,
                    cardApi.getCard().let { card ->
                        withCol { card.note(this).hasTag(this, "marked") }
                    },
                )

            "cardFlag" -> convertToByteArray(contract, cardApi.getCard().userFlag())
            "cardReps" -> convertToByteArray(contract, cardApi.getCard().reps)
            "cardInterval" -> convertToByteArray(contract, cardApi.getCard().ivl)
            "cardFactor" -> convertToByteArray(contract, cardApi.getCard().factor)
            "cardMod" -> convertToByteArray(contract, cardApi.getCard().mod)
            "cardId" -> convertToByteArray(contract, cardApi.getCard().id)
            "cardNid" -> convertToByteArray(contract, cardApi.getCard().nid)
            "cardType" -> convertToByteArray(contract, cardApi.getCard().type.code)
            "cardDid" -> convertToByteArray(contract, cardApi.getCard().did)
            "cardLeft" -> convertToByteArray(contract, cardApi.getCard().left)
            "cardODid" -> convertToByteArray(contract, cardApi.getCard().oDid)
            "cardODue" -> convertToByteArray(contract, cardApi.getCard().oDue)
            "cardQueue" -> convertToByteArray(contract, cardApi.getCard().queue.code)
            "cardLapses" -> convertToByteArray(contract, cardApi.getCard().lapses)
            "cardDue" -> convertToByteArray(contract, cardApi.getCard().due)
            "deckName" ->
                convertToByteArray(
                    contract,
                    cardApi.getCard().did.let { did ->
                        withCol { Decks.basename(decks.name(did)) }
                    },
                )

            "getNoteTags" -> {
                val noteId = cardApi.getCard().nid
                val noteTags =
                    withCol {
                        getNote(noteId).tags
                    }
                convertToByteArray(contract, JSONArray(noteTags).toString())
            }

            "isDisplayingAnswer" -> convertToByteArray(contract, cardApi.isDisplayingAnswer)

            "showAnswer", "answerEase1", "answerEase2", "answerEase3", "answerEase4" -> {
                cardApi.showOrAnswer(answerRating(methodName))
                convertToByteArray(contract, true)
            }
            else -> null
        }

    internal interface CardApi {
        val isDisplayingAnswer: Boolean
        val isFullscreen: Boolean
        val isTopbarShown: Boolean
        val isInNightMode: Boolean

        suspend fun getCard(): Card

        suspend fun getCardData(includeEta: Boolean = false): CardDataForJsApi

        /** Flip the Card if the question is showing. With a rating, grade an already flipped Card. */
        suspend fun showOrAnswer(rating: Rating?)

        suspend fun toggleFlag(flag: Flag): Boolean

        suspend fun performAction(action: ViewerAction): Boolean

        suspend fun setCardDue(days: Int): Boolean

        suspend fun resetProgress(): Boolean

        suspend fun updateNoteTags(
            card: Card,
            noteId: NoteId,
            update: Collection.(Note) -> Unit,
        ): Boolean

        fun searchCard(query: String): Boolean

        fun setFullscreenMode(mode: FullScreenMode): Boolean

        fun setScrollbar(
            horizontal: Boolean,
            enabled: Boolean,
        ): Boolean

        fun showNavigation(): Boolean

        fun showOptionsMenu(): Boolean

        fun canStartSpeechRecognition(): Boolean

        /** Captures the page receiving an asynchronous result. */
        fun captureCallback(): (String) -> Unit
    }

    private suspend fun processAction(
        action: ViewerAction,
        apiContract: ApiContract,
        errorCode: Int,
    ): ByteArray {
        val status = cardApi.performAction(action)
        if (!status) {
            showDeveloperContact(errorCode, apiContract.cardSuppliedDeveloperContact)
        }
        return convertToByteArray(apiContract, status)
    }

    private fun decodeToast(text: String): String =
        try {
            URLDecoder.decode(text, Charsets.UTF_8.name())
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Error decoding template toast")
            showThemedToast(context, context.getString(CommonString.card_viewer_url_decode_error), true)
            ""
        }

    @NeedsTest("needs coverage")
    private suspend fun ankiSearchCardWithCallback(apiContract: ApiContract): ByteArray {
        permissions.requirePermission(DangerousJsApiPermission.QUERY_COLLECTION)
        val emit = cardApi.captureCallback()
        val results =
            try {
                withCol {
                    val search = SearchString.fromUserInput(apiContract.cardSuppliedData).getOrThrow()
                    val cards = findCards(search.value, SortOrder.UseCollectionOrdering)
                    JSONArray()
                        .apply {
                            for (id in cards) {
                                val card = getCard(id)
                                val note = card.note(this@withCol)
                                val fields = JSONObject()
                                note.notetype.fieldsNames.zip(note.fields).forEach { (name, value) ->
                                    fields.put(name, value)
                                }
                                put(JSONObject().put("cardId", card.id).put("noteId", card.nid).put("fieldsData", fields))
                            }
                        }.toString()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Card search failed")
                emit("console.log(${JSONObject.quote(context.getString(CommonString.search_card_js_api_no_results))})")
                showDeveloperContact(AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_SEARCH_CARD, apiContract.cardSuppliedDeveloperContact)
                return convertToByteArray(apiContract, false)
            }

        // Quote the JSON so collection fields cannot escape into the JavaScript expression.
        emit("ankiSearchCard(${JSONObject.quote(results)})")
        return convertToByteArray(apiContract, true)
    }

    open class CardDataForJsApi {
        var newCardCount: Int = -1
        var lrnCardCount: Int = -1
        var revCardCount: Int = -1
        var eta = -1
        var nextTime1 = ""
        var nextTime2 = ""
        var nextTime3 = ""
        var nextTime4 = ""
    }

    sealed class ApiResult protected constructor(
        private val status: JvmBoolean,
    ) {
        class Boolean(
            status: JvmBoolean,
            val value: JvmBoolean,
        ) : ApiResult(status) {
            override fun putValue(o: JSONObject) {
                o.put(VALUE_KEY, value)
            }
        }

        class Integer(
            status: JvmBoolean,
            val value: JvmInt,
        ) : ApiResult(status) {
            override fun putValue(o: JSONObject) {
                o.put(VALUE_KEY, value)
            }
        }

        class Float(
            status: JvmBoolean,
            val value: JvmFloat,
        ) : ApiResult(status) {
            override fun putValue(o: JSONObject) {
                o.put(VALUE_KEY, value)
            }
        }

        class Long(
            status: JvmBoolean,
            val value: JvmLong,
        ) : ApiResult(status) {
            override fun putValue(o: JSONObject) {
                o.put(VALUE_KEY, value)
            }
        }

        class String(
            status: JvmBoolean,
            val value: JvmString,
        ) : ApiResult(status) {
            override fun putValue(o: JSONObject) {
                o.put(VALUE_KEY, value)
            }
        }

        abstract fun putValue(o: JSONObject)

        override fun toString() =
            JSONObject()
                .apply {
                    put(SUCCESS_KEY, status)
                    putValue(this)
                }.toString()

        @Suppress("RemoveRedundantQualifierName") // we don't want `String(true, value)`
        companion object {
            fun success(value: JvmString) = ApiResult.String(true, value)

            fun failure(value: JvmString) = ApiResult.String(false, value)
        }
    }

    class ApiContract(
        val isValid: Boolean,
        val cardSuppliedDeveloperContact: String,
        val cardSuppliedData: String,
    )

    companion object {
        internal fun answerRating(methodName: String?): Rating? =
            when (methodName) {
                "answerEase1" -> Rating.AGAIN
                "answerEase2" -> Rating.HARD
                "answerEase3" -> Rating.GOOD
                "answerEase4" -> Rating.EASY
                else -> null
            }

        /**
         * Key for a success value.
         */
        const val VALUE_KEY = "value"
        const val SUCCESS_KEY = "success"
    }
}
