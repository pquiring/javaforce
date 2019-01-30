#!/bin/bash
if [ "$1" == "" ]; then
  echo usage : install.sh {ARCH}
  echo where ARCH=x32,x64,a32,a64
  exit
fi

case $1 in
x32)
  ARCH=i386
  ;;
x64)
  ARCH=amd64
  ;;
a32)
  ARCH=armhf
  ;;
a64)
  ARCH=aarch64
  ;;
*)
  echo Invalid arch!
  exit
  ;;
esac

#download JavaForce Repo file
cd /etc/apt/sources.list.d
if [ ! -f javaforce.list ]; then
  echo Download javaforce.list
  wget http://javaforce.sf.net/debian/$ARCH/javaforce.list
fi
cd /etc/apt/trusted.gpg.d
if [ ! -f javaforce.gpg ]; then
  echo Download javaforce.gpg
  wget http://javaforce.sf.net/debian/$ARCH/javaforce.gpg
fi

echo JavaForce repo installed!
