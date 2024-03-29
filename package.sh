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
      case $HOSTTYPE in
      x86_64)
        ARCH=amd64
        ;;
      *)
        echo Invalid HOSTTYPE!
        exit
        ;;
      esac
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
ant repo

#clean repo
echo cleaning repo/$OS/$ARCH/\*.$pkg
rm repo/$OS/$ARCH/*.$pkg

#package javaforce
ant $pkg

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
