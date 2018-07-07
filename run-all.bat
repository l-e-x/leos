@REM
@REM Copyright 2016 European Commission
@REM
@REM Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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

CALL "cmd /c start run-database.bat"
CALL "cmd /c start run-repository.bat"
CALL "cmd /c start run-leos.bat"

echo "--------------------------------------------------------------------------"
echo "---ALL 3 COMMAND WINDOWS NEED TO STAY OPEN FOR LEOS TO FUNCTION-----------"
echo "---IT MIGHT TAKE FEW MINUTES TO DOWNLOAD DEPENDENCIES AND TO BE READY-----"
echo "---USE CTRL+C TO STOP INDIVIDUAL COMPONENT--------------------------------"
echo "--------------------------------------------------------------------------"
