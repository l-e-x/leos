@REM
@REM Copyright 2019 European Commission
@REM
@REM Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
@REM You may not use this work except in compliance with the Licence.
@REM You may obtain a copy of the Licence at:
@REM
@REM     https://joinup.ec.europa.eu/software/page/eupl
@REM
@REM Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the Licence for the specific language governing permissions and limitations under the Licence.
@REM

@echo off
setlocal enabledelayedexpansion
TITLE Annotate Server

echo "---------------------Annotate Server-----------------------------------------------"
cd ./modules/annotate

for /F %%a in ('findstr /c:proxy .\client\.npmrc') do set pr=%%a
if not defined pr echo "WARNING: PROXY SETTING NOT FOUND. If you are behind a proxy, you need to set proxy in ./client/.npmrc"

for /F %%a in ('findstr /c:bamboo_NPM_TOKEN .\client\.npmrc') do set token=%%a
if defined token echo "ERROR: you need to remove/update NPM_TOKEN in ./client/.npmrc!!!"


echo "---------------------Annotate Server-----------------------------------------------"


echo "---------------------Annotate Server COMPILING...----------------------------------"
call mvn clean install -Dmaven.test.skip=true
echo "---------------------Annotate Server COMPILED.-------------------------------------"

echo "---------------------Annotate Server STARTING...-----------------------------------"
cd ./server
call mvn spring-boot:run -Dspring-boot.run.profiles=h2 -Dspring-boot.run.folders=../config/target/generated-config
echo "---------------------Annotate Server STOPPED....-----------------------------------"

pause
