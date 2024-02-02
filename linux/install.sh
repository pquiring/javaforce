#!/bin/bash

# install jfLinux onto pre-installed minimal linux

DESKTOP=ask
if [ "$1" = "--desktop=no" ]; then
  DESKTOP=no
fi
if [ "$1" = "--desktop=yes" ]; then
  DESKTOP=yes
fi

function detectos {
  if [ ! -f /etc/os-release ]; then
    echo Unable to detect os
    echo /etc/os-release not found!
    exit
  fi
  . /etc/os-release
  case $ID in
    debian | ubuntu)
      OS=debian

      ;;
    fedora)
      OS=fedora

      ;;
    arch)
      OS=arch

      ;;
    *)
      echo Unknown os detected!
      echo ID=%ID
      exit
      ;;
  esac
}

#check if root
if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root"
  exit 1
fi

function debian {
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

  if [ $DESKTOP = "ask" ]; then
    read -p "Install jfLinux Desktop Environment? " -n 1 -r

    if [[ $REPLY =~ ^[Yy]$ ]]; then
      DESKTOP=yes
    fi
  fi
  if [ $DESKTOP = "yes" ]; then
    apt --yes install jflogon jfdesktop jfconfig jfapps
    systemctl enable jflogon.service
    systemctl enable jfbusserver.service
  fi
}

function fedora {
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

  if [ $DESKTOP = "ask" ]; then
    read -p "Install jfLinux Desktop Environment? " -n 1 -r

    if [[ $REPLY =~ ^[Yy]$ ]]; then
      DESKTOP=yes
    fi
  fi
  if [ $DESKTOP = "yes" ]; then
    dnf -y install jflogon jfdesktop jfconfig jfapps
    systemctl enable jflogon.service
    systemctl enable jfbusserver.service
  fi
}

function arch {
  case $HOSTTYPE in
  x86_64)
    ;;
  *)
    echo Invalid HOSTTYPE!
    exit
    ;;
  esac

  echo "[javaforce]" >> /etc/pacman.conf
  echo "SigLevel = TrustAll" >> /etc/pacman.conf
  echo "Server = http://javaforce.sourceforge.net/arch/$HOSTTYPE" >> /etc/pacman.conf

  if [ ! -f javaforce.gpg ]; then
    echo Download javaforce.gpg
    wget http://javaforce.sf.net/arch/$HOSTTYPE/javaforce.gpg
    chmod 644 javaforce.gpg
  fi

  pacman-key --add javaforce.gpg
  #gpg --show-key javaforce.gpg
  pacman-key --lsign-key A67121E5285E7FE2290FCCB5DB9C4FA6AFF0CBDC

  #update everything
  pacman -Syy

  pacman -S --noconfirm javaforce

  if [ $DESKTOP = "ask" ]; then
    read -p "Install jfLinux Desktop Environment? " -n 1 -r

    if [[ $REPLY =~ ^[Yy]$ ]]; then
      DESKTOP=yes
    fi
  fi
  if [ $DESKTOP = "yes" ]; then
    pacman -S --noconfirm jflogon jfdesktop jfconfig jfapps
    systemctl enable jflogon.service
    systemctl enable jfbusserver.service
  fi
}

detectos

case $OS in
  debian)
    debian
    ;;
  fedora)
    fedora
    ;;
  arch)
    arch
    ;;
  *)
    echo OS installation not available yet!
    echo OS=$OS
    exit
    ;;
esac

echo Install complete, please reboot!
