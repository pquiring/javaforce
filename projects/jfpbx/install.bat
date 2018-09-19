@echo off

echo jPBXlite manual install.
echo This will place files into required folders.
echo Continue?
pause

set DataFolder=%ProgramData%
if not "%DataFolder%" == "" goto win60
set DataFolder=%AllUsersProfile%
:win60

set AppFolder=%ProgramFiles(x86)%
if not "%AppFolder%" == "" goto x64
set AppFolder=%ProgramFiles%
:x64

echo AppFolder = %AppFolder%
echo DataFolder = %DataFolder%

md "%AppFolder%\jpbxlite"
md "%DataFolder%\jpbxlite"
md "%DataFolder%\jpbxlite\logs"
md "%DataFolder%\jpbxlite\lib"
md "%DataFolder%\jpbxlite\plugins"
md "%DataFolder%\jpbxlite\sounds\en"

copy *.jar "%AppFolder%\jpbxlite"
copy run.bat "%AppFolder%\jpbxlite"
	
copy plugins\*.jar "%DataFolder%\jpbxlite\plugins"

copy sounds\en\*.wav "%DataFolder%\jpbxlite\sounds\en"

echo Install complete! Use "%AppFolder%\jpbxlite\run.bat" to start server.

set AppFolder=
set DataFolder=
