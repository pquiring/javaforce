#!/bin/bash

if [ $EUID -ne 0 ]; then
  echo Must be root to run this script.
  exit 1
fi

echo jPBXlite manual install.
echo This will place files into required location
echo Press Enter to Continue?
read

mkdir -p /usr/share/java/jpbx
mkdir -p /var/log/jpbx
mkdir -p /var/lib/jpbx/voicemail
mkdir -p /usr/share/java/jpbx/plugins
mkdir -p /usr/share/sounds/jpbx/en

cp javaforce.jar /usr/share/java
cp *.jar /usr/share/java/jpbx
cp run.sh /usr/bin/jpbxlite
chmod +x /usr/bin/jpbxlite

cp plugins/*.jar /usr/share/java/jpbx/plugins

cp sounds/en/*.wav /usr/share/sounds/jpbx/en

echo Install complete! Run jpbxlite to start server.
