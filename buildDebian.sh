#!/bin/bash

function build {
  ant jar
  sudo ant install -Dbits=$1
  ant deb -Dbits=$1 -Darch=$2 -Darchext=$3
}

if [ "$1" == "" ]; then
  echo usage : buildDebian.sh {ARCH}
  echo where ARCH=x32,x64,a32,a64
  exit
fi

ARCH=$1
BITS=${ARCH:1:2}

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

build $BITS $ARCH $ARCHEXT
