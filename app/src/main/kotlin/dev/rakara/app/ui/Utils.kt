package dev.rakara.app.ui

import java.net.URLEncoder

internal fun encode(s: String): String =
    URLEncoder.encode(s, "UTF-8")