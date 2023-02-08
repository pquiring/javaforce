#!/bin/bash

#check if root
if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root" 
  exit 1
fi

case $HOSTTYPE in
x86_64)
  ARCH=amd64
  ;;
aarch64)
  ARCHEXT=arm64
  ;;
*)
  echo Invalid HOSTTYPE!
  exit
  ;;
esac

dnf -y install wget

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
