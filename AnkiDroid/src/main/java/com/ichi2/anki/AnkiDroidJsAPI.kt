// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2020 Mani infinyte01@gmail.com

package com.ichi2.anki

import android.content.Context
import androidx.lifecycle.lifecycleScope
import anki.scheduler.CardAnswer.Rating
import com.github.zafarkhaja.semver.Version
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_BURT_NOTE
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_BURY_CARD
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_ERROR
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_FLAG_CARD
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_MARK_CARD
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_SET_DUE
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_SUSPEND_CARD
import com.ichi2.anki.AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_SUSPEND_NOTE
import com.ichi2.anki.AnkiDroidJsAPIConstants.flagCommands
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.browser.search.SearchString
import com.ichi2.anki.cardviewer.ViewerCommand
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.destinations.BrowserDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.common.utils.ext.stringIterable
import com.ichi2.anki.libanki.Card
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.Decks
import com.ichi2.anki.libanki.Note
import com.ichi2.anki.libanki.SortOrder
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.reviewer.FullScreenMode
import com.ichi2.anki.security.AppPermissions
import com.ichi2.anki.security.DangerousJsApiPermission
import com.ichi2.anki.security.DangerousJsPermissionDeniedException
import com.ichi2.anki.servicelayer.rescheduleCards
import com.ichi2.anki.servicelayer.resetCards
import com.ichi2.anki.snackbar.setMaxLines
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

typealias JvmBoolean = Boolean
typealias JvmInt = Int
typealias JvmFloat = Float
typealias JvmLong = Long
typealias JvmString = String

open class AnkiDroidJsAPI internal constructor(
    private val context: Context,
    private val showMessage: (String) -> Unit,
    private val cardApi: CardApi,
    private val legacyActivity: AbstractFlashcardViewer? = null,
) {
    constructor(activity: AbstractFlashcardViewer) : this(
        context = activity,
        showMessage = { activity.showSnackbar(it) },
        cardApi =
            object : CardApi {
                override val isDisplayingAnswer: Boolean get() = activity.isDisplayingAnswer

                override suspend fun getCard(): Card = checkNotNull(activity.currentCard)

                override suspend fun getCardData(includeEta: Boolean) = activity.getCardDataForJsApi()

                override suspend fun showOrAnswer(rating: Rating?) {
                    if (rating == null) activity.displayCardAnswer() else activity.flipOrAnswerCard(rating)
                }
            },
        legacyActivity = activity,
    )

    private val activity: AbstractFlashcardViewer
        get() = checkNotNull(legacyActivity)

    private val currentCard: Card
        get() = activity.currentCard!!

    private val getColUnsafe: Collection
        get() = activity.getColUnsafe

    /**
     Javascript Interface class for calling Java function from AnkiDroid WebView
     see js-api.js for available functions
     */

    private val permissions by lazy { AppPermissions(context, showMessage) }

    // Text to speech
    private val talker by lazy { JavaScriptTTS() }

    // Speech to Text
    private val speechRecognizer by lazy { JavaScriptSTT(context) }

    open fun convertToByteArray(
        apiContract: ApiContract,
        boolean: Boolean,
    ): ByteArray = ApiResult.Boolean(apiContract.isValid, boolean).toString().toByteArray()

    open fun convertToByteArray(
        apiContract: ApiContract,
        int: Int,
    ): ByteArray = ApiResult.Integer(apiContract.isValid, int).toString().toByteArray()

    open fun convertToByteArray(
        apiContract: ApiContract,
        long: Long,
    ): ByteArray = ApiResult.Long(apiContract.isValid, long).toString().toByteArray()

    open fun convertToByteArray(
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

        activity.showSnackbar(snackbarMsg, Snackbar.LENGTH_INDEFINITE) {
            setMaxLines(3)
            setAction(CommonString.reviewer_invalid_api_version_visit_documentation) {
                activity.openUrl("https://github.com/ankidroid/Anki-Android/wiki")
            }
        }
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

    /**
     * Handle js api request,
     * some of the methods are overridden in Reviewer.kt and default values are returned.
     * @param methodName
     * @param bytes
     * @param returnDefaultValues `true` if default values should be returned (if non-[Reviewer])
     * @return
     */
    @NeedsTest("setNoteTags: Test that tags are set for all edge cases")
    open suspend fun handleJsApiRequest(
        methodName: String,
        bytes: ByteArray,
        returnDefaultValues: Boolean = true,
    ) = withContext(Dispatchers.Main) {
        // the method will call to set the card supplied data and is valid version for each api request
        val apiContract = parseJsApiContract(bytes)!!
        // if api not init or is api not called from reviewer then return default -1
        // also other action will not be modified
        if (!apiContract.isValid or returnDefaultValues) {
            return@withContext convertToByteArray(apiContract, -1)
        }

        handleCardRequest(methodName, apiContract)?.let { return@withContext it }

        val apiParams = apiContract.cardSuppliedData

        return@withContext try {
            when (methodName) {
                "toggleFlag" -> {
                    if (apiParams !in flagCommands) {
                        showDeveloperContact(
                            ANKI_JS_ERROR_CODE_FLAG_CARD,
                            apiContract.cardSuppliedDeveloperContact,
                        )
                        return@withContext convertToByteArray(apiContract, false)
                    }
                    convertToByteArray(
                        apiContract,
                        activity.executeCommand(flagCommands[apiParams]!!),
                    )
                }

                "markCard" ->
                    processAction({
                        activity.executeCommand(ViewerCommand.MARK)
                    }, apiContract, ANKI_JS_ERROR_CODE_MARK_CARD, ::convertToByteArray)

                "buryCard" ->
                    processAction(
                        activity::buryCard,
                        apiContract,
                        ANKI_JS_ERROR_CODE_BURY_CARD,
                        ::convertToByteArray,
                    )

                "buryNote" ->
                    processAction(
                        activity::buryNote,
                        apiContract,
                        ANKI_JS_ERROR_CODE_BURT_NOTE,
                        ::convertToByteArray,
                    )

                "suspendCard" ->
                    processAction(
                        activity::suspendCard,
                        apiContract,
                        ANKI_JS_ERROR_CODE_SUSPEND_CARD,
                        ::convertToByteArray,
                    )

                "suspendNote" ->
                    processAction(
                        activity::suspendNote,
                        apiContract,
                        ANKI_JS_ERROR_CODE_SUSPEND_NOTE,
                        ::convertToByteArray,
                    )

                "setCardDue" -> {
                    try {
                        val days = apiParams.toInt()
                        if (days !in 0..9999) {
                            showDeveloperContact(
                                ANKI_JS_ERROR_CODE_SET_DUE,
                                apiContract.cardSuppliedDeveloperContact,
                            )
                            return@withContext convertToByteArray(apiContract, false)
                        }
                        activity.launchCatchingTask {
                            activity.rescheduleCards(listOf(currentCard.id), days)
                        }
                        return@withContext convertToByteArray(apiContract, true)
                    } catch (_: NumberFormatException) {
                        showDeveloperContact(
                            ANKI_JS_ERROR_CODE_SET_DUE,
                            apiContract.cardSuppliedDeveloperContact,
                        )
                        return@withContext convertToByteArray(apiContract, false)
                    }
                }

                "resetProgress" -> {
                    val cardIds = listOf(currentCard.id)
                    activity.launchCatchingTask { activity.resetCards(cardIds) }
                    convertToByteArray(apiContract, true)
                }

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
                "searchCard" -> {
                    with(activity) { navigate(BrowserDestination.Search(query = apiParams, allDecks = false)) }
                    convertToByteArray(apiContract, true)
                }

                "searchCardWithCallback" -> ankiSearchCardWithCallback(apiContract)
                "addTagToCard" -> {
                    activity.runOnUiThread { activity.showTagsDialog() }
                    convertToByteArray(apiContract, true)
                }

                "isInFullscreen" -> convertToByteArray(apiContract, activity.isFullscreen)
                "setFullscreenButtonsAndMenu" -> setFullscreenMode(FullScreenMode.BUTTONS_AND_MENU, apiContract)
                "setFullscreenButtonsOnly" -> setFullscreenMode(FullScreenMode.BUTTONS_ONLY, apiContract)
                "setFullscreenAllGone" -> setFullscreenMode(FullScreenMode.FULLSCREEN_ALL_GONE, apiContract)
                "isTopbarShown" -> convertToByteArray(apiContract, activity.prefShowTopbar)
                "isInNightMode" -> convertToByteArray(apiContract, activity.isInNightMode)
                "enableHorizontalScrollbar" -> {
                    activity.webView!!.isHorizontalScrollBarEnabled = apiParams.toBoolean()
                    convertToByteArray(apiContract, true)
                }

                "enableVerticalScrollbar" -> {
                    activity.webView!!.isVerticalScrollBarEnabled = apiParams.toBoolean()
                    convertToByteArray(apiContract, true)
                }

                "showNavigationDrawer" -> {
                    activity.onNavigationPressed()
                    convertToByteArray(apiContract, true)
                }

                "showOptionsMenu" -> {
                    activity.openOptionsMenu()
                    convertToByteArray(apiContract, true)
                }

                "showToast" -> {
                    val jsonObject = JSONObject(apiParams)
                    val text = jsonObject.getString("text")
                    val shortLength = jsonObject.optBoolean("shortLength", true)
                    val msgDecode = activity.decodeUrl(text)
                    showThemedToast(context, msgDecode, shortLength)
                    convertToByteArray(apiContract, true)
                }

                "addTagToNote" -> {
                    val jsonObject = JSONObject(apiParams)
                    val noteId = jsonObject.getLong("noteId")
                    val tag = jsonObject.getString("tag")
                    if (noteId != currentCard.nid) {
                        permissions.requirePermission(DangerousJsApiPermission.MODIFY_TAGS)
                    }
                    val note =
                        getColUnsafe.getNote(noteId).apply {
                            addTag(tag)
                        }
                    getColUnsafe.updateNote(note)
                    convertToByteArray(apiContract, true)
                }

                "setNoteTags" -> {
                    val jsonObject = JSONObject(apiParams)
                    val noteId = currentCard.nid
                    val tags = jsonObject.getJSONArray("tags")
                    withCol {
                        fun Note.setTagsFromList(tagList: List<String>) {
                            val sanitizedTags = tagList.map { it.trim() }
                            val spaces = "\\s|\u3000".toRegex()
                            if (sanitizedTags.any { it.contains(spaces) }) {
                                throw IllegalArgumentException("Tags cannot contain spaces")
                            }
                            val tagsAsString = this@withCol.tags.join(sanitizedTags)
                            setTagsFromStr(this@withCol, tagsAsString)
                        }

                        val note =
                            getNote(noteId).apply {
                                setTagsFromList(tags.stringIterable().toList())
                            }
                        updateNote(note)
                    }
                    convertToByteArray(apiContract, true)
                }

                "sttSetLanguage" ->
                    convertToByteArray(
                        apiContract,
                        speechRecognizer.setLanguage(apiParams),
                    )

                "sttStart" -> {
                    val callback =
                        object : JavaScriptSTT.SpeechRecognitionCallback {
                            override fun onResult(results: List<String>) {
                                activity.lifecycleScope.launch {
                                    val apiResult =
                                        ApiResult.success(
                                            Json.encodeToString(
                                                ListSerializer(String.serializer()),
                                                results,
                                            ),
                                        )
                                    val jsonEncodedString =
                                        withContext(Dispatchers.Default) {
                                            JSONObject.quote(apiResult.toString())
                                        }
                                    activity.webView!!.evaluateJavascript(
                                        "ankiSttResult($jsonEncodedString)",
                                        null,
                                    )
                                }
                            }

                            override fun onError(errorMessage: String) {
                                activity.lifecycleScope.launch {
                                    val apiResult = ApiResult.failure(errorMessage)
                                    val jsonEncodedString =
                                        withContext(Dispatchers.Default) {
                                            JSONObject.quote(apiResult.toString())
                                        }
                                    activity.webView!!.evaluateJavascript(
                                        "ankiSttResult($jsonEncodedString)",
                                        null,
                                    )
                                }
                            }
                        }
                    speechRecognizer.setRecognitionCallback(callback)
                    convertToByteArray(apiContract, speechRecognizer.start())
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

    /** The subset available in the study screen. Remaining methods require the old reviewer. */
    internal suspend fun handleCardRequest(
        methodName: String,
        bytes: ByteArray,
    ): ByteArray? =
        withContext(Dispatchers.Main) {
            val contract = requireNotNull(parseJsApiContract(bytes))
            if (!contract.isValid) return@withContext convertToByteArray(contract, -1)
            handleCardRequest(methodName, contract)
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

        suspend fun getCard(): Card

        suspend fun getCardData(includeEta: Boolean = false): CardDataForJsApi

        /** Flip the Card if the question is showing. With a rating, grade an already flipped Card. */
        suspend fun showOrAnswer(rating: Rating?)
    }

    private fun setFullscreenMode(
        mode: FullScreenMode,
        apiContract: ApiContract,
    ): ByteArray {
        val result = (legacyActivity as? Reviewer)?.setFullscreenMode(mode) ?: false
        return convertToByteArray(apiContract, result)
    }

    private fun processAction(
        action: () -> Boolean,
        apiContract: ApiContract,
        errorCode: Int,
        conversion: (ApiContract, Boolean) -> ByteArray,
    ): ByteArray {
        val status = action()
        if (!status) {
            showDeveloperContact(errorCode, apiContract.cardSuppliedDeveloperContact)
        }
        return conversion(apiContract, status)
    }

    @NeedsTest("needs coverage")
    private suspend fun ankiSearchCardWithCallback(apiContract: ApiContract): ByteArray =
        withContext(Dispatchers.Main) {
            permissions.requirePermission(DangerousJsApiPermission.QUERY_COLLECTION)
            val cards =
                try {
                    val searchString = withCol { SearchString.fromUserInput(apiContract.cardSuppliedData) }.getOrThrow()
                    searchForRows(searchString, SortOrder.UseCollectionOrdering, CardsOrNotes.CARDS)
                        .map { withCol { getCard(it.cardOrNoteId) } }
                } catch (_: Exception) {
                    activity.webView!!.evaluateJavascript(
                        "console.log('${context.getString(CommonString.search_card_js_api_no_results)}')",
                        null,
                    )
                    showDeveloperContact(AnkiDroidJsAPIConstants.ANKI_JS_ERROR_CODE_SEARCH_CARD, apiContract.cardSuppliedDeveloperContact)
                    return@withContext convertToByteArray(apiContract, false)
                }
            val searchResult: MutableList<String> = ArrayList()
            for (card in cards) {
                val jsonObject = JSONObject()
                val fieldsData = card.note(getColUnsafe).fields
                val fieldsName = card.noteType(getColUnsafe).fieldsNames

                val noteId = card.nid
                val cardId = card.id
                jsonObject.put("cardId", cardId)
                jsonObject.put("noteId", noteId)

                val jsonFieldObject = JSONObject()
                fieldsName.zip(fieldsData).forEach { pair ->
                    jsonFieldObject.put(pair.component1(), pair.component2())
                }
                jsonObject.put("fieldsData", jsonFieldObject)

                searchResult.add(jsonObject.toString())
            }

            // quote result to prevent JSON injection attack
            val jsonEncodedString = JSONObject.quote(searchResult.toString())
            activity.runOnUiThread {
                activity.webView!!.evaluateJavascript("ankiSearchCard($jsonEncodedString)", null)
            }
            convertToByteArray(apiContract, true)
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
