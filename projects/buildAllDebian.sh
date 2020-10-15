#!/bin/bash

function build {
  if [ "$1" == "jfrdp" ]; then
    return
  fi
  cd $1
  ant jar
  sudo ant install
  ant deb -Darch=$2 -Darchext=$3
  cd ..
}

if [ "$1" == "" ]; then
  echo usage : buildAllDebian.sh {ARCH}
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

for i in *; do
  if [ -d $i ]; then
    build $i $ARCH $ARCHEXT
  fi
done
