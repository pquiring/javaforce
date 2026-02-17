#!/bin/bash

# install linux deps for building native binaries

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
  xargs -d '\n' apt --yes install < deb-build.deps
}

function fedora {
  xargs -d '\n' dnf -y install gcc-c++ < rpm-build.deps
}

function arch {
  pacman -Sy
  xargs -d '\n' pacman -S --noconfirm < pac-build.deps
}

function freebsd {
  pkg install -y gcc ffmpeg libX11 libXrandr libXcursor libXinerama libXi libXt libvirt ncurses mesa-libs
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
