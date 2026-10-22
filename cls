[35m.github/workflows/build.yml[m[36m:[m[32m31[m[36m:[m          # Install packages needed by [1;31mRakaRa[m
[35m.github/workflows/build.yml[m[36m:[m[32m55[m[36m:[m          name: [1;31mRakaRa[m-debug-apk
[35m.gitignore[m[36m:[m[32m28[m[36m:[m# ---------- [1;31mRakaRa[m artifacts ----------
[35mapp/src/main/kotlin/dev/rakara/app/ui/HomeScreen.kt[m[36m:[m[32m1[m[36m:[m﻿package dev.[1;31mRakaRa[m.app.ui
[35mapp/src/main/kotlin/dev/rakara/app/ui/HomeScreen.kt[m[36m:[m[32m28[m[36m:[mimport dev.[1;31mRakaRa[m.app.[1;31mRakaRa[mApp
[35mapp/src/main/kotlin/dev/rakara/app/ui/HomeScreen.kt[m[36m:[m[32m29[m[36m:[mimport dev.[1;31mRakaRa[m.app.data.ReadMark
[35mapp/src/main/kotlin/dev/rakara/app/ui/HomeScreen.kt[m[36m:[m[32m30[m[36m:[mimport dev.[1;31mRakaRa[m.app.vm.HomeViewModel
[35mapp/src/main/kotlin/dev/rakara/app/ui/HomeScreen.kt[m[36m:[m[32m31[m[36m:[mimport dev.[1;31mRakaRa[m.source.SManga
[35mapp/src/main/kotlin/dev/rakara/app/ui/HomeScreen.kt[m[36m:[m[32m36[m[36m:[m    val app = [1;31mRakaRa[mApp.from(LocalContext.current)
[35mapp/src/main/kotlin/dev/rakara/app/ui/HomeScreen.kt[m[36m:[m[32m49[m[36m:[m                        "[1;31mRakaRa[m",
[35mapp/src/main/kotlin/dev/rakara/app/ui/HomeScreen.kt[m[36m:[m[32m275[m[36m:[m                        color = MaterialTheme.[1;31mRakaRa[mExtra.gold,
[35mapp/src/main/kotlin/dev/rakara/app/ui/SettingsScreen.kt[m[36m:[m[32m1[m[36m:[m﻿package dev.[1;31mRakaRa[m.app.ui
[35mapp/src/main/kotlin/dev/rakara/app/ui/SettingsScreen.kt[m[36m:[m[32m55[m[36m:[m            InfoRow("Aplikasi", "[1;31mRakaRa[m")
[35mapp/src/main/kotlin/dev/rakara/app/ui/SplashScreen.kt[m[36m:[m[32m1[m[36m:[m﻿package dev.[1;31mRakaRa[m.app.ui
[35mapp/src/main/kotlin/dev/rakara/app/ui/SplashScreen.kt[m[36m:[m[32m23[m[36m:[mimport dev.[1;31mRakaRa[m.app.R
[35mapp/src/main/kotlin/dev/rakara/app/ui/SplashScreen.kt[m[36m:[m[32m53[m[36m:[m                "Selamat datang di [1;31mRakaRa[m",
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m1[m[36m:[m﻿package dev.[1;31mRakaRa[m.app.ui
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m23[m[36m:[m// PALETTE â€” Brand [1;31mRakaRa[m
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m58[m[36m:[mdata class [1;31mRakaRa[mExtraColors(
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m68[m[36m:[mprivate val LightExtra = [1;31mRakaRa[mExtraColors(
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m78[m[36m:[mprivate val DarkExtra = [1;31mRakaRa[mExtraColors(
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m88[m[36m:[mval Local[1;31mRakaRa[mExtra = staticCompositionLocalOf { LightExtra }
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m90[m[36m:[mval MaterialTheme.[1;31mRakaRa[mExtra: [1;31mRakaRa[mExtraColors
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m91[m[36m:[m    @Composable get() = Local[1;31mRakaRa[mExtra.current
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m155[m[36m:[mprivate val [1;31mRakaRa[mTypography = Typography(
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m234[m[36m:[mfun [1;31mRakaRa[mTheme(
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m240[m[36m:[m        Local[1;31mRakaRa[mExtra provides if (dark) DarkExtra else LightExtra,
[35mapp/src/main/kotlin/dev/rakara/app/ui/Theme.kt[m[36m:[m[32m244[m[36m:[m            typography = [1;31mRakaRa[mTypography,
[35mapp/src/main/res/values/strings.xml[m[36m:[m[32m3[m[36m:[m    <string name="app_name">[1;31mRakaRa[m</string>
[35mapp/src/main/res/values/strings.xml[m[36m:[m[32m4[m[36m:[m    <string name="engine_repo_url" translatable="false">https://github.com/EurekaElderitch/[1;31mRakaRa[m-public/releases/latest/download/index.json</string>
[35mengine-manager/src/main/kotlin/dev/rakara/engine/EngineManager.kt[m[36m:[m[32m1[m[36m:[m﻿package dev.[1;31mRakaRa[m.engine
[35mengine-manager/src/main/kotlin/dev/rakara/engine/EngineManager.kt[m[36m:[m[32m5[m[36m:[mimport dev.[1;31mRakaRa[m.source.Source
[35mengine-manager/src/main/kotlin/dev/rakara/engine/EngineManager.kt[m[36m:[m[32m77[m[36m:[m                .onFailure { android.util.Log.w("[1;31mRakaRa[m", "load ${e.manifest.id} gagal", it) }
[35mengine-manager/src/main/kotlin/dev/rakara/engine/EngineUpdater.kt[m[36m:[m[32m1[m[36m:[m﻿package dev.[1;31mRakaRa[m.engine
[35mengine-manager/src/main/kotlin/dev/rakara/engine/EngineUpdater.kt[m[36m:[m[32m101[m[36m:[m            .header("User-Agent", "[1;31mRakaRa[m/updater")
[35mengine-manager/src/main/kotlin/dev/rakara/engine/EngineUpdater.kt[m[36m:[m[32m132[m[36m:[m            .header("User-Agent", "[1;31mRakaRa[m/updater")
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m1[m[36m:[m﻿package dev.[1;31mRakaRa[m.engines.example
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m3[m[36m:[mimport dev.[1;31mRakaRa[m.source.FilterList
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m4[m[36m:[mimport dev.[1;31mRakaRa[m.source.MangasPage
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m5[m[36m:[mimport dev.[1;31mRakaRa[m.source.SChapter
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m6[m[36m:[mimport dev.[1;31mRakaRa[m.source.SManga
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m7[m[36m:[mimport dev.[1;31mRakaRa[m.source.SPage
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m8[m[36m:[mimport dev.[1;31mRakaRa[m.source.Source
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m20[m[36m:[m * Struktur method mengikuti kontrak dev.[1;31mRakaRa[m.source.Source.
[35mengines/example/src/main/kotlin/dev/rakara/engines/example/ExampleSource.kt[m[36m:[m[32m136[m[36m:[m            .header("User-Agent", "Mozilla/5.0 (compatible; [1;31mRakaRa[m/0.1)")
[35mpackage.json[m[36m:[m[32m2[m[36m:[m  "name": "[1;31mRakaRa[m-tools",
[35mpackage.json[m[36m:[m[32m6[m[36m:[m  "description": "[1;31mRakaRa[m engine packager & dev tools",
[35msettings.gradle.kts[m[36m:[m[32m23[m[36m:[mrootProject.name = "[1;31mRakaRa[m"
[35msource-api/src/main/kotlin/dev/rakara/source/Source.kt[m[36m:[m[32m1[m[36m:[m﻿package dev.[1;31mRakaRa[m.source
[35msource-api/src/main/kotlin/dev/rakara/source/Source.kt[m[36m:[m[32m4[m[36m:[m * Kontrak utama yang menghubungkan [1;31mRakaRa[m (host) dengan engine (.rkx).
[35mtools/build-rkx.mjs[m[36m:[m[32m3[m[36m:[m * build-rkx.mjs â€” packager engine .rkx untuk [1;31mRakaRa[m
[35mtools/build-rkx.mjs[m[36m:[m[32m98[m[36m:[mconst versionFile = path.join(root, "source-api/src/main/resources/META-INF/[1;31mRakaRa[m-source-api.version");
[35mtools/build-rkx.mjs[m[36m:[m[32m277[m[36m:[mconsole.log(`  # lalu: [1;31mRakaRa[m â†’ Engine â†’ + Install .rkx`);
[35mtools/install-engine.sh[m[36m:[m[32m90[m[36m:[mecho "  1. Buka [1;31mRakaRa[m"
[35mtools/install-engine.sh[m[36m:[m[32m98[m[36m:[m    read -r -p "Launch [1;31mRakaRa[m sekarang? [y/N] " ans
[35mtools/install-engine.sh[m[36m:[m[32m100[m[36m:[m        adb -s "$FIRST_DEVICE" shell monkey -p dev.[1;31mRakaRa[m.app -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
[35mtools/install-engine.sh[m[36m:[m[32m101[m[36m:[m        echo "â–¸ [1;31mRakaRa[m launched."
[35mtools/sync-public.ps1[m[36m:[m[32m10[m[36m:[m$publicDir = Join-Path (Split-Path -Parent $root) "[1;31mRakaRa[m-public"
[35mtools/sync-public.ps1[m[36m:[m[32m67[m[36m:[m    & git config user.name "[1;31mRakaRa[m Release"
[35mtools/sync-public.ps1[m[36m:[m[32m68[m[36m:[m    & git config user.email "release@[1;31mRakaRa[m.local"
