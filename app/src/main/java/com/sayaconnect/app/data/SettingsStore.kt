package com.sayaconnect.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("sayaconnect_settings", Context.MODE_PRIVATE)
    var uiLanguage by mutableStateOf(prefs.getString("uiLanguage", "en") ?: "en")
        private set
    var targetLanguage by mutableStateOf(prefs.getString("targetLanguage", "my") ?: "my")
        private set
    var darkMode by mutableStateOf(prefs.getString("darkMode", "system") ?: "system")
        private set
    var wallpaper by mutableStateOf(prefs.getString("wallpaper", "Aurora") ?: "Aurora")
        private set
    var customWallpaper by mutableStateOf(prefs.getString("customWallpaper", "") ?: "")
        private set
    var autoTranslate by mutableStateOf(prefs.getBoolean("autoTranslate", false))
        private set
    var lowData by mutableStateOf(prefs.getBoolean("lowData", true))
        private set

    fun setUiLanguage(v: String) { uiLanguage = v; prefs.edit().putString("uiLanguage", v).apply() }
    fun setTargetLanguage(v: String) { targetLanguage = v; prefs.edit().putString("targetLanguage", v).apply() }
    fun setDarkMode(v: String) { darkMode = v; prefs.edit().putString("darkMode", v).apply() }
    fun setWallpaper(v: String) { wallpaper = v; prefs.edit().putString("wallpaper", v).apply() }
    fun setCustomWallpaper(v: String) { customWallpaper = v; prefs.edit().putString("customWallpaper", v).apply() }
    fun setAutoTranslate(v: Boolean) { autoTranslate = v; prefs.edit().putBoolean("autoTranslate", v).apply() }
    fun setLowData(v: Boolean) { lowData = v; prefs.edit().putBoolean("lowData", v).apply() }
}

val languages = linkedMapOf(
    "my" to "မြန်မာ (Burmese)", "en" to "English", "th" to "ไทย (Thai)",
    "zh" to "中文 (Chinese)", "ja" to "日本語 (Japanese)", "ko" to "한국어 (Korean)",
    "hi" to "हिन्दी (Hindi)", "bn" to "বাংলা (Bengali)", "id" to "Bahasa Indonesia",
    "vi" to "Tiếng Việt", "es" to "Español", "fr" to "Français", "de" to "Deutsch",
    "ar" to "العربية", "pt" to "Português", "ru" to "Русский", "tr" to "Türkçe"
)
private val burmese = mapOf(
    "Chats" to "စာတိုများ", "Friends" to "သူငယ်ချင်းများ", "Profile" to "ပရိုဖိုင်",
    "Settings" to "ဆက်တင်များ", "Explore" to "ရှာဖွေရန်", "Sign in" to "ဝင်ရောက်မည်",
    "Create account" to "အကောင့်ဖွင့်မည်", "Email address" to "အီးမေးလ်", "Password" to "စကားဝှက်",
    "Username" to "အသုံးပြုသူအမည်", "Display name" to "ဖော်ပြမည့်အမည်", "Bio" to "ကိုယ်ရေးအကျဉ်း",
    "Continue" to "ဆက်လုပ်မည်", "Search username" to "အမည်ဖြင့်ရှာရန်", "Add friend" to "သူငယ်ချင်းထည့်မည်",
    "Accept" to "လက်ခံမည်", "Send" to "ပို့မည်", "Type a message" to "စာရေးပါ",
    "Video call" to "ဗီဒီယိုခေါ်ဆိုမှု", "Incoming call" to "ဝင်လာသောခေါ်ဆိုမှု",
    "Decline" to "ငြင်းပယ်မည်", "Answer" to "ဖြေမည်", "End call" to "ခေါ်ဆိုမှုအဆုံးသတ်မည်",
    "Edit profile" to "ပရိုဖိုင်ပြင်မည်", "Save" to "သိမ်းမည်", "App language" to "အက်ပ်ဘာသာစကား",
    "Translation language" to "ဘာသာပြန်ရန်ဘာသာစကား", "Auto translate" to "အလိုအလျောက်ဘာသာပြန်",
    "Appearance" to "အသွင်အပြင်", "Wallpaper" to "နောက်ခံပုံ", "Light" to "အလင်း",
    "Dark" to "အမှောင်", "System" to "စနစ်အတိုင်း", "Low data mode" to "ဒေတာသက်သာစေမည့်ပုံစံ",
    "Sign out" to "အကောင့်ထွက်မည်", "Music" to "သီချင်း", "Upload music" to "သီချင်းထည့်မည်",
    "Upload photo" to "ဓာတ်ပုံထည့်မည်", "Friend requests" to "သူငယ်ချင်းတောင်းဆိုမှုများ",
    "No chats yet" to "စာတိုမရှိသေးပါ", "No friends yet" to "သူငယ်ချင်းမရှိသေးပါ",
    "Group call" to "အဖွဲ့လိုက်ခေါ်မည်", "Start a group call" to "အဖွဲ့လိုက် ဖုန်းခေါ်ရန်",
    "Select 2–3 friends · Up to 4 participants" to "သူငယ်ချင်း ၂–၃ ယောက်ရွေးပါ · စုစုပေါင်း ၄ ယောက်အထိ",
    "Video" to "ဗီဒီယို", "Voice" to "အသံ", "Start call" to "စတင်ခေါ်မည်",
    "Cancel" to "ပယ်ဖျက်မည်", "End group call" to "အဖွဲ့လိုက်ခေါ်ဆိုမှု အဆုံးသတ်မည်",
    "Leave call" to "ခေါ်ဆိုမှုမှ ထွက်မည်", "You" to "သင်", "Mic" to "မိုက်",
    "Camera" to "ကင်မရာ", "Flip" to "ကင်မရာပြောင်း", "React" to "တုံ့ပြန်",
    "Waiting for friends to accept the invitation…" to "သူငယ်ချင်းများ လက်ခံရန် စောင့်ဆိုင်းနေသည်…",
    "Incoming group call" to "အဖွဲ့လိုက် ဖုန်းခေါ်ဆိုမှု"
)
fun t(label: String, uiLanguage: String) = if (uiLanguage == "my") burmese[label] ?: label else label
