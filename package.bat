@echo off

call ant msi

cd projects
call package.bat
cd ..
