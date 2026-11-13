package com.babelmic.app.util;

import com.babelmic.app.model.Language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry of all languages supported by BabelMic.
 *
 * <p>All languages here are supported by both:
 * <ul>
 *   <li>Google ML Kit on-device translation (the full list is at
 *       <a href="https://developers.google.com/ml-kit/language/translation/translation-language-support">
 *       ML Kit language support</a>).</li>
 *   <li>Android's on-device SpeechRecognizer (varies by device, but all listed
 *       languages are available on stock Android 10+).</li>
 *   <li>Android's built-in TextToSpeech engine.</li>
 * </ul>
 * </p>
 *
 * <p>To add a new language, simply add an entry to {@link #buildLanguageList()}.</p>
 */
public final class LanguageManager {

    // Prevent instantiation — this is a static utility class
    private LanguageManager() {}

    private static final List<Language> LANGUAGES = buildLanguageList();

    /**
     * @return Unmodifiable list of all supported languages, alphabetically sorted
     *         by English display name.
     */
    public static List<Language> getSupportedLanguages() {
        return LANGUAGES;
    }

    /**
     * Find a {@link Language} by its BCP-47 tag (case-insensitive).
     *
     * @param tag BCP-47 tag, e.g. {@code "en"}, {@code "fr"}
     * @return The matching Language, or {@code null} if not found.
     */
    public static Language findByTag(String tag) {
        if (tag == null) return null;
        for (Language lang : LANGUAGES) {
            if (lang.getBcp47Tag().equalsIgnoreCase(tag)) return lang;
        }
        return null;
    }

    /**
     * Find a {@link Language} by its English display name (case-insensitive).
     *
     * @param name English name, e.g. {@code "French"}
     * @return The matching Language, or {@code null} if not found.
     */
    public static Language findByDisplayName(String name) {
        if (name == null) return null;
        for (Language lang : LANGUAGES) {
            if (lang.getDisplayName().equalsIgnoreCase(name)) return lang;
        }
        return null;
    }

    // ─── Language catalogue ───────────────────────────────────────────────────

    private static List<Language> buildLanguageList() {
        List<Language> list = new ArrayList<>();

        //          BCP-47   Display Name          Native Name           Flag
        list.add(new Language("af",  "Afrikaans",          "Afrikaans",          "🇿🇦"));
        list.add(new Language("sq",  "Albanian",           "Shqip",              "🇦🇱"));
        list.add(new Language("ar",  "Arabic",             "العربية",             "🇸🇦"));
        list.add(new Language("be",  "Belarusian",         "Беларуская",         "🇧🇾"));
        list.add(new Language("bn",  "Bengali",            "বাংলা",               "🇧🇩"));
        list.add(new Language("bg",  "Bulgarian",          "Български",          "🇧🇬"));
        list.add(new Language("ca",  "Catalan",            "Català",             "🏴"));
        list.add(new Language("zh",  "Chinese",            "中文",                "🇨🇳"));
        list.add(new Language("hr",  "Croatian",           "Hrvatski",           "🇭🇷"));
        list.add(new Language("cs",  "Czech",              "Čeština",            "🇨🇿"));
        list.add(new Language("da",  "Danish",             "Dansk",              "🇩🇰"));
        list.add(new Language("nl",  "Dutch",              "Nederlands",         "🇳🇱"));
        list.add(new Language("en",  "English",            "English",            "🇬🇧"));
        list.add(new Language("eo",  "Esperanto",          "Esperanto",          "🌍"));
        list.add(new Language("et",  "Estonian",           "Eesti",              "🇪🇪"));
        list.add(new Language("fi",  "Finnish",            "Suomi",              "🇫🇮"));
        list.add(new Language("fr",  "French",             "Français",           "🇫🇷"));
        list.add(new Language("gl",  "Galician",           "Galego",             "🏴"));
        list.add(new Language("ka",  "Georgian",           "ქართული",            "🇬🇪"));
        list.add(new Language("de",  "German",             "Deutsch",            "🇩🇪"));
        list.add(new Language("el",  "Greek",              "Ελληνικά",           "🇬🇷"));
        list.add(new Language("gu",  "Gujarati",           "ગુજરાતી",             "🇮🇳"));
        list.add(new Language("ht",  "Haitian Creole",     "Kreyòl ayisyen",     "🇭🇹"));
        list.add(new Language("he",  "Hebrew",             "עברית",               "🇮🇱"));
        list.add(new Language("hi",  "Hindi",              "हिन्दी",               "🇮🇳"));
        list.add(new Language("hu",  "Hungarian",          "Magyar",             "🇭🇺"));
        list.add(new Language("is",  "Icelandic",          "Íslenska",           "🇮🇸"));
        list.add(new Language("id",  "Indonesian",         "Bahasa Indonesia",   "🇮🇩"));
        list.add(new Language("ga",  "Irish",              "Gaeilge",            "🇮🇪"));
        list.add(new Language("it",  "Italian",            "Italiano",           "🇮🇹"));
        list.add(new Language("ja",  "Japanese",           "日本語",              "🇯🇵"));
        list.add(new Language("kn",  "Kannada",            "ಕನ್ನಡ",               "🇮🇳"));
        list.add(new Language("ko",  "Korean",             "한국어",              "🇰🇷"));
        list.add(new Language("lt",  "Lithuanian",         "Lietuvių",           "🇱🇹"));
        list.add(new Language("lv",  "Latvian",            "Latviešu",           "🇱🇻"));
        list.add(new Language("mk",  "Macedonian",         "Македонски",         "🇲🇰"));
        list.add(new Language("ms",  "Malay",              "Bahasa Melayu",      "🇲🇾"));
        list.add(new Language("mt",  "Maltese",            "Malti",              "🇲🇹"));
        list.add(new Language("mr",  "Marathi",            "मराठी",               "🇮🇳"));
        list.add(new Language("no",  "Norwegian",          "Norsk",              "🇳🇴"));
        list.add(new Language("fa",  "Persian",            "فارسی",               "🇮🇷"));
        list.add(new Language("pl",  "Polish",             "Polski",             "🇵🇱"));
        list.add(new Language("pt",  "Portuguese",         "Português",          "🇵🇹"));
        list.add(new Language("ro",  "Romanian",           "Română",             "🇷🇴"));
        list.add(new Language("ru",  "Russian",            "Русский",            "🇷🇺"));
        list.add(new Language("sk",  "Slovak",             "Slovenčina",         "🇸🇰"));
        list.add(new Language("sl",  "Slovenian",          "Slovenščina",        "🇸🇮"));
        list.add(new Language("es",  "Spanish",            "Español",            "🇪🇸"));
        list.add(new Language("sv",  "Swedish",            "Svenska",            "🇸🇪"));
        list.add(new Language("sw",  "Swahili",            "Kiswahili",          "🇰🇪"));
        list.add(new Language("tl",  "Tagalog",            "Tagalog",            "🇵🇭"));
        list.add(new Language("ta",  "Tamil",              "தமிழ்",               "🇮🇳"));
        list.add(new Language("te",  "Telugu",             "తెలుగు",              "🇮🇳"));
        list.add(new Language("th",  "Thai",               "ภาษาไทย",            "🇹🇭"));
        list.add(new Language("tr",  "Turkish",            "Türkçe",             "🇹🇷"));
        list.add(new Language("uk",  "Ukrainian",          "Українська",         "🇺🇦"));
        list.add(new Language("ur",  "Urdu",               "اردو",                "🇵🇰"));
        list.add(new Language("vi",  "Vietnamese",         "Tiếng Việt",         "🇻🇳"));
        list.add(new Language("cy",  "Welsh",              "Cymraeg",            "🏴󠁧󠁢󠁷󠁬󠁳󠁿"));

        // Sort alphabetically by English display name
        Collections.sort(list, (a, b) ->
                a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));

        return Collections.unmodifiableList(list);
    }
}
