#!/bin/bash

# build and package everything for Debian

ant repo
ant deb
cd projects
./buildAllDebian.sh
cd ../jars
./buildAllDebian.sh
cd ..
