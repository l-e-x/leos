@echo off

CALL "cmd /c start run-database.bat"
CALL "cmd /c start run-repository.bat"
CALL "cmd /c start run-leos.bat"

echo "--------------------------------------------------------------------------"
echo "---ALL 3 COMMAND WINDOWS NEED TO STAY OPEN FOR LEOS TO FUNCTION-----------"
echo "---IT MIGHT TAKE FEW MINUTES TO DOWNLOAD DEPENDENCIES AND TO BE READY-----"
echo "---USE CTRL+C TO STOP INDIVIDUAL COMPONENT--------------------------------"
echo "--------------------------------------------------------------------------"
