plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(project(":source-api"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
    compileOnly("org.jsoup:jsoup:1.18.1")
}

tasks.jar {
    archiveBaseName.set("engine")
    exclude("kotlin/**")
    exclude("META-INF/*.kotlin_module")
    exclude("META-INF/*.version")
    exclude("META-INF/MANIFEST.MF")
}