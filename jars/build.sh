#!/bin/bash

# package all dep jars

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
  cp $1.jar /usr/share/java
  echo /usr/share/java/$1.jar > files.lst
  if [ -f build.xml ]; then rm build.xml; fi
  echo "<project>" >> build.xml
  echo "<description>$1</description>" >> build.xml
  echo "<property name=\"app\" value=\"$2\"/>" >> build.xml
  echo "<property name=\"version\" value=\"$3\"/>" >> build.xml
  echo "</project>" >> build.xml
  java -cp javaforce.jar javaforce.utils.Gen$PKG $2 $3 ..
  rm files.lst
  rm build.xml
}

detectos

package jcifs jfcifs 1.3.19
package jsch jfsch 0.1.55
package jzlib jfzlib 1.1.3
package derby jfderby 10.11.1.1
