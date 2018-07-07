@echo off
TITLE Repository
echo "---------------------REPOSITORY-----------------------------------------------"

cd ./tools/cmis/chemistry-opencmis-server-inmemory

echo "---------------------REPOSITORY COMPILING...----------------------------------"
call mvn clean install
echo "---------------------REPOSITORY COMPILED.-------------------------------------"

echo "---------------------REPOSITORY STARTING...-----------------------------------"
call mvn jetty:run-war
echo "---------------------REPOSITORY STOPPED....-----------------------------------"
