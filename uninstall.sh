#!/bin/bash

# uninstall everything for Linux

#uninstall javaforce
ant uninstallapp

#uninstall projects
cd projects
chmod +x uninstall.sh
./uninstall.sh
cd ..

echo Uninstall Complete!
