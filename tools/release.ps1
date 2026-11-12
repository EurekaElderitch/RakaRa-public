<#
.SYNOPSIS
One-command release untuk RakaRa (interaktif).
Cukup jalankan: .\tools\release.ps1
Script akan menanyakan: mau rilis APK, Engine, atau keduanya.

.EXAMPLE
.\tools\release.ps1
# Ikuti prompt interaktif

.EXAMPLE
.\tools\release.ps1 -Type app -Message "feat: home dropdown"
# Mode langsung (skip prompt)
#>

param(
    [string]$Version,
    [string]$Message,
    [ValidateSet("app", "engines", "both", "")]
    [string]$Type = "",
    [switch]$SkipSync
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$publicDir = Join-Path (Split-Path -Parent $root) "RakaRa-public"
$gradleFile = Join-Path $root "app\build.gradle.kts"

# === INTERACTIVE PROMPT kalau Type tidak diberikan ===
if ([string]::IsNullOrWhiteSpace($Type)) {
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "     RAKARA RELEASE - Pilih Tipe" -ForegroundColor Cyan
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  1. APK           (UI, fitur, bug fix)" -ForegroundColor White
    Write-Host "  2. Engine        (scraper Source01.kt)" -ForegroundColor White
    Write-Host "  3. Both          (APK + Engine)" -ForegroundColor White
    Write-Host "  0. Batal" -ForegroundColor DarkGray
    Write-Host ""
    do {
        $choice = Read-Host "Pilih [1/2/3/0]"
        switch ($choice) {
            "1" { $Type = "app" }
            "2" { $Type = "engines" }
            "3" { $Type = "both" }
            "0" { Write-Host "Batal." -ForegroundColor Yellow; exit 0 }
            default { Write-Host "Input tidak valid. Coba lagi." -ForegroundColor Red }
        }
    } while ([string]::IsNullOrWhiteSpace($Type))
}

# === INTERACTIVE PROMPT kalau Message tidak diberikan ===
if ([string]::IsNullOrWhiteSpace($Message)) {
    Write-Host ""
    $Message = Read-Host "Tulis pesan commit (contoh: fix: home dropdown)"
    if ([string]::IsNullOrWhiteSpace($Message)) {
        $Message = "release: $Type"
    }
}

# === Baca versi saat ini ===
$content = Get-Content $gradleFile -Raw
$currentVersionName = [regex]::Match($content, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
$currentVersionCode = [int][regex]::Match($content, 'versionCode\s*=\s*(\d+)').Groups[1].Value

Write-Host ""
Write-Host "=== RAKARA RELEASE SCRIPT ===" -ForegroundColor Cyan
Write-Host "Tipe    : $Type" -ForegroundColor Gray
Write-Host "Current : v$currentVersionName (code $currentVersionCode)" -ForegroundColor Gray
Write-Host "Message : $Message" -ForegroundColor Gray
Write-Host ""

# === Hitung versi baru ===
if ([string]::IsNullOrWhiteSpace($Version)) {
    if ($Type -eq "engines") {
        $newVersionName = $currentVersionName
        $newVersionCode = $currentVersionCode
    } else {
        # auto-bump patch (0.3.2 -> 0.3.3)
        $parts = $currentVersionName.Split('.')
        if ($parts.Length -eq 3) {
            $parts[2] = [string]([int]$parts[2] + 1)
            $newVersionName = $parts -join "."
        } else {
            throw "Format versionName tidak valid: $currentVersionName"
        }
        $newVersionCode = $currentVersionCode + 1
    }
} else {
    $newVersionName = $Version
    $newVersionCode = $currentVersionCode + 1
}

if ($Type -ne "engines") {
    Write-Host "New     : v$newVersionName (code $newVersionCode)" -ForegroundColor Green
} else {
    Write-Host "Engine  : versi APK tidak berubah" -ForegroundColor Green
}

# === Update build.gradle.kts kalau bukan rilis engine-only ===
if ($Type -ne "engines") {
    $newContent = $content `
        -replace "versionName\s*=\s*`"[^`"]+`"", "versionName = `"$newVersionName`"" `
        -replace "versionCode\s*=\s*\d+", "versionCode = $newVersionCode"

    [System.IO.File]::WriteAllText($gradleFile, $newContent, [System.Text.UTF8Encoding]::new($false))
    Write-Host "  [OK] build.gradle.kts updated" -ForegroundColor Green
}

# === Git add, commit, push ke private ===
Write-Host ""
Write-Host ">> Pushing to PRIVATE repo..." -ForegroundColor Yellow
Push-Location $root
& git add -A
& git commit -m "$Message"
if ($LASTEXITCODE -ne 0) {
    Write-Host "  (tidak ada perubahan untuk di-commit, lanjut)" -ForegroundColor Gray
}
& git push origin main

# === Tag & push (Support both app & engines) ===
$tagsToPush = @()

if ($Type -eq "app" -or $Type -eq "both") {
    $appTag = "app-v$newVersionName"
    $tagsToPush += $appTag
}

if ($Type -eq "engines" -or $Type -eq "both") {
    $enginesTag = "engines-v" + (Get-Date -Format "yyyyMMddHHmm")
    $tagsToPush += $enginesTag
}

Write-Host ""
foreach ($tag in $tagsToPush) {
    Write-Host ">> Creating tag: $tag" -ForegroundColor Yellow
    & git tag $tag
    & git push origin $tag
}

Pop-Location
Write-Host "  [OK] Pushed to private with tags: $($tagsToPush -join ', ')" -ForegroundColor Green

if ($SkipSync) {
    Write-Host ""
    Write-Host ">> SkipSync enabled. Sync ke public DILEWATI." -ForegroundColor Yellow
    Write-Host "   Jalankan manual nanti: .\tools\sync-public.ps1 -PublicRepo ..." -ForegroundColor Yellow
    exit 0
}

# === Tunggu user konfirmasi build private hijau ===
Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  STEP MANUAL: Tunggu build PRIVATE hijau" -ForegroundColor Cyan
Write-Host "  Buka: https://github.com/EurekaElderitch/RakaRa/actions" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""
$confirm = Read-Host "Sudah hijau? Lanjut sync ke public? (y/n)"
if ($confirm -ne "y") {
    Write-Host "Berhenti. Tag sudah dibuat di private, tapi belum sync ke public." -ForegroundColor Yellow
    Write-Host "Jalankan manual: .\tools\sync-public.ps1 -PublicRepo ..." -ForegroundColor Yellow
    exit 0
}

# === Sync ke public ===
Write-Host ""
Write-Host ">> Syncing to PUBLIC repo..." -ForegroundColor Yellow
if (Test-Path $publicDir) {
    Remove-Item -Recurse -Force $publicDir
}
& powershell -ExecutionPolicy Bypass -File (Join-Path $root "tools\sync-public.ps1") `
    -PublicRepo "https://github.com/EurekaElderitch/RakaRa-public.git" `
    -Message "Release: $($tagsToPush -join ', ')"

if ($LASTEXITCODE -ne 0) { throw "sync-public.ps1 gagal" }

# === Push tag ke public ===
Write-Host ""
Write-Host ">> Pushing tags to PUBLIC repo..." -ForegroundColor Yellow
Push-Location $publicDir
& git push origin main
foreach ($tag in $tagsToPush) {
    & git tag $tag
    & git push origin $tag
}
Pop-Location

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  RELEASE SELESAI!" -ForegroundColor Green
Write-Host "  Tags: $($tagsToPush -join ', ')" -ForegroundColor Green
Write-Host "  Version: v$newVersionName (code $newVersionCode)" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next: tunggu build PUBLIC hijau, lalu test auto-update di HP." -ForegroundColor Cyan