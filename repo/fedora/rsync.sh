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
  ARCHEXT=i686
  ;;
x64)
  ARCHEXT=x86_64
  ;;
a32)
  ARCHEXT=armv7hl
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
FOLDER=/home/project-web/javaforce/htdocs/fedora/$ARCHEXT
rsync -rv *.rpm *.repo RPM-GPG-KEY-javaforce repodata $USERNAME@$SITE:$FOLDER
