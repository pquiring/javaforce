#!/bin/bash

# installs the JetBrains Runtime : https://github.com/JetBrains/JetBrainsRuntime
# OpenJDK fork with Project Wakefield implemented : https://openjdk.org/projects/wakefield/

case $HOSTTYPE in
x86_64)
  JBRARCH=x64
  ARCH=amd64
  ;;
aarch64)
  JBRARCH=aarch64
  ARCH=arm64
  ;;
*)
  echo Invalid HOSTTYPE!
  exit
  ;;
esac

VERSION=25.0.4.1
BUILD=b583.48
BASE=jbrsdk-$VERSION-linux-$JBRARCH-$BUILD
TAR=$BASE.tar.gz
FOLDER=jbrsdk-$VERSION-openjdk-$ARCH

function install_link() {
  if [ -f /usr/bin/$1 || -h /usr/bin/$1 ]; then
    rm /usr/bin/$1
  fi
  ln -s /opt/$FOLDER/bin/$1 /usr/bin/$1
}

function install_sdk() {

  cd /opt

  wget https://cache-redirector.jetbrains.com/intellij-jbr/$TAR
  tar xf $TAR
  mv $BASE $FOLDER

  install_link java
  install_link javac
  install_link javadoc
  install_link jlink
  install_link jimage
  install_link keytool

}

if [ -f /usr/bin/java ]; then
  echo /usr/bin/java already exists, please uninstall all JVMs.
else
  install_sdk
fi
