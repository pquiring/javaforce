#!/bin/bash

#check if root
if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root" 
  exit 1
fi

if [ "$1" == "" ]; then
  echo usage : install_debian.sh {ARCH}
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

#configure JavaForce repository
cd /etc/apt/sources.list.d
if [ ! -f javaforce.list ]; then
  echo Download javaforce.list
  wget http://javaforce.sf.net/ubuntu/$ARCHEXT/javaforce.list
  chmod 644 javaforce.list
fi
cd /etc/apt/trusted.gpg.d
if [ ! -f javaforce.gpg ]; then
  echo Download javaforce.gpg
  wget http://javaforce.sf.net/ubuntu/$ARCHEXT/javaforce.gpg
  chmod 644 javaforce.gpg
fi
cd /

apt update
apt install jflogon jfdesktop jfapps
systemctl enable jflogon.service

