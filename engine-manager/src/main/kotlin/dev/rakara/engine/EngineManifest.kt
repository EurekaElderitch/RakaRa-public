package dev.rakara.engine

import kotlinx.serialization.Serializable

@Serializable
data class EngineManifest(
    val id: String,
    val name: String,
    val version: Int,
    val apiVersion: Int,
    val className: String,
    val lang: String = "id",
    val nsfw: Boolean = false,
    val icon: String? = null,
    val sha256: String,
    val runtime: String = "dex",
) {
    companion object {
        const val CURRENT_API = 1
    }
}