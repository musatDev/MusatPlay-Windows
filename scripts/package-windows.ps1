param([ValidateSet('x64')][string]$Architecture = 'x64')
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'package-webview2.ps1') -PortableOnly