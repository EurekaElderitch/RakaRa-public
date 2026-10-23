# Interface yang di-load via DexClassLoader — jangan di-strip
-keep interface dev.rakara.source.Source { *; }
-keep class dev.rakara.source.** { *; }

# Manifest dibaca via kotlinx-serialization reflection
-keep class dev.rakara.engine.EngineManifest { *; }
-keepclassmembers class dev.rakara.engine.EngineManifest {
    <fields>;
    <init>(...);
}
-keepclassmembers class dev.rakara.engine.EngineManifest$$serializer {
    *** INSTANCE;
}

# DexClassLoader butuh constructor publik
-keepclassmembers class * implements dev.rakara.source.Source {
    public <init>();
}

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class dev.rakara.**$$serializer { *; }
-keepclassmembers class dev.rakara.** {
    *** Companion;
}
-keepclasseswithmembers class dev.rakara.** {
    kotlinx.serialization.KSerializer serializer(...);
}