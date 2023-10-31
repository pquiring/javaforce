#!/bin/bash

# build and package everything for Linux

function detectos {
  if [ ! -f /etc/os-release ]; then
    echo Unable to detect os
    echo /etc/os-release not found!
    exit
  fi
  . /etc/os-release
  case $ID in
    debian | ubuntu)
      pkg=deb
      PKG=DEB
      ;;
    fedora)
      pkg=rpm
      PKG=RPM
      ;;
    arch)
      pkg=pac
      PKG=PAC
      ;;
    *)
      echo Unknown os detected!
      echo ID=%ID
      exit
      ;;
  esac
}

detectos

if [ ! -f javaforce.jar ]; then
  echo Please build javaforce first!
  exit
fi

if [ ! -f native/linux64.bin ]; then
  echo Please build native first!
  exit
fi

ant repo
ant $pkg
cd projects
chmod +x package.sh
./package.sh
cd ../lib
chmod +x package.sh
./package.sh
cd ..

echo Build Complete!
