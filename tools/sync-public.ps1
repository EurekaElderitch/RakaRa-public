param(
    [Parameter(Mandatory = $true)]
    [string]$PublicRepo,
    [string]$Message = "Release v0.1.0"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$publicDir = Join-Path (Split-Path -Parent $root) "RakaRa-public"

Write-Host ">> Private : $root"
Write-Host ">> Public  : $publicDir"
Write-Host ">> Remote  : $PublicRepo"
Write-Host ""

try {

    if (Test-Path $publicDir) {
        Write-Host ">> Cleaning old workdir..."
        Remove-Item -Recurse -Force $publicDir
    }

    Write-Host ">> Copying files..."
    New-Item -ItemType Directory -Force -Path $publicDir | Out-Null

    $robocopyArgs = @(
        $root,
        $publicDir,
        "/E",
        "/NFL", "/NDL", "/NJH", "/NJS",
        "/XD", "build", ".gradle", ".idea", "node_modules", ".git", "dist",
        "/XF", "secrets.json", "local.properties", "*.iml", "*.apk", "*.aab", "*.rkx", "*.log", "*.keystore", "*.jks", "*.p12"
    )
    & robocopy @robocopyArgs | Out-Null

    # Safety net: pastikan keystore tidak bocor (jaga-jaga kalau robocopy skip gagal)
    Get-ChildItem -Path $publicDir -Include "*.keystore","*.jks","*.p12" -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "  [WARN] Removing leaked keystore: $($_.FullName)"
        Remove-Item $_.FullName -Force
    }

    Get-ChildItem -Path (Join-Path $publicDir "engines") -Directory -ErrorAction SilentlyContinue | ForEach-Object {
        $buildDir = Join-Path $_.FullName "build"
        if (Test-Path $buildDir) { Remove-Item -Recurse -Force $buildDir }
    }

    Write-Host ">> Stripping comments from .kt files..."
    Push-Location $publicDir
    node "tools/strip-comments.mjs"
    Pop-Location

    Write-Host ">> Verifying no site references..."
    Push-Location $publicDir
    $leaks = Select-String -Path (Get-ChildItem -Recurse -Include "*.kt","*.xml","*.kts","*.json","*.md" -File) -Pattern "keikomik" -CaseSensitive:$false -ErrorAction SilentlyContinue
    if ($leaks) {
        Pop-Location
        Write-Host ""
        Write-Host "[X] Found 'keikomik' references in public tree:"
        $leaks | ForEach-Object { Write-Host "  $($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
        Write-Host ""
        exit 1
    }
    Write-Host "  [OK] Clean."
    Pop-Location

    # Verify no keystore in public
    $leakedKeys = Get-ChildItem -Path $publicDir -Include "*.keystore","*.jks","*.p12" -Recurse -File -ErrorAction SilentlyContinue
    if ($leakedKeys) {
        Write-Host ""
        Write-Host "[X] KeyStore leaked to public repo (should not happen):"
        $leakedKeys | ForEach-Object { Write-Host "  $($_.FullName)" }
        exit 1
    }
    Write-Host "  [OK] No keystore leaked."

    Write-Host ">> Init git in public dir..."
    Push-Location $publicDir

    & git init -b main
    if ($LASTEXITCODE -ne 0) { throw "git init failed" }

    & git config user.name "RakaRa Release"
    & git config user.email "release@RakaRa.local"

    & git add -A
    if ($LASTEXITCODE -ne 0) { throw "git add failed" }

    & git update-index --chmod=+x gradlew 2>$null

    & git commit -m $Message
    if ($LASTEXITCODE -ne 0) { throw "git commit failed" }

    & git remote add origin $PublicRepo
    if ($LASTEXITCODE -ne 0) { throw "git remote add failed" }

    Write-Host ">> Force pushing to $PublicRepo ..."
    & git push -f origin main
    if ($LASTEXITCODE -ne 0) { throw "git push failed" }

    Pop-Location

    Write-Host ""
    Write-Host "[OK] Done."

} catch {
    Write-Host ""
    Write-Host "[X] Error: $_" -ForegroundColor Red
    if (Test-Path $publicDir) {
        Write-Host "    Public workdir exists at: $publicDir"
    }
    exit 1
}