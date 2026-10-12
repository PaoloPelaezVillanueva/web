@echo off
REM ====================================================================
REM  CRM de Cobranzas conectado a MYSQL (datos reales y permanentes)
REM  Requisitos:
REM   1. Haber ejecutado database\01_esquema.sql en MySQL Workbench
REM   2. Haber creado backend\.env a partir de backend\.env.example
REM ====================================================================
title CRM Cobranzas - MySQL

if not exist "%~dp0backend\.env" (
  echo.
  echo  [!] Falta el archivo backend\.env
  echo      Copia backend\.env.example como backend\.env y escribe tu contrasena de MySQL.
  echo      Instrucciones en README.md, seccion "Conectar con MySQL".
  echo.
  pause
  exit /b 1
)

start "Backend - Spring Boot (MySQL)" cmd /k "cd /d "%~dp0backend" && mvnw.cmd spring-boot:run"

echo Backend iniciado en http://localhost:8080/api
echo Espera a que la ventana del Backend muestre "Started" y luego prueba POST /api/auth/login.
echo.
pause
