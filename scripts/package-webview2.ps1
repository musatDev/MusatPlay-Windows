param([string]$WixHome = '', [string]$Version = '1.1.3', [switch]$PortableOnly)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
Push-Location $project
try {
    $java = & java -XshowSettings:properties -version 2>&1 | Out-String
    if ($java -notmatch 'java.specification.version = 21' -or $java -notmatch 'os.arch = amd64') { throw 'Use x64 JDK 21.' }
    & .\mvnw.cmd -B verify
    if ($LASTEXITCODE -ne 0) { throw 'Maven failed.' }
    & node --test tests/player-bridge.test.cjs
    if ($LASTEXITCODE -ne 0) { throw 'Player bridge tests failed.' }
    $build = Join-Path $project 'target\webview2-build'
    $inputDir = Join-Path $build 'package-input'
    New-Item -ItemType Directory -Force $inputDir | Out-Null
    Copy-Item "$build\MusatPlay.jar" $inputDir -Force
    Copy-Item "$build\lib" $inputDir -Recurse -Force
    & java -jar "$build\MusatPlay.jar" --write-icon target\MusatPlay.ico
    if ($LASTEXITCODE -ne 0) { throw 'Icon generation failed.' }
    $runtime = Join-Path $build 'java-runtime-en-US-tls'
    if (!(Test-Path $runtime)) {
        & jlink --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,jdk.unsupported,jdk.crypto.ec `
            --strip-debug --no-header-files --no-man-pages --strip-native-commands --compress=2 --output $runtime
        if ($LASTEXITCODE -ne 0) { throw 'Java runtime creation failed.' }
    }
    $destination = Join-Path $build ('image-' + [guid]::NewGuid().ToString('N'))
    & jpackage --type app-image --name MusatPlay --app-version $Version --vendor musatDev --input $inputDir `
        --main-jar MusatPlay.jar --main-class dev.musat.musatplay.Launcher --icon target\MusatPlay.ico --runtime-image $runtime `
        --dest $destination --java-options '-Dfile.encoding=UTF-8' --java-options '-Xms16m' --java-options '-Xmx256m'
    if ($LASTEXITCODE -ne 0) { throw 'App image creation failed.' }
    $app = Join-Path $destination 'MusatPlay'
    $updateCheck = Join-Path $destination 'update-check.txt'
    $check = Start-Process -FilePath "$app\MusatPlay.exe" -ArgumentList @('--check-updates', ('"' + $updateCheck + '"')) -WindowStyle Hidden -PassThru
    if (!$check.WaitForExit(45000)) { $check.Kill(); throw 'Packaged update check timed out.' }
    if ($check.ExitCode -ne 0 -or !(Test-Path -LiteralPath $updateCheck)) { throw 'Packaged HTTPS update check failed.' }
    Get-Content -LiteralPath $updateCheck | Write-Host
    Copy-Item LICENSE "$app\LICENSE.txt"
    Copy-Item THIRD_PARTY.md $app
    if ($PortableOnly) {
        $zip = Join-Path $destination 'MusatPlay-windows-x64.zip'
        Compress-Archive -Path $app -DestinationPath $zip -CompressionLevel Optimal
        Write-Host "Portable package: $zip"
        return
    }
    & .\scripts\package-installer.ps1 -AppImage $app -Version $Version -WixHome $WixHome
    New-Item -ItemType Directory -Force release | Out-Null
    Copy-Item "target\installer\MusatPlay-Setup-$Version.exe" "release\MusatPlay-Setup-$Version.exe" -Force
    $setup = Join-Path $project "release\MusatPlay-Setup-$Version.exe"
    $hash = (Get-FileHash -LiteralPath $setup -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText($setup + '.sha256', $hash + '  ' + [IO.Path]::GetFileName($setup) + "`n")
    Write-Host "Clean release: $project\release\MusatPlay-Setup-$Version.exe"
} finally { Pop-Location }
