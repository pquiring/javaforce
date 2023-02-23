#!/bin/bash

#check if root
if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root" 
  exit 1
fi

dnf -y install wget

#configure JavaForce repository
cd /etc/yum.repos.d
if [ ! -f javaforce.repo ]; then
  echo Download javaforce.repo
  wget http://javaforce.sf.net/fedora/$HOSTTYPE/javaforce.repo
  chmod 644 javaforce.repo
fi
cd /

dnf update
dnf -y install javaforce

read -p "Install jfLinux Desktop Environment? " -n 1 -r

if [[ $REPLY =~ ^[Yy]$ ]]
then
  dnf -y install jflogon jfdesktop jfconfig jfapps
  systemctl enable jflogon.service
  systemctl enable jfbusserver.service
fi

echo Install complete, please reboot!
