#!/bin/bash

if [ $EUID -ne 0 ]; then
  echo Must be root to run this script.
  exit 1
fi

echo jPBXlite manual install.
echo This will place files into required location
echo Press Enter to Continue?
read

mkdir -p /usr/share/java/jfpbx
mkdir -p /var/log/jfpbx
mkdir -p /var/lib/jfpbx/voicemail
mkdir -p /usr/share/java/jfpbx/plugins
mkdir -p /usr/share/sounds/jfpbx/en

cp javaforce.jar /usr/share/java
cp *.jar /usr/share/java/jfpbx
cp run.sh /usr/bin/jfpbxlite
chmod +x /usr/bin/jfpbxlite

cp plugins/*.jar /usr/share/java/jfpbx/plugins

cp sounds/en/*.wav /usr/share/sounds/jfpbx/en

echo Install complete! Run jfpbxlite to start server.
