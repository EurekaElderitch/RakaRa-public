// Root build script — hanya deklarasi versi plugin, tidak ada dependencies di sini.

plugins {
    id("com.android.application") version "8.5.0" apply false
    id("com.android.library") version "8.5.0" apply false
    kotlin("android") version "1.9.24" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
}

tasks.register<Delete>("cleanDist") {
    delete(layout.projectDirectory.dir("dist"))
}

tasks.register("cleanAll") {
    group = "build"
    description = "Clean build outputs + dist/ + node_modules cache"
    dependsOn("clean")
    doLast {
        delete(layout.projectDirectory.dir("dist"))
        delete(layout.projectDirectory.dir("engines/example/build"))
        delete(layout.projectDirectory.dir("node_modules/.cache"))
        println("cleanAll selesai")
    }
}