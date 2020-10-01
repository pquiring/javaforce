@echo off
if "%1"=="web" start http://localhost
java -cp javaforce.jar;bouncycastle.jar;jfpbxcore.jar jfpbx.core.Main %*
