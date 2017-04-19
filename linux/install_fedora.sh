#!/bin/bash

#check if root
if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root" 
  exit 1
fi

if [ "$1" == "" ]; then
  echo usage : install_fedora.sh {ARCH}
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

#configure JavaForce repository
cd /etc/yum.repos.d
if [ ! -f javaforce.repo ]; then
  echo Download javaforce.repo
  wget http://javaforce.sf.net/fedora/$ARCHEXT/javaforce.repo
  chmod 644 javaforce.repo
fi
cd /

dnf update
dnf -y install jflogon jfdesktop jfconfig jfapps
systemctl enable jflogon.service

echo Install complete, please reboot!
