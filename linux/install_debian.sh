#!/bin/bash

#check if root
if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root" 
  exit 1
fi

#convert gnu $HOSTTYPE to debian $ARCH
#see /usr/share/dpkg/cputable
case $HOSTTYPE in
x86_64)
  ARCH=amd64
  ;;
aarch64)
  ARCH=arm64
  ;;
*)
  echo Invalid HOSTTYPE!
  exit
  ;;
esac

apt -y install wget

#configure JavaForce repository
cd /etc/apt/sources.list.d
if [ ! -f javaforce.list ]; then
  echo Download javaforce.list
  wget http://javaforce.sf.net/debian/$ARCH/javaforce.list
  chmod 644 javaforce.list
fi
cd /etc/apt/trusted.gpg.d
if [ ! -f javaforce.gpg ]; then
  echo Download javaforce.gpg
  wget http://javaforce.sf.net/debian/$ARCH/javaforce.gpg
  chmod 644 javaforce.gpg
fi
cd /

apt update
apt --yes install javaforce

read -p "Install jfLinux Desktop Environment? " -n 1 -r

if [[ $REPLY =~ ^[Yy]$ ]]
then
  apt --yes install jflogon jfdesktop jfconfig jfapps
  systemctl enable jflogon.service
  systemctl enable jfbusserver.service
fi

echo Install complete, please reboot!
