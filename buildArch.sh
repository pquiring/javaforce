#!/bin/bash

# build and package everything for Arch

ant repo
ant pac
cd projects
./buildAllArch.sh
cd ../jars
./buildAllArch.sh
cd ..
