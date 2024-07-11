@echo off

net session >nul 2>&1
if %errorlevel% equ 0 (
  winget source add JavaForce https://javaforce.sourceforge.net/windows/amd64 Microsoft.Rest
) else (
  echo Must have admin rights!
)

