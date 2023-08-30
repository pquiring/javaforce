@echo off
if "%1"=="web" start http://localhost
java -cp javaforce.jar;bcprov.jar;bctls.jar;jfpbx.jar jfpbx.core.Main %*
