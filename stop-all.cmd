@echo off
title BookPoint — Detener Microservicios
color 0e

echo =======================================================================
echo            BookPoint — Deteniendo todos los Microservicios
echo =======================================================================
echo.

echo [+] Buscando procesos Java correspondientes a los microservicios BookPoint...
:: Ejecuta un comando PowerShell seguro que busca los procesos java.exe que esten corriendo algun JAR de version 0.0.1-SNAPSHOT.jar
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Get-CimInstance Win32_Process -Filter 'Name=\"java.exe\"' | " ^
    "Where-Object { $_.CommandLine -like '*0.0.1-SNAPSHOT.jar*' } | " ^
    "ForEach-Object { " ^
    "   Write-Host '[DETENIENDO] PID' $_.ProcessId '->' $_.CommandLine.Split(' ')[-1]; " ^
    "   Stop-Process -Id $_.ProcessId -Force " ^
    "}"

echo.
echo =======================================================================
echo  [SUCCESS] Todos los microservicios BookPoint han sido detenidos.
echo  Los puertos 8080-8090 deberian estar libres ahora.
echo =======================================================================
echo.
pause
