#!/bin/bash

# package all dep jars

# Many dep jars are not found or outdated within most Linux repos
# This script will package these dep jars to be placed in the JavaForce repos

function detectos {
  if [ ! -f /etc/os-release ]; then
    echo Unable to detect os
    echo /etc/os-release not found!
    exit
  fi
  . /etc/os-release
  case $ID in
    debian | ubuntu)
      pkg=deb
      PKG=DEB
      ;;
    fedora)
      pkg=rpm
      PKG=RPM
      ;;
    arch)
      pkg=pac
      PKG=PAC
      ;;
    *)
      echo Unknown os detected!
      echo ID=%ID
      exit
      ;;
  esac
}

function package {
  echo Packaging $1
  sudo cp $1-$3.jar /usr/share/java/$1.jar
  echo /usr/share/java/$1.jar > files.lst
  if [ -f build.xml ]; then rm build.xml; fi
  echo "<project>" >> build.xml
  echo "<description>$1</description>" >> build.xml
  echo "<property name=\"app\" value=\"$2\"/>" >> build.xml
  echo "<property name=\"version\" value=\"$3\"/>" >> build.xml
  echo "<property name=\"home\" value=\"..\"/>" >> build.xml
  echo "</project>" >> build.xml
  java -cp ../javaforce.jar javaforce.utils.Gen$PKG build.xml
  rm files.lst
  rm build.xml
}

detectos

# Note : these versions must match ..\versions.xml
package jcifs jfcifs 2.1.34
package derby jfderby 10.16.1.1
package sshd-core jfsshd-core 2.10.0
package sshd-common jfsshd-common 2.10.0
package sshd-sftp jfsshd-sftp 2.10.0
package sshd-scp jfsshd-scp 2.10.0
package slf4j-api jfslf4j-api 2.0.13
package slf4j-jdk14 jfslf4j-jdk14 2.0.13
package slf4j-nop jfslf4j-nop 2.0.13
package slf4j-reload4j jfslf4j-reload4j 2.0.13
package slf4j-simple jfslf4j-simple 2.0.13
package llrp jfllrp 2.0
package bcprov jfbcprov 1.76
package bctls jfbctls 1.76
package bcutil jfbcutil 1.76
package mina jfmina 2.2.3
package log4j-api jflog4j-api 2.23.1
package log4j-core jflog4j-api 2.23.1
package log4j-1.2-api jflog4j-1.2-api 2.23.1
