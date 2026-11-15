#!/usr/bin/env node
/**
 * build-rkx.mjs â€” packager engine .rkx untuk RakaRa
 *
 * Pemakaian:
 *   node tools/build-rkx.mjs <engineName> [--out <dir>] [--skip-gradle] [--keep-dex]
 *
 * Contoh:
 *   node tools/build-rkx.mjs example
 *   node tools/build-rkx.mjs example --out dist
 *   node tools/build-rkx.mjs example --skip-gradle      (jar sudah ada)
 *   node tools/build-rkx.mjs example --keep-dex         (jangan hapus dexout/)
 *
 * Prasyarat:
 *   - Node.js 22+
 *   - npm i adm-zip   (didefinisikan di package.json root)
 *   - ANDROID_HOME / ANDROID_SDK_ROOT menunjuk ke Android SDK
 *   - gradlew ada di root proyek
 *
 * Alur:
 *   1. Gradle build jar engine
 *   2. Ambil jar dari build/libs/
 *   3. jar â†’ classes.dex pakai d8
 *   4. Hitung sha256 dari classes.dex
 *   5. Tulis manifest.json + classes.dex (+ icon.png) ke .rkx (zip)
 *
 * Output:
 *   <out>/<manifest.id>-<manifest.version>.rkx
 */
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { execFileSync } from "node:child_process";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);

// ---------- resolve root proyek ----------
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");

// ---------- parse args ----------
const args = process.argv.slice(2);
const flags = new Set(args.filter((a) => a.startsWith("--")));
const positional = args.filter((a) => !a.startsWith("--"));
const outIdx = args.indexOf("--out");
const outDir = outIdx >= 0 ? args[outIdx + 1] : "dist";

const engineName = positional[0];
if (!engineName) {
    console.error("Usage: build-rkx.mjs <engineName> [--out dist] [--skip-gradle] [--keep-dex]");
    process.exit(1);
}

const skipGradle = flags.has("--skip-gradle");
const keepDex = flags.has("--keep-dex");

// ---------- dependencies ----------
let AdmZip;
try {
    AdmZip = require("adm-zip");
} catch {
    console.error("âœ– adm-zip belum terpasang. Jalankan: npm i");
    process.exit(1);
}

// ---------- paths ----------
const engineDir = path.join(root, "engines", engineName);
const manifestTplPath = path.join(engineDir, "manifest.template.json");
const iconPath = path.join(engineDir, "icon.png");
const libsDir = path.join(engineDir, "build", "libs");
const dexOut = path.join(engineDir, "build", "dexout");

if (!fs.existsSync(engineDir)) {
    console.error(`âœ– Folder engine tidak ada: ${engineDir}`);
    process.exit(1);
}
if (!fs.existsSync(manifestTplPath)) {
    console.error(`âœ– manifest.template.json tidak ada di ${engineDir}`);
    process.exit(1);
}

const manifest = JSON.parse(fs.readFileSync(manifestTplPath, "utf8"));

// ---------- validasi manifest ----------
const requiredFields = ["id", "name", "version", "apiVersion", "className"];
const missing = requiredFields.filter((f) => manifest[f] === undefined || manifest[f] === null);
if (missing.length) {
    console.error(`âœ– manifest.template.json kurang field: ${missing.join(", ")}`);
    process.exit(1);
}
if (manifest.sha256 !== "PACKAGER_FILLS_THIS" && manifest.sha256) {
    console.warn(`âš  sha256 di template bukan placeholder â€” akan ditimpa oleh packager.`);
}

// ---------- cek apiVersion vs source-api ----------
const versionFile = path.join(root, "source-api/src/main/resources/META-INF/RakaRa-source-api.version");
if (fs.existsSync(versionFile)) {
    const expected = fs.readFileSync(versionFile, "utf8").trim();
    if (String(manifest.apiVersion) !== expected) {
        console.error(
            `âœ– apiVersion mismatch:\n` +
            `  manifest.template.json = ${manifest.apiVersion}\n` +
            `  source-api version     = ${expected}\n` +
            `  â†’ perbaiki salah satu, lalu rebuild.`
        );
        process.exit(1);
    }
}

// ---------- 1. gradle build jar ----------
if (!skipGradle) {
    console.log(`â–¸ Building :engines:${engineName}:jar ...`);
    const gradlew = path.join(root, process.platform === "win32" ? "gradlew.bat" : "gradlew");
    if (!fs.existsSync(gradlew)) {
        console.error(`âœ– gradlew tidak ada di ${root}. Jalankan: gradle wrapper`);
        process.exit(1);
    }
    const task = `:engines:${engineName}:jar`;
    if (process.platform === "win32") {
        // Node 22+ tidak boleh spawn .bat langsung, harus lewat cmd.exe
        execFileSync("cmd.exe", ["/c", gradlew, task, "--console=plain"], {
            cwd: root,
            stdio: "inherit",
        });
    } else {
        execFileSync(gradlew, [task, "--console=plain"], {
            cwd: root,
            stdio: "inherit",
        });
    }
}

// ---------- 2. cari jar ----------
if (!fs.existsSync(libsDir)) {
    console.error(`âœ– build/libs tidak ada â€” jalankan gradle dulu atau hapus --skip-gradle`);
    process.exit(1);
}
const jars = fs
    .readdirSync(libsDir)
    .filter((f) => f.endsWith(".jar") && !f.endsWith("-sources.jar") && !f.endsWith("-javadoc.jar"));

if (jars.length === 0) {
    console.error(`âœ– Tidak ada .jar di ${libsDir}`);
    process.exit(1);
}
if (jars.length > 1) {
    console.error(`âœ– Ditemukan lebih dari satu .jar: ${jars.join(", ")}`);
    console.error(`  â†’ hapus jar lama di ${libsDir}, atau perbaiki build script engine.`);
    process.exit(1);
}
const jarPath = path.join(libsDir, jars[0]);
const jarSize = fs.statSync(jarPath).size;
console.log(`â–¸ Jar: ${path.relative(root, jarPath)} (${(jarSize / 1024).toFixed(1)} KB)`);

if (jarSize > 500 * 1024) {
    console.warn(
        `âš  Jar > 500 KB. Kemungkinan stdlib atau dependency ikut ter-bundle.\n` +
        `  Cek exclude("kotlin/**") di engines/${engineName}/build.gradle.kts.`
    );
}

// ---------- 3. jar â†’ dex pakai d8 ----------
const androidHome = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
if (!androidHome) {
    console.error("âœ– ANDROID_HOME / ANDROID_SDK_ROOT tidak di-set.");
    process.exit(1);
}

const buildToolsRoot = path.join(androidHome, "build-tools");
if (!fs.existsSync(buildToolsRoot)) {
    console.error(`âœ– Folder build-tools tidak ada: ${buildToolsRoot}`);
    process.exit(1);
}
const btVersions = fs
    .readdirSync(buildToolsRoot)
    .filter((d) => /^\d+\.\d+\.\d+$/.test(d))
    .sort(cmpSemver);
if (btVersions.length === 0) {
    console.error(`âœ– Tidak ada build-tools versi valid di ${buildToolsRoot}`);
    process.exit(1);
}
const btVersion = btVersions[btVersions.length - 1];
const d8 = path.join(buildToolsRoot, btVersion, process.platform === "win32" ? "d8.bat" : "d8");
console.log(`â–¸ d8: build-tools ${btVersion}`);

const platformsRoot = path.join(androidHome, "platforms");
if (!fs.existsSync(platformsRoot)) {
    console.error(`âœ– Folder platforms tidak ada: ${platformsRoot}`);
    process.exit(1);
}
const platformDirs = fs
    .readdirSync(platformsRoot)
    .filter((d) => /^android-\d+$/.test(d))
    .map((d) => ({ d, n: parseInt(d.replace("android-", ""), 10) }))
    .sort((a, b) => a.n - b.n);
if (platformDirs.length === 0) {
    console.error(`âœ– Tidak ada platform android-* di ${platformsRoot}`);
    process.exit(1);
}
const androidJar = path.join(platformsRoot, platformDirs[platformDirs.length - 1].d, "android.jar");
console.log(`â–¸ android.jar: ${path.relative(androidHome, androidJar)}`);

// bersihkan & siapkan folder dex
fs.rmSync(dexOut, { recursive: true, force: true });
fs.mkdirSync(dexOut, { recursive: true });

console.log(`â–¸ Running d8 ...`);
const d8Args = [
    "--lib", androidJar,
    "--min-api", "26",
    "--release",
    "--output", dexOut,
    jarPath,
];
if (process.platform === "win32") {
    execFileSync("cmd.exe", ["/c", d8, ...d8Args], { stdio: "inherit" });
} else {
    execFileSync(d8, d8Args, { stdio: "inherit" });
}

const dexPath = path.join(dexOut, "classes.dex");
if (!fs.existsSync(dexPath)) {
    console.error(`âœ– d8 tidak menghasilkan classes.dex di ${dexOut}`);
    process.exit(1);
}

const dexBytes = fs.readFileSync(dexPath);
const dexSha = crypto.createHash("sha256").update(dexBytes).digest("hex");
console.log(`â–¸ classes.dex: ${(dexBytes.length / 1024).toFixed(1)} KB`);

// ---------- 4. tulis manifest final ----------
manifest.sha256 = dexSha;
manifest.runtime = manifest.runtime || "dex";

// ---------- 5. zip â†’ .rkx ----------
const zip = new AdmZip();
zip.addFile("manifest.json", Buffer.from(JSON.stringify(manifest, null, 2), "utf8"));
zip.addFile("classes.dex", dexBytes);

if (fs.existsSync(iconPath)) {
    zip.addFile("icon.png", fs.readFileSync(iconPath));
    console.log(`â–¸ icon.png disertakan`);
}

const outPath = path.isAbsolute(outDir) ? outDir : path.join(root, outDir);
fs.mkdirSync(outPath, { recursive: true });

const outFile = path.join(outPath, `${manifest.id}-${manifest.version}.rkx`);
zip.writeZip(outFile);

// ---------- 6. bersihkan ----------
if (!keepDex) {
    fs.rmSync(dexOut, { recursive: true, force: true });
}

// ---------- summary ----------
const rkxSize = fs.statSync(outFile).size;
console.log("");
console.log("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");
console.log(`âœ” ${path.relative(root, outFile)}`);
console.log(`  id        ${manifest.id}`);
console.log(`  name      ${manifest.name}`);
console.log(`  version   ${manifest.version}`);
console.log(`  apiVersion ${manifest.apiVersion}`);
console.log(`  size      ${(rkxSize / 1024).toFixed(1)} KB`);
console.log(`  sha256    ${dexSha}`);
console.log("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");
console.log("");
console.log("Install:");
console.log(`  bash tools/install-engine.sh ${path.relative(root, outFile)}`);
console.log(`  # atau via PowerShell:`);
console.log(`  powershell -File tools/install-engine.ps1 -File ${path.relative(root, outFile)}`);
console.log(`  # atau manual:`);
console.log(`  adb push ${path.relative(root, outFile)} /sdcard/Download/`);
console.log(`  # lalu: RakaRa â†’ Engine â†’ + Install .rkx`);

// ---------- helpers ----------
function cmpSemver(a, b) {
    const pa = a.split(".").map(Number);
    const pb = b.split(".").map(Number);
    for (let i = 0; i < 3; i++) {
        if (pa[i] !== pb[i]) return pa[i] - pb[i];
    }
    return 0;
}
