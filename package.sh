#!/bin/bash

# build and package everything for Linux

# recommend running as : package.sh > build.log
# then search build.log for "error" or "fail"

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
      OS=debian
      sudo apt install bzip2
      case $HOSTTYPE in
      x86_64)
        ARCH=amd64
        ;;
      aarch64)
        ARCH=arm64
        ;;
      *)
        echo Invalid HOSTTYPE!
        exit
        ;;
      esac
      ;;
    fedora)
      pkg=rpm
      PKG=RPM
      OS=fedora
      ARCH=$HOSTTYPE
      ;;
    arch)
      pkg=pac
      PKG=PAC
      OS=arch
      ARCH=$HOSTTYPE
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

#install repo
if [ ! -f repo/readme.txt ]; then
  ant repo
fi

#clean repo
echo cleaning repo/$OS/$ARCH/
cd repo/$OS/$ARCH
chmod +x clean.sh
./clean.sh
cd ../../..

#force rebuild everything
find -name "*.class" | xargs rm 2>/dev/null

#package javaforce
echo Packaging javaforce
echo Packaging javaforce 1>&2
ant $pkg

#package utils
echo Packaging utils
echo Packaging utils 1>&2
cd utils
ant $pkg
cd ..

#package projects
cd projects
chmod +x package.sh
./package.sh
cd ..

#package libs
cd lib
chmod +x package.sh
./package.sh
cd ..

echo Build Complete!
