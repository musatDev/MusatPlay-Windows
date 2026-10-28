param(
    [Parameter(Mandatory=$true)][string]$AppImage,
    [string]$Version = '1.1.3',
    [string]$WixHome = ''
)

$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
Push-Location $project
try {
    if (!$WixHome) { $WixHome = Split-Path (Get-Command candle -ErrorAction Stop).Source }
    foreach ($tool in @('heat.exe', 'candle.exe', 'light.exe')) {
        if (!(Test-Path (Join-Path $WixHome $tool))) { throw "Missing WiX 3 tool: $tool" }
    }
    $compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
    $source = (Resolve-Path -LiteralPath $AppImage).Path
    $output = Join-Path $project 'target\installer'
    $work = Join-Path $output ('work-' + [guid]::NewGuid().ToString('N'))
    $image = Join-Path $work 'image'
    New-Item -ItemType Directory -Force "$image\app", $output | Out-Null
    if (!(Test-Path "$source\runtime\bin\server\jvm.dll")) { throw 'Source app image must include its Java runtime.' }
    Copy-Item "$source\*" $image -Recurse -Force
    Copy-Item 'LICENSE' "$image\LICENSE.txt"
    Copy-Item 'THIRD_PARTY.md' $image
    $icon = (Resolve-Path 'target\MusatPlay.ico').Path
    $productCode = [guid]::NewGuid().ToString('B').ToUpperInvariant()
    $uninstallSource = [IO.File]::ReadAllText((Join-Path $project 'installer\Uninstall.cs')).Replace('@PRODUCT_CODE@', $productCode)
    [IO.File]::WriteAllText("$work\Uninstall.cs", $uninstallSource)
    & $compiler /nologo /target:winexe /platform:x64 /optimize+ "/win32icon:$icon" "/out:$image\Uninstall.exe" "$work\Uninstall.cs"
    if ($LASTEXITCODE -ne 0) { throw 'Uninstaller compilation failed.' }
    & "$WixHome\heat.exe" dir $image -nologo -ag -sreg -srd -cg AppFiles -dr INSTALLFOLDER -var var.Image -out "$work\Files.wxs"
    if ($LASTEXITCODE -ne 0) { throw 'File harvesting failed.' }
    [xml]$files = Get-Content "$work\Files.wxs" -Raw
    $ns = New-Object Xml.XmlNamespaceManager($files.NameTable)
    $ns.AddNamespace('w', 'http://schemas.microsoft.com/wix/2006/wi')
    foreach ($component in $files.SelectNodes('//w:Component', $ns)) {
        $hash = [Security.Cryptography.MD5]::Create()
        try { $component.SetAttribute('Guid', ([guid]::new($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes('MusatPlay-nojava-' + $component.Id)))).ToString('B')) }
        finally { $hash.Dispose() }
        foreach ($file in $component.SelectNodes('w:File', $ns)) { $file.RemoveAttribute('KeyPath') }
        $key = $files.CreateElement('RegistryValue', $ns.LookupNamespace('w'))
        foreach ($entry in @{Root='HKCU'; Key='Software\musatDev\MusatPlay\Files'; Name=$component.Id; Type='integer'; Value='1'; KeyPath='yes'}.GetEnumerator()) { $key.SetAttribute($entry.Key, $entry.Value) }
        $component.AppendChild($key) | Out-Null
        $remove = $files.CreateElement('RemoveFolder', $ns.LookupNamespace('w'))
        $remove.SetAttribute('Id', 'remove_' + $component.Id)
        $remove.SetAttribute('On', 'uninstall')
        $component.AppendChild($remove) | Out-Null
    }
    $cleanup = $files.SelectSingleNode('//w:Component', $ns)
    foreach ($directory in $files.SelectNodes('//w:Directory[not(w:Component)]', $ns)) {
        $remove = $files.CreateElement('RemoveFolder', $ns.LookupNamespace('w'))
        $remove.SetAttribute('Id', 'remove_' + $directory.Id)
        $remove.SetAttribute('Directory', $directory.Id)
        $remove.SetAttribute('On', 'uninstall')
        $cleanup.AppendChild($remove) | Out-Null
    }
    $files.Save("$work\Files.wxs")
    & "$WixHome\candle.exe" -nologo -arch x64 "-dImage=$image" "-dIcon=$icon" "-dProductCode=$productCode" "-dVersion=$Version" -out "$work\" installer\Setup.wxs "$work\Files.wxs"
    if ($LASTEXITCODE -ne 0) { throw 'MSI compilation failed.' }
    $msi = Join-Path $output "MusatPlay-$Version.msi"
    & "$WixHome\light.exe" -nologo -ext WixUIExtension -cultures:en-us -loc installer\en-US.wxl -out $msi "$work\Setup.wixobj" "$work\Files.wixobj"
    if ($LASTEXITCODE -ne 0) { throw 'MSI linking failed.' }
    $setup = Join-Path $output "MusatPlay-Setup-$Version.exe"
    $setupSource = [IO.File]::ReadAllText((Join-Path $project 'installer\SetupWebView2.cs')).Replace('@VERSION@', $Version)
    [IO.File]::WriteAllText("$work\SetupWebView2.cs", $setupSource)
    & $compiler /nologo /target:winexe /platform:x64 /optimize+ "/win32icon:$icon" /reference:System.Windows.Forms.dll /reference:System.Drawing.dll "/resource:$msi,MusatPlay.msi" "/out:$setup" "$work\SetupWebView2.cs"
    if ($LASTEXITCODE -ne 0) { throw 'Setup compilation failed.' }
    Write-Host "Installer ready: $setup"
} finally { Pop-Location }
