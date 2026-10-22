package dev.rakara.source

import java.security.MessageDigest


fun sha256Hex(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }


fun Source.autoId(): Long =
    sha256Hex("$name|$lang").take(15).toLong(16)