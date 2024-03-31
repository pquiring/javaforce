#!/bin/bash

# build and package all packages

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

function build {
  if [ "$1" == "jfbackup" ]; then
    return
  fi
  cd $1
  ant $pkg
  cd ..
}

detectos
for i in *; do
  if [ -d $i ]; then
    build $i
  fi
done
