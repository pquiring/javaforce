@echo off

:: This script will copy FreeRDP from msys with all deps

:: install FreeRDP : pacman -S mingw-w64-ucrt-x86_64-freerdp

:: install ntldd : pacman -S mingw-w64-ucrt-x86_64-ntldd

set MSYS=c:\bin\msys\ucrt64\bin

copy %MSYS%\wfreerdp.exe
copy %MSYS%\sdl-freerdp.exe

ntldd -R %MSYS%\wfreerdp.exe | grep lib | gawk '{print $1}' > list.txt

for /f %%a in (list.txt) do copy %MSYS%\%%a .

ntldd -R %MSYS%\sdl-freerdp.exe | grep lib | gawk '{print $1}' > list.txt

for /f %%a in (list.txt) do copy %MSYS%\%%a .

del list.txt
