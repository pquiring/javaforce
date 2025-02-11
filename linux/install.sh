#!/bin/bash

# install jfLinux onto pre-installed minimal linux

DESKTOP=ask
JAVAFORCE=yes
UPDATE=no

for arg in "$@"
do
  if [ "$arg" = "--desktop=no" ]; then
    DESKTOP=no
  fi
  if [ "$arg" = "--desktop=yes" ]; then
    DESKTOP=yes
  fi
  if [ "$arg" = "--javaforce=no" ]; then
    JAVAFORCE=no
  fi
  if [ "$arg" = "--javaforce=yes" ]; then
    JAVAFORCE=yes
  fi
  if [ "$arg" = "--update=no" ]; then
    UPDATE=no
  fi
  if [ "$arg" = "--update=yes" ]; then
    UPDATE=yes
  fi
done

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
  if [ ! -f /etc/apt/sources.list.d/javaforce.list ]; then
    echo Download javaforce.list
    wget -NP /etc/apt/sources.list.d http://javaforce.sf.net/debian/$VERSION_CODENAME/$ARCH/javaforce.list
    chmod 644 javaforce.list
  fi
  if [ $UPDATE = "yes" ]; then
    rm /etc/apt/trusted.gpg.d/javaforce.gpg
  fi
  if [ ! -f /etc/apt/trusted.gpg.d/javaforce.gpg ]; then
    echo Download javaforce.gpg
    wget -NP /etc/apt/trusted.gpg.d http://javaforce.sf.net/debian/$VERSION_CODENAME/$ARCH/javaforce.gpg
    chmod 644 javaforce.gpg
  fi

  apt update
  if [ $JAVAFORCE = "yes" ]; then
    apt --yes install javaforce
  fi

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
  if [ ! -f /etc/yum.repos.d/javaforce.repo ]; then
    echo Download javaforce.repo
    wget -NP /etc/yum.repos.d http://javaforce.sf.net/fedora/$VERSION_ID/$HOSTTYPE/javaforce.repo
    chmod 644 javaforce.repo
  fi

  dnf update
  if [ $JAVAFORCE = "yes" ]; then
    dnf -y install javaforce
  fi

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

  if [ $UPDATE = "yes" ]; then
    rm /tmp/javaforce.gpg
  fi

  if [ ! -f /tmp/javaforce.gpg ]; then
    echo Download javaforce.gpg
    wget -NP /tmp http://javaforce.sf.net/arch/$HOSTTYPE/javaforce.gpg
    chmod 644 /tmp/javaforce.gpg
  fi

  pacman-key --add /tmp/javaforce.gpg
  #gpg --show-key /tmp/javaforce.gpg
  pacman-key --lsign-key A67121E5285E7FE2290FCCB5DB9C4FA6AFF0CBDC

  #update everything
  pacman -Syy

  if [ $JAVAFORCE = "yes" ]; then
    pacman -S --noconfirm javaforce
  fi

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
