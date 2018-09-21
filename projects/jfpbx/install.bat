@echo off

echo jfPBX manual install.
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

md "%AppFolder%\jfpbx"
md "%DataFolder%\jfpbx"
md "%DataFolder%\jfpbx\logs"
md "%DataFolder%\jfpbx\lib"
md "%DataFolder%\jfpbx\plugins"
md "%DataFolder%\jfpbx\sounds\en"

copy *.jar "%AppFolder%\jfpbx"
copy run.bat "%AppFolder%\jfpbx"
	
copy plugins\*.jar "%DataFolder%\jfpbx\plugins"

copy sounds\en\*.wav "%DataFolder%\jfpbx\sounds\en"

echo Install complete! Use "%AppFolder%\jfpbx\run.bat" to start server.

set AppFolder=
set DataFolder=
