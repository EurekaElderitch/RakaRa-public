param(
    [Parameter(Mandatory = $true)]
    [string]$PublicRepo,

    [string]$Message = "Sync from local $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",

    # Pattern tag yang mau di-push. Default: engines-v* (untuk trigger release workflow di public)
    # Set ke "*" untuk semua tag, atau "" (kosong) untuk tidak push tag sama sekali
    [string]$TagPattern = "engines-v*",

    # Dry run — tampilkan apa yang akan dilakukan, tanpa benar-benar push
    [switch]$DryRun,

    # Force recreate workdir meski sudah ada (default: true — selalu fresh)
    [switch]$NoCleanWorkdir
)

$ErrorActionPreference = "Stop"

# --- Helper output ---
function Write-Step { param($m) Write-Host ""; Write-Host ">> $m" -ForegroundColor Cyan }
function Write-Info { param($m) Write-Host "   $m" -ForegroundColor Gray }
function Write-Ok   { param($m) Write-Host "   [OK] $m" -ForegroundColor Green }
function Write-Warn { param($m) Write-Host "   [WARN] $m" -ForegroundColor Yellow }
function Write-Err  { param($m) Write-Host "   [ERR] $m" -ForegroundColor Red }
function Write-Dry  { param($m) Write-Host "   [DRY] $m" -ForegroundColor Magenta }

# --- Paths ---
$root = Split-Path -Parent $PSScriptRoot
$publicDir = Join-Path (Split-Path -Parent $root) "RakaRa-public"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   RAKARA - Sync to Public Repo" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Info "Private : $root"
Write-Info "Public  : $publicDir"
Write-Info "Remote  : $PublicRepo"
Write-Info "Message : $Message"
Write-Info "Tags    : $TagPattern"
Write-Info "Dry run : $DryRun"
Write-Host ""

# --- Sanity check ---
if (-not (Test-Path (Join-Path $root ".git"))) {
    Write-Err "Bukan git repo: $root"
    exit 1
}

if ($PublicRepo -notmatch "^[^/]+/[^/]+$") {
    Write-Err "Format -PublicRepo harus 'owner/repo'. Contoh: myuser/RakaRa-public"
    exit 1
}

try {

    # --- Clean old workdir ---
    if ((Test-Path $publicDir) -and (-not $NoCleanWorkdir)) {
        Write-Step "Cleaning old workdir..."
        if (-not $DryRun) { Remove-Item -Recurse -Force $publicDir }
        Write-Ok "Cleaned: $publicDir"
    }

    # --- Copy files via robocopy ---
    Write-Step "Copying files via robocopy..."
    if (-not $DryRun) {
        New-Item -ItemType Directory -Force -Path $publicDir | Out-Null

        $robocopyArgs = @(
            $root,
            $publicDir,
            "/E",
            "/NFL", "/NDL", "/NJH", "/NJS",
            "/XD", "build", ".gradle", ".idea", "node_modules", ".git", "dist", ".vs", ".kotlin",
            "/XF", "secrets.json", "local.properties", "*.iml", "*.apk", "*.aab", "*.rkx", "*.log", "*.keystore", "*.jks", "*.p12", "*.bak"
        )
        & robocopy @robocopyArgs | Out-Null
        # robocopy returns non-zero even on success (0-7 = success codes)
        if ($LASTEXITCODE -gt 7) { throw "robocopy failed with code $LASTEXITCODE" }
    } else {
        Write-Dry "robocopy $root -> $publicDir"
    }
    Write-Ok "Copied."

    # --- Safety: remove leaked keystores ---
    if (-not $DryRun) {
        Get-ChildItem -Path $publicDir -Include "*.keystore","*.jks","*.p12" -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Warn "Removing leaked keystore: $($_.Name)"
            Remove-Item $_.FullName -Force
        }
    }

    # --- Strip engine build dirs ---
    if (-not $DryRun) {
        Get-ChildItem -Path (Join-Path $publicDir "engines") -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            $buildDir = Join-Path $_.FullName "build"
            if (Test-Path $buildDir) { Remove-Item -Recurse -Force $buildDir }
        }
    }
    Write-Ok "Engine build dirs cleaned."

    # --- Strip comments from .kt ---
    Write-Step "Stripping comments from .kt files..."
    if (-not $DryRun) {
        Push-Location $publicDir
        try {
            node "tools/strip-comments.mjs"
            if ($LASTEXITCODE -ne 0) { throw "strip-comments.mjs failed (exit $LASTEXITCODE)" }
        } finally {
            Pop-Location
        }
    } else {
        Write-Dry "node tools/strip-comments.mjs (in $publicDir)"
    }
    Write-Ok "Comments stripped."

    # --- Verify: no site references ---
    Write-Step "Verifying no site references..."
    if (-not $DryRun) {
        Push-Location $publicDir
        try {
            $files = Get-ChildItem -Recurse -Include "*.kt","*.xml","*.kts","*.json","*.md" -File
            $leaks = $files | Select-String -Pattern "keikomik" -CaseSensitive:$false -ErrorAction SilentlyContinue
            if ($leaks) {
                Write-Err "Found 'keikomik' references in public tree:"
                $leaks | ForEach-Object { Write-Host "     $($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
                throw "Cleanup required — fix source before sync"
            }
        } finally {
            Pop-Location
        }
    } else {
        Write-Dry "grep -r keikomik (in $publicDir)"
    }
    Write-Ok "No 'keikomik' references."

    # --- Verify: no keystore leaked ---
    Write-Step "Verifying no keystore leaked..."
    if (-not $DryRun) {
        $leakedKeys = Get-ChildItem -Path $publicDir -Include "*.keystore","*.jks","*.p12" -Recurse -File -ErrorAction SilentlyContinue
        if ($leakedKeys) {
            Write-Err "KeyStore leaked to public repo:"
            $leakedKeys | ForEach-Object { Write-Host "     $($_.FullName)" }
            throw "Keystore leak detected"
        }
    }
    Write-Ok "No keystore leaked."

    # --- Init git in workdir ---
    Write-Step "Initializing git in public workdir..."
    if (-not $DryRun) {
        Push-Location $publicDir
        try {
            & git init -b main | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "git init failed" }

            & git config user.name "RakaRa Release" | Out-Null
            & git config user.email "release@RakaRa.local" | Out-Null

            & git add -A
            if ($LASTEXITCODE -ne 0) { throw "git add failed" }

            & git update-index --chmod=+x gradlew 2>$null

            & git commit -m $Message | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "git commit failed" }
        } finally {
            Pop-Location
        }
    } else {
        Write-Dry "git init -b main (in $publicDir)"
        Write-Dry "git add -A && git commit -m '$Message'"
    }
    Write-Ok "Local commit created."

    # --- Sync tags from local repo ---
    Write-Step "Syncing tags from local repo (pattern: $TagPattern)..."
    $localTags = @()
    if ($TagPattern) {
        Push-Location $root
        try {
            $allTags = & git tag 2>$null
            $localTags = $allTags | Where-Object { $_ -like $TagPattern }
        } finally {
            Pop-Location
        }
    }

    if ($localTags.Count -eq 0) {
        Write-Warn "Tidak ada tag yang match pattern '$TagPattern' — skip tag push."
    } else {
        Write-Info "Found $($localTags.Count) tag(s): $($localTags -join ', ')"

        if (-not $DryRun) {
            Push-Location $publicDir
            try {
                foreach ($tag in $localTags) {
                    # Tag commit HEAD di workdir (fresh init → semua tag point ke commit ini)
                    & git tag $tag 2>$null
                    if ($LASTEXITCODE -eq 0) {
                        Write-Info "  + Tagged: $tag"
                    } else {
                        Write-Warn "  ! Failed to tag: $tag (mungkin sudah ada)"
                    }
                }
            } finally {
                Pop-Location
            }
        } else {
            $localTags | ForEach-Object { Write-Dry "git tag $_ (in $publicDir)" }
        }
        Write-Ok "Tags tagged locally."
    }

    # --- Push main ---
    Write-Step "Pushing main to public (force)..."
    if (-not $DryRun) {
        Push-Location $publicDir
        try {
            & git remote add origin "https://github.com/$PublicRepo.git" 2>$null
            if ($LASTEXITCODE -ne 0) {
                # Remote might already exist — set URL
                & git remote set-url origin "https://github.com/$PublicRepo.git"
            }

            Write-Info "→ git push -f origin main"
            & git push -f origin main
            if ($LASTEXITCODE -ne 0) { throw "git push main failed" }
        } finally {
            Pop-Location
        }
    } else {
        Write-Dry "git push -f origin main"
    }
    Write-Ok "Main pushed."

    # --- Push tags ---
    if ($localTags.Count -gt 0) {
        Write-Step "Pushing tags to public (force)..."
        if (-not $DryRun) {
            Push-Location $publicDir
            try {
                foreach ($tag in $localTags) {
                    Write-Info "→ git push -f origin $tag"
                    & git push -f origin $tag
                    if ($LASTEXITCODE -ne 0) {
                        Write-Warn "  Failed to push tag: $tag"
                    } else {
                        Write-Info "  ✓ $tag pushed"
                    }
                }
            } finally {
                Pop-Location
            }
        } else {
            $localTags | ForEach-Object { Write-Dry "git push -f origin $_" }
        }
        Write-Ok "Tags pushed. Workflow release-engines.yml akan auto-trigger."
    }

    # --- Summary ---
    Write-Host ""
    Write-Host "==============================================" -ForegroundColor Green
    Write-Host "   [OK] Sync selesai." -ForegroundColor Green
    Write-Host "==============================================" -ForegroundColor Green
    Write-Host ""
    Write-Info "Repo    : https://github.com/$PublicRepo"
    Write-Info "Actions : https://github.com/$PublicRepo/actions"
    if ($localTags.Count -gt 0) {
        Write-Info "Release : https://github.com/$PublicRepo/releases"
        Write-Info ""
        Write-Info "NOTE: Setelah workflow 'Release Engines' hijau,"
        Write-Info "      JANGAN LUPA mark release sebagai 'Latest'!"
    }
    Write-Host ""

} catch {
    Write-Host ""
    Write-Err "Error: $_"
    if (Test-Path $publicDir) {
        Write-Info "Workdir masih ada: $publicDir"
    }
    exit 1
}