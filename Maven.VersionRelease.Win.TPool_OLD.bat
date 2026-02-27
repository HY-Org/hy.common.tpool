@echo off

:: 解决中文乱码
chcp 65001 >nul 2>&1
:: 执行PowerShell脚本，-ExecutionPolicy Bypass 临时跳过执行策略限制
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass ^
  -Command "$OutputEncoding = [System.Text.UTF8Encoding]::new($false); & '%~dp0Maven.VersionRelease.Win.TPool_OLD.ps1'"
pause