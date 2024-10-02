package cn.yiiguxing.plugin.translate.tts.microsoft

import cn.yiiguxing.plugin.translate.TranslationStorages
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Lang.*
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.Http.setUserAgent
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlinx.collections.immutable.toImmutableList
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.util.*

/**
 * Microsoft Edge TTS voice manager.
 */
internal object EdgeTTSVoiceManager {

    private val TTS_DIR = TranslationStorages.DATA_DIRECTORY.resolve("tts")
    private val VOICES_FILE = TTS_DIR.resolve("ms-edge-tts-voices.json")


    // https://github.com/microsoft/cognitive-services-speech-sdk-js/blob/master/src/sdk/Synthesizer.ts#L92
    @Suppress("SpellCheckingInspection")
    private val localeToDefaultVoice: Map<String, String> = mapOf(
        "af-ZA" to "af-ZA-AdriNeural",
        "am-ET" to "am-ET-AmehaNeural",
        "ar-AE" to "ar-AE-FatimaNeural",
        "ar-BH" to "ar-BH-AliNeural",
        "ar-DZ" to "ar-DZ-AminaNeural",
        "ar-EG" to "ar-EG-SalmaNeural",
        "ar-IQ" to "ar-IQ-BasselNeural",
        "ar-JO" to "ar-JO-SanaNeural",
        "ar-KW" to "ar-KW-FahedNeural",
        "ar-LY" to "ar-LY-ImanNeural",
        "ar-MA" to "ar-MA-JamalNeural",
        "ar-QA" to "ar-QA-AmalNeural",
        "ar-SA" to "ar-SA-HamedNeural",
        "ar-SY" to "ar-SY-AmanyNeural",
        "ar-TN" to "ar-TN-HediNeural",
        "ar-YE" to "ar-YE-MaryamNeural",
        "bg-BG" to "bg-BG-BorislavNeural",
        "bn-BD" to "bn-BD-NabanitaNeural",
        "bn-IN" to "bn-IN-BashkarNeural",
        "ca-ES" to "ca-ES-JoanaNeural",
        "cs-CZ" to "cs-CZ-AntoninNeural",
        "cy-GB" to "cy-GB-AledNeural",
        "da-DK" to "da-DK-ChristelNeural",
        "de-AT" to "de-AT-IngridNeural",
        "de-CH" to "de-CH-JanNeural",
        "de-DE" to "de-DE-KatjaNeural",
        "el-GR" to "el-GR-AthinaNeural",
        "en-AU" to "en-AU-NatashaNeural",
        "en-CA" to "en-CA-ClaraNeural",
        "en-GB" to "en-GB-LibbyNeural",
        "en-HK" to "en-HK-SamNeural",
        "en-IE" to "en-IE-ConnorNeural",
        "en-IN" to "en-IN-NeerjaNeural",
        "en-KE" to "en-KE-AsiliaNeural",
        "en-NG" to "en-NG-AbeoNeural",
        "en-NZ" to "en-NZ-MitchellNeural",
        "en-PH" to "en-PH-JamesNeural",
        "en-SG" to "en-SG-LunaNeural",
        "en-TZ" to "en-TZ-ElimuNeural",
        "en-US" to "en-US-JennyNeural",
        "en-ZA" to "en-ZA-LeahNeural",
        "es-AR" to "es-AR-ElenaNeural",
        "es-BO" to "es-BO-MarceloNeural",
        "es-CL" to "es-CL-CatalinaNeural",
        "es-CO" to "es-CO-GonzaloNeural",
        "es-CR" to "es-CR-JuanNeural",
        "es-CU" to "es-CU-BelkysNeural",
        "es-DO" to "es-DO-EmilioNeural",
        "es-EC" to "es-EC-AndreaNeural",
        "es-ES" to "es-ES-AlvaroNeural",
        "es-GQ" to "es-GQ-JavierNeural",
        "es-GT" to "es-GT-AndresNeural",
        "es-HN" to "es-HN-CarlosNeural",
        "es-MX" to "es-MX-DaliaNeural",
        "es-NI" to "es-NI-FedericoNeural",
        "es-PA" to "es-PA-MargaritaNeural",
        "es-PE" to "es-PE-AlexNeural",
        "es-PR" to "es-PR-KarinaNeural",
        "es-PY" to "es-PY-MarioNeural",
        "es-SV" to "es-SV-LorenaNeural",
        "es-US" to "es-US-AlonsoNeural",
        "es-UY" to "es-UY-MateoNeural",
        "es-VE" to "es-VE-PaolaNeural",
        "et-EE" to "et-EE-AnuNeural",
        "fa-IR" to "fa-IR-DilaraNeural",
        "fi-FI" to "fi-FI-SelmaNeural",
        "fil-PH" to "fil-PH-AngeloNeural",
        "fr-BE" to "fr-BE-CharlineNeural",
        "fr-CA" to "fr-CA-SylvieNeural",
        "fr-CH" to "fr-CH-ArianeNeural",
        "fr-FR" to "fr-FR-DeniseNeural",
        "ga-IE" to "ga-IE-ColmNeural",
        "gl-ES" to "gl-ES-RoiNeural",
        "gu-IN" to "gu-IN-DhwaniNeural",
        "he-IL" to "he-IL-AvriNeural",
        "hi-IN" to "hi-IN-MadhurNeural",
        "hr-HR" to "hr-HR-GabrijelaNeural",
        "hu-HU" to "hu-HU-NoemiNeural",
        "id-ID" to "id-ID-ArdiNeural",
        "is-IS" to "is-IS-GudrunNeural",
        "it-IT" to "it-IT-IsabellaNeural",
        "ja-JP" to "ja-JP-NanamiNeural",
        "jv-ID" to "jv-ID-DimasNeural",
        "kk-KZ" to "kk-KZ-AigulNeural",
        "km-KH" to "km-KH-PisethNeural",
        "kn-IN" to "kn-IN-GaganNeural",
        "ko-KR" to "ko-KR-SunHiNeural",
        "lo-LA" to "lo-LA-ChanthavongNeural",
        "lt-LT" to "lt-LT-LeonasNeural",
        "lv-LV" to "lv-LV-EveritaNeural",
        "mk-MK" to "mk-MK-AleksandarNeural",
        "ml-IN" to "ml-IN-MidhunNeural",
        "mr-IN" to "mr-IN-AarohiNeural",
        "ms-MY" to "ms-MY-OsmanNeural",
        "mt-MT" to "mt-MT-GraceNeural",
        "my-MM" to "my-MM-NilarNeural",
        "nb-NO" to "nb-NO-PernilleNeural",
        "nl-BE" to "nl-BE-ArnaudNeural",
        "nl-NL" to "nl-NL-ColetteNeural",
        "pl-PL" to "pl-PL-AgnieszkaNeural",
        "ps-AF" to "ps-AF-GulNawazNeural",
        "pt-BR" to "pt-BR-FranciscaNeural",
        "pt-PT" to "pt-PT-DuarteNeural",
        "ro-RO" to "ro-RO-AlinaNeural",
        "ru-RU" to "ru-RU-SvetlanaNeural",
        "si-LK" to "si-LK-SameeraNeural",
        "sk-SK" to "sk-SK-LukasNeural",
        "sl-SI" to "sl-SI-PetraNeural",
        "so-SO" to "so-SO-MuuseNeural",
        "sr-RS" to "sr-RS-NicholasNeural",
        "su-ID" to "su-ID-JajangNeural",
        "sv-SE" to "sv-SE-SofieNeural",
        "sw-KE" to "sw-KE-RafikiNeural",
        "sw-TZ" to "sw-TZ-DaudiNeural",
        "ta-IN" to "ta-IN-PallaviNeural",
        "ta-LK" to "ta-LK-KumarNeural",
        "ta-SG" to "ta-SG-AnbuNeural",
        "te-IN" to "te-IN-MohanNeural",
        "th-TH" to "th-TH-PremwadeeNeural",
        "tr-TR" to "tr-TR-AhmetNeural",
        "uk-UA" to "uk-UA-OstapNeural",
        "ur-IN" to "ur-IN-GulNeural",
        "ur-PK" to "ur-PK-AsadNeural",
        "uz-UZ" to "uz-UZ-MadinaNeural",
        "vi-VN" to "vi-VN-HoaiMyNeural",
        "zh-CN" to "zh-CN-XiaoxiaoNeural",
        "zh-HK" to "zh-HK-HiuMaanNeural",
        "zh-TW" to "zh-TW-HsiaoChenNeural",
        "zu-ZA" to "zu-ZA-ThandoNeural",
    )
    private val languageToLocale: Map<Lang, String> = mapOf(
        AFRIKAANS to "af-ZA",
        AMHARIC to "am-ET",
        ARABIC to "ar-SA",
        BENGALI to "bn-IN",
        BULGARIAN to "bg-BG",
        CATALAN to "ca-ES",
        CHINESE to "zh-CN",
        CHINESE_SIMPLIFIED to "zh-CN",
        CHINESE_CANTONESE to "zh-HK",
        CHINESE_TRADITIONAL to "zh-CN",
        CROATIAN to "hr-HR",
        CZECH to "cs-CZ",
        DANISH to "da-DK",
        DUTCH to "nl-NL",
        ENGLISH to "en-US",
        ENGLISH_AMERICAN to "en-US",
        ENGLISH_BRITISH to "en-GB",
        ESTONIAN to "et-EE",
        FINNISH to "fi-FI",
        FRENCH to "fr-FR",
        FRENCH_CANADA to "fr-CA",
        GERMAN to "de-DE",
        GREEK to "el-GR",
        GUJARATI to "gu-IN",
        HEBREW to "he-IL",
        HINDI to "hi-IN",
        HUNGARIAN to "hu-HU",
        ICELANDIC to "is-IS",
        INDONESIAN to "id-ID",
        IRISH to "ga-IE",
        ITALIAN to "it-IT",
        JAPANESE to "ja-JP",
        KANNADA to "kn-IN",
        KAZAKH to "kk-KZ",
        KHMER to "km-KH",
        KOREAN to "ko-KR",
        LAO to "lo-LA",
        LATVIAN to "lv-LV",
        LITHUANIAN to "lt-LT",
        MACEDONIAN to "mk-MK",
        MALAY to "ms-MY",
        MALAYALAM to "ml-IN",
        MALTESE to "mt-MT",
        MARATHI to "mr-IN",
        MYANMAR to "my-MM",
        NORWEGIAN to "nb-NO",
        PASHTO to "ps-AF",
        PERSIAN to "fa-IR",
        POLISH to "pl-PL",
        PORTUGUESE to "pt-PT",
        PORTUGUESE_BRAZILIAN to "pt-BR",
        PORTUGUESE_PORTUGUESE to "pt-PT",
        ROMANIAN to "ro-RO",
        RUSSIAN to "ru-RU",
        SERBIAN_CYRILLIC to "sr-RS",
        SLOVAK to "sk-SK",
        SLOVENIAN to "sl-SI",
        SPANISH to "es-ES",
        SWEDISH to "sv-SE",
        TAMIL to "ta-IN",
        TELUGU to "te-IN",
        THAI to "th-TH",
        TURKISH to "tr-TR",
        UKRAINIAN to "uk-UA",
        URDU to "ur-IN",
        UZBEK to "uz-UZ",
        VIETNAMESE to "vi-VN",
        WELSH to "cy-GB",
    )


    /**
     * Checks if the specified [language][lang] is supported.
     */
    fun isSupportLanguage(lang: Lang): Boolean = languageToLocale.containsKey(lang)

    /**
     * Gets the default voice name for the specified [language][lang].
     */
    fun getDefaultVoiceName(lang: Lang): String {
        val locale = languageToLocale[lang] ?: Locale.getDefault().toLanguageTag()
        return localeToDefaultVoice[locale] ?: localeToDefaultVoice["en-US"]!!
    }

    /**
     * Fetches the list of voices.
     */
    @RequiresBackgroundThread
    fun fetchVoiceList(): List<EdgeTTSVoice> {
        val url = "$EDGE_TTS_VOICES_URL?TrustedClientToken=$TRUSTED_CLIENT_TOKEN"
        return Http.request<List<EdgeTTSVoice>>(url, typeOfT = type<List<EdgeTTSVoice>>()) { setUserAgent() }
            .toImmutableList()
            .also { saveVoicesToLocale(it) }
    }

    /**
     * Gets the list of voices.
     */
    @RequiresBackgroundThread
    fun getVoices(): List<EdgeTTSVoice> {
        return getVoicesFromLocale()?.toImmutableList() ?: fetchVoiceList()
    }

    private fun getVoicesFromLocale(): List<EdgeTTSVoice>? {
        createDirectoriesIfNotExists(TTS_DIR)
        if (Files.notExists(VOICES_FILE)) {
            return null
        }
        return try {
            Files.newBufferedReader(VOICES_FILE).use {
                Gson().fromJson(it, type<List<EdgeTTSVoice>>())
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveVoicesToLocale(voices: List<EdgeTTSVoice>) {
        try {
            createDirectoriesIfNotExists(TTS_DIR)
            VOICES_FILE.writeSafe { os ->
                BufferedWriter(OutputStreamWriter(os)).use {
                    GsonBuilder().setPrettyPrinting().create().toJson(voices, it)
                }
            }
        } catch (e: IOException) {
            thisLogger().w("Failed to save voices to locale.", e)
        }
    }
}