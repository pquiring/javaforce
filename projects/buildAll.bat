@echo off
for /d %%a in (*) do (
  cd %%a
  ant
  cd..
)
