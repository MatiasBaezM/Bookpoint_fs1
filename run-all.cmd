@echo off
title BookPoint — Orquestador de Microservicios
color 0b

echo =======================================================================
echo          BookPoint — Iniciando Orquestador de Microservicios
echo =======================================================================
echo.

:: 1. Verificar si existe Maven Wrapper
if exist "mvnw.cmd" (
    set MAVEN_CMD=mvnw.cmd
) else (
    echo Maven Wrapper (mvnw.cmd) no encontrado en la raiz. Usando 'mvn' global...
    set MAVEN_CMD=mvn
)

:: 2. Compilar todos los microservicios en el proyecto padre
echo [+] Compilando todos los modulos (esto puede tomar unos momentos)...
call %MAVEN_CMD% clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo.
    color 0c
    echo [ERROR] La compilacion de Maven ha fallado. Abortando inicio.
    pause
    exit /b %ERRORLEVEL%
)
echo [+] Compilacion completada con exito.
echo.

:: 3. Definir opciones de JVM optimizadas para desarrollo (Consumo minimo de RAM)
:: -XX:TieredStopAtLevel=1: Limita la compilacion JIT para acelerar el arranque
:: -Xms64m -Xmx128m: Asigna minimo 64MB y maximo 128MB de Heap por microservicio
set JVM_OPTS=-XX:TieredStopAtLevel=1 -Xms64m -Xmx128m
echo [+] Iniciando microservicios con optimizaciones de RAM (%JVM_OPTS%)...
echo.

:: 4. Arrancar los microservicios en ventanas independientes de CMD
echo [*] Levantando ms-gateway (Puerto 8080)...
start "ms-gateway [8080]" java %JVM_OPTS% -jar ms-gateway\target\ms-gateway-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-ventas (Puerto 8081)...
start "ms-ventas [8081]" java %JVM_OPTS% -jar ms-ventas\target\ventas-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-inventario (Puerto 8082)...
start "ms-inventario [8082]" java %JVM_OPTS% -jar ms-inventario\target\inventario-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-usuarios (Puerto 8083)...
start "ms-usuarios [8083]" java %JVM_OPTS% -jar ms-usuarios\target\usuarios-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-catalogo (Puerto 8084)...
start "ms-catalogo [8084]" java %JVM_OPTS% -jar ms-catalogo\target\catalogo-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-logistica (Puerto 8085)...
start "ms-logistica [8085]" java %JVM_OPTS% -jar ms-logistica\target\logistica-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-proveedores (Puerto 8086)...
start "ms-proveedores [8086]" java %JVM_OPTS% -jar ms-proveedores\target\proveedores-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-promociones (Puerto 8087)...
start "ms-promociones [8087]" java %JVM_OPTS% -jar ms-promociones\target\promociones-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-facturacion (Puerto 8088)...
start "ms-facturacion [8088]" java %JVM_OPTS% -jar ms-facturacion\target\facturacion-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-bodega (Puerto 8089)...
start "ms-bodega [8089]" java %JVM_OPTS% -jar ms-bodega\target\bodega-0.0.1-SNAPSHOT.jar

echo [*] Levantando ms-sucursales (Puerto 8090)...
start "ms-sucursales [8090]" java %JVM_OPTS% -jar ms-sucursales\target\sucursales-0.0.1-SNAPSHOT.jar

echo.
echo =======================================================================
echo  [SUCCESS] Todos los microservicios estan arrancando en segundo plano.
echo  Puedes ver sus logs en cada una de las ventanas CMD abiertas.
echo  Para detener todos los servicios a la vez, ejecuta: stop-all.cmd
echo =======================================================================
echo.
pause
