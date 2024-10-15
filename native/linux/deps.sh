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
  apt --yes install g++ default-jdk ant libx11-dev libfuse-dev libpam0g-dev libavcodec-dev libavformat-dev libavutil-dev libswscale-dev mesa-common-dev libxcursor-dev libxrandr-dev libxinerama-dev libxi-dev libxt-dev libncurses-dev libvirt-dev zlib1g-dev sudo libv4l-0
}

function fedora {
  dnf -y install gcc-c++ java-17-openjdk-devel ant libX11-devel fuse-devel pam-devel ffmpeg-free-devel mesa-libGL-devel libXcursor-devel libXrandr-devel libXinerama-devel libXi-devel libXt-devel libvirt-devel ncurses-devel rpm-build libv4l
}

function arch {
  pacman -Sy
  pacman -S --noconfirm jdk17-openjdk apache-ant fuse pam ffmpeg mesa libxcursor libxrandr libxinerama libxi libxt libvirt v4l-utils
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
