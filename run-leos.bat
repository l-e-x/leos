@echo off

TITLE Leos-Prototype
echo "---------------------LEOS-----------------------------------------------"

echo "---------------------LEOS COMPILING...----------------------------------"
call mvn clean install
echo "---------------------LEOS COMPILED.-------------------------------------"

cd ./modules/web

echo "---------------------LEOS STARTING...-----------------------------------"
call mvn jetty:run-war
echo "---------------------LEOS STOPPED.--------------------------------------"
