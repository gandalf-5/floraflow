package com.floraflow.app.util

import java.util.Locale

object LocaleUtil {

    private val SUPPORTED = setOf("fr", "es", "de", "pt", "it", "ja", "zh")

    fun getDeviceLang(): String {
        val lang = Locale.getDefault().language
        return if (lang in SUPPORTED) lang else "en"
    }
}
