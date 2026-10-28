@echo off
setlocal
cd /d "%~dp0"
java -jar target\webview2-build\MusatPlay.jar
if errorlevel 1 pause
