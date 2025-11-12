@echo off
echo Microsoft Visual C++ 2026 compiler setup

if not "%VCVER%"=="" goto end

SET VCTYPE=Community
SET VCYEAR=18
SET VCVER=14.50.35717
SET WINVER=10.0.26100.0

SET MSVS=C:\Program Files\Microsoft Visual Studio
SET MSSDK=C:\Program Files (x86)\Windows Kits

echo VCYEAR=%VCYEAR%
echo VCVER=%VCVER%
echo WINVER=%WINVER%

if "%1"=="detect" goto detect

::vcpkg
set VCPKG_DEFAULT_TRIPLET=x64-windows
::set VCPKG=c:\program files\vcpkg
::set PATH=%VCPKG%\installed\x64-windows\bin;%PATH%

::VC Paths
SET PATH=%MSVS%\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\bin\HostX64\x64;%PATH%
SET PATH=%MSVS%\%VCYEAR%\%VCTYPE%\Common7\IDE\VC\VCPackages;%PATH%
SET PATH=%MSVS%\%VCYEAR%\%VCTYPE%\Common7\IDE\CommonExtensions\Microsoft\TeamFoundation\Team Explorer;%PATH%
SET PATH=%MSVS%\%VCYEAR%\%VCTYPE%\MSBuild\Current\bin\Roslyn;%PATH%
SET PATH=%MSVS%\%VCYEAR%\%VCTYPE%\MSBuild\Current\bin;%PATH%
SET PATH=%MSVS%\%VCYEAR%\%VCTYPE%\Common7\IDE\;%PATH%
SET PATH=%MSVS%\%VCYEAR%\%VCTYPE%\Common7\Tools\;%PATH%
SET PATH=%MSSDK%\10\bin\%WINVER%\x64;%PATH%

::VC Includes
SET INCLUDE=%INCLUDE%;%MSVS%\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\include
SET INCLUDE=%INCLUDE%;%MSSDK%\10\Include\%WINVER%\ucrt
SET INCLUDE=%INCLUDE%;%MSSDK%\10\Include\%WINVER%\um
SET INCLUDE=%INCLUDE%;%MSSDK%\10\Include\%WINVER%\shared
::SET INCLUDE=%INCLUDE%;.;..

::VC Libs
SET LIB=%LIB%;%MSVS%\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\lib\x64
SET LIB=%LIB%;%MSSDK%\10\Lib\%WINVER%\um\x64
SET LIB=%LIB%;%MSSDK%\10\Lib\%WINVER%\ucrt\x64
SET LIB=%LIB%;%MSVS%\%VCYEAR%\%VCTYPE%\MSBuild\Current\Bin\Roslyn

::VC Libpaths
SET LIBPATH=%LIBPATH%;%MSVS%\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\lib\x64
SET LIBPATH=%LIBPATH%;%MSVS%\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\lib\x86\store\references
::SET LIBPATH=%LIBPATH%;C:\Windows\Microsoft.NET\Framework64\v4.0.30319

::VC IFCPATH
SET IFCPATH=%MSVS%\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\ifc\x64

::VC misc vars
SET VS170COMNTOOLS=%MSVS%\%VCYEAR%\%VCTYPE%\Common7\Tools\
SET VCIDEInstallDir=%MSVS%\%VCYEAR%\%VCTYPE%\Common7\IDE\VC\
SET VCINSTALLDIR=%MSVS%\%VCYEAR%\%VCTYPE%\VC\
SET VCToolsInstallDir=%MSVS%\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\%VCVER%\
SET VCToolsRedistDir=%MSVS%\%VCYEAR%\%VCTYPE%\VC\Redist\MSVC\%VCVER%\
SET VCToolsVersion=%VCVER%
SET VisualStudioVersion=18.0

::set default properties
set Configuration=Release
set Platform=x64

goto end

:detect
dir "%MSVS%\%VCYEAR%\%VCTYPE%\VC\Tools\MSVC\"
dir "%MSSDK%\10\bin\"

:end
