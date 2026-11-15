package dev.rakara.engine

import dev.rakara.source.Source
import java.io.File


data class InstalledEngine(
    val manifest: EngineManifest,
    val dir: File,      
    val enabled: Boolean,
)


data class LoadedEngine(
    val manifest: EngineManifest,
    val dir: File,
    val source: Source,
) {
    
    fun iconFile(): File? {
        val name = manifest.icon ?: return null
        val f = File(dir, name)
        return if (f.exists()) f else null
    }
}