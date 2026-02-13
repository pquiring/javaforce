#!/bin/bash

# build and package everything for Linux

# recommend running as : package.sh > build.log
# then search build.log for "error" or "fail"

ABORT=false

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  #script executed directly
  EXIT=exit
else
  #script sourced
  EXIT=return
fi

function detectos {
  if [ ! -f /etc/os-release ]; then
    echo Unable to detect os
    echo /etc/os-release not found!
    ABORT=true
    return
  fi
  . /etc/os-release
  case $ID in
    debian)
      pkg=deb
      PKG=DEB
      OS=debian
      #need to remove quotes from VERSION_ID
      RELEASE=${VERSION_ID//\"/}
      case $HOSTTYPE in
      x86_64)
        ARCH=amd64
        ;;
      aarch64)
        ARCH=arm64
        ;;
      *)
        echo Invalid HOSTTYPE!
        ABORT=true
        return
        ;;
      esac
      ;;
    fedora)
      pkg=rpm
      PKG=RPM
      OS=fedora
      RELEASE=$VERSION_ID
      ARCH=$HOSTTYPE
      ;;
    arch)
      pkg=pac
      PKG=PAC
      OS=arch
      RELEASE=latest
      ARCH=$HOSTTYPE
      ;;
    ubuntu)
      #VERSION_CODENAME varies
      echo Ubuntu not supported for packaging, please use Debian!
      echo Debian repo can be used in Ubuntu.
      ABORT=true
      return
      ;;
    *)
      echo Unknown os detected!
      echo ID=%ID
      ABORT=true
      return
      ;;
  esac
}

function package {
  #clean repo
  echo cleaning repo/$OS/$RELEASE/$ARCH/
  cd repo/$OS/$RELEASE/$ARCH
  chmod +x clean.sh
  ./clean.sh
  cd ../../../..

  #force rebuild everything
  find -name "*.class" | xargs rm 2>/dev/null

  echo Packaging for : $OS/$RELEASE on $ARCH
  echo Packaging for : $OS/$RELEASE on $ARCH 1>&2

  #package javaforce
  echo Packaging javaforce
  echo Packaging javaforce 1>&2
  ant $pkg

  #package utils
  echo Packaging jfutils
  echo Packaging jfutils 1>&2
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
}

detectos

if [[ "$ABORT" == "true" ]]; then
  $EXIT
fi

if [[ "$RELEASE" == "" ]]; then
  echo OS version not detected!
  $EXIT
fi

if [ ! -f javaforce.jar ]; then
  echo Please build javaforce first!
  $EXIT
fi

if [ ! -f ~/bin/linux64.bin ]; then
  echo Please build native first!
  $EXIT
fi

#install repo
if [ ! -f repo/readme.txt ]; then
  ant repo
fi

package
