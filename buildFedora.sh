#!/bin/bash

# build and package everything for Fedora

ant repo
ant rpm
cd projects
./buildAllFedora.sh
cd ../jars
./buildAllFedora.sh
cd ..
