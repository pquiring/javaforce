#!/bin/bash

# install linux deps for building javaforce

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
    freebsd)
      OS=freebsd

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
  apt update
  apt --yes install default-jdk ant git
}

function fedora {
  dnf -y install java-17-openjdk-devel ant git
}

function arch {
  pacman -Sy
  pacman -S --noconfirm jdk17-openjdk apache-ant git
}

function freebsd {
  pkg install -y git ant
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
  freebsd)
    freebsd
    ;;
  *)
    echo OS installation not available yet!
    echo OS=$OS
    exit
    ;;
esac

echo Deps install complete!
