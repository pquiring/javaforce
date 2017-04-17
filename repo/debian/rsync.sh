#!/bin/bash

if [ "$1" == "" ]; then
  echo usage : rsync.sh {ARCH}
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

USERNAME=digiboy86,javaforce
SITE=web.sf.net
FOLDER=/home/project-web/javaforce/htdocs/debian/$ARCHEXT
rsync -v *.deb Packages Release InRelease *.list *.gpg $USERNAME@$SITE:$FOLDER
