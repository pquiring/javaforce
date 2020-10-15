#!/bin/bash

function build {
  ant jar
  sudo ant install
  ant deb -Darch=$1 -Darchext=$2
}

if [ "$1" == "" ]; then
  echo usage : buildDebian.sh {ARCH}
  echo where ARCH=x32,x64,a32,a64
  exit
fi

ARCH=$1

case $1 in
x32)
  ARCHEXT=i386
  ;;
x64)
  ARCHEXT=amd64
  ;;
a32)
  ARCHEXT=armhf
  ;;
a64)
  ARCHEXT=aarch64
  ;;
*)
  echo Invalid arch!
  exit
  ;;
esac

apt --yes install sudo bzip2

build $ARCH $ARCHEXT
