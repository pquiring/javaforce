@echo off
echo Microsoft C++ compiler setup

::Select C++ Edition
SET VCTYPE=BuildTools
::  or
::SET VCTYPE=Community

SET VCYEAR=2022

if "%1"=="detect" goto detect

if not "%VCVER%"=="" goto end

SET VCVER=14.42.34433
SET WINVER=10.0.22621.0

echo VCYEAR=%VCYEAR%
echo VCVER=%VCVER%
echo WINVER=%WINVER%

::vcpkg
set VCPKG_DEFAULT_TRIPLET=x64-windows
::set VCPKG=c:\program files\vcpkg
::set PATH=%VCPKG%\installed\x64-windows\bin;%PATH%

::VC Paths
SET PATH=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\bin\HostX64\x64;%PATH%
SET PATH=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\Common7\IDE\VC\VCPackages;%PATH%
SET PATH=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\Common7\IDE\CommonExtensions\Microsoft\TeamFoundation\Team Explorer;%PATH%
SET PATH=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\MSBuild\Current\bin\Roslyn;%PATH%
SET PATH=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\MSBuild\Current\bin;%PATH%
SET PATH=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\Common7\IDE\;%PATH%
SET PATH=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\Common7\Tools\;%PATH%
SET PATH=C:\Program Files (x86)\Windows Kits\10\bin\%WINVER%\x64;%PATH%

::VC Includes
SET INCLUDE=%INCLUDE%;C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\include
SET INCLUDE=%INCLUDE%;C:\Program Files (x86)\Windows Kits\10\Include\%WINVER%\ucrt
SET INCLUDE=%INCLUDE%;C:\Program Files (x86)\Windows Kits\10\Include\%WINVER%\um
SET INCLUDE=%INCLUDE%;C:\Program Files (x86)\Windows Kits\10\Include\%WINVER%\shared
::SET INCLUDE=%INCLUDE%;.;..

::VC Libs
SET LIB=%LIB%;C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\lib\x64
SET LIB=%LIB%;C:\Program Files (x86)\Windows Kits\10\Lib\%WINVER%\um\x64
SET LIB=%LIB%;C:\Program Files (x86)\Windows Kits\10\Lib\%WINVER%\ucrt\x64
SET LIB=%LIB%;c:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\MSBuild\Current\Bin\Roslyn

::VC Libpaths
SET LIBPATH=%LIBPATH%;C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\lib\x64
SET LIBPATH=%LIBPATH%;C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\lib\x86\store\references
::SET LIBPATH=%LIBPATH%;C:\Windows\Microsoft.NET\Framework64\v4.0.30319

::VC IFCPATH
SET IFCPATH=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\ifc\x64

::VC misc vars
SET VS170COMNTOOLS=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\Common7\Tools\
SET VCIDEInstallDir=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\Common7\IDE\VC\
SET VCINSTALLDIR=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\
SET VCToolsInstallDir=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\
SET VCToolsRedistDir=C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Redist\MSVC\%VCVER%\
SET VCToolsVersion=%VCVER%
SET VisualStudioVersion=17.0

::set default properties
set Configuration=Release
set Platform=x64

goto end

:detect
dir "C:\Program Files (x86)\Microsoft Visual Studio\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\"
dir "C:\Program Files (x86)\Windows Kits\10\bin\"

:end
