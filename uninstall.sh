#!/bin/bash

# uninstall everything for Linux

#uninstall javaforce
ant uninstallapp

#uninstall projects
cd projects
chmod +x uninstall.sh
./uninstall.sh
cd ..

#uninstall utils
cd utils
ant uninstallapp
cd ..

echo Uninstall Complete!
