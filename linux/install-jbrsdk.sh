#!/bin/bash

VERSION=25.0.4.1
BUILD=b583.48
BASE=jbrsdk-$VERSION-linux-x64-$BUILD
TAR=$BASE.tar.gz
FOLDER=jbrsdk-$VERSION-openjdk-amd64

function install_link() {
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
