@echo off

if "%1"=="package" goto package
if not "%1"=="" goto usage

for /d %%d in (*) do call package.bat package %%d

:package
if not exist %2\wix64.xml goto end
echo Packaging %2
echo Packaging %2 1>&2
cd %2
call ant msi
cd ..
goto end

:usage
echo This script will package all apps into MSI packages.
goto end

:end
