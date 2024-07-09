@echo off

if "%1"=="" goto all

if not exist %1\wix64.xml goto end
echo Packaging %1
echo Packaging %1 1>&2
cd %1
call ant msi
cd ..
goto end

:all
for /d %%d in (*) do call package.bat %%d

:end
