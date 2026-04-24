@echo off
REM Inspecciona via API ML el JSON de items dados sus SKUs.
REM Imprime un resumen por SKU + guarda el JSON formateado en debug/items/.
REM
REM Uso:
REM   "Test Variaciones.bat"                  → usa los SKUs hardcodeados
REM   "Test Variaciones.bat" 1234 5678 9012   → usa esos SKUs
REM
REM Editar los SKUs default en TestVariaciones.java (constante SKUS_DEFAULT).

setlocal
cd /d "%~dp0"

set "SKUS_ARG="
if not "%~1"=="" set "SKUS_ARG=-Dexec.args=%*"

call mvn -q exec:java -Dexec.mainClass=ar.com.leo.api.ml.debug.TestVariaciones %SKUS_ARG%

echo.
echo ===== Listo. JSONs en: debug\items\ =====
pause
