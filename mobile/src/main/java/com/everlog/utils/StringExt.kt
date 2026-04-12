package com.everlog.utils

import android.os.Build
import android.text.Html
import android.text.Spanned

fun String.makeSingleLine(): String {
    return this.replace("\n"," ")
}

fun String.fromHtml(): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(this)
    }
}

fun String.append(separator: String, text: String): String {
    return this + String.format("%s%s", if (this.isNotEmpty()) separator else "", text)
}