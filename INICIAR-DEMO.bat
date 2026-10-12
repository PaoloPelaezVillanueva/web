@echo off
REM ====================================================================
REM  CRM de Cobranzas - MODO DEMO (base de datos en memoria, sin MySQL)
REM  Doble clic en este archivo. Se abre una ventana: no la cierres.
REM ====================================================================
title CRM Cobranzas - Demo

echo Iniciando backend (Spring Boot, perfil demo)...
start "Backend - Spring Boot" cmd /k "cd /d "%~dp0backend" && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo"

echo.
echo  API:          http://localhost:8080/api
echo  Consola BD:   http://localhost:8080/h2-console  (JDBC: jdbc:h2:mem:crm, usuario sa, sin clave)
echo.
echo  Usuarios de prueba (para probar el login en Postman):
echo    Administrador : admin    / Admin12345
echo    Supervisor    : sgarcia  / Clave12345
echo    Asesor        : asesor1  / Clave12345
echo.
echo  Espera a que la ventana de Backend muestre "Started", luego prueba POST /api/auth/login.
echo.
pause
