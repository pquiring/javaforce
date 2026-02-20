#!/bin/bash

# package all dep jars
# Many dep jars are not found or outdated within most Linux repos
# This script will package these dep jars to be placed in the JavaForce repos

# convert '-' properties to '_' properties
java -cp ../javaforce.jar javaforce.utils.ConvertProperties ../versions.properties versions_properties
. versions_properties
rm versions_properties

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
  echo Packaging $1 1>&2
  cp $1-$3.jar /usr/share/java/$1.jar
  echo /usr/share/java/$1.jar > files.lst
  if [ -f build.xml ]; then rm build.xml; fi
  echo "<project>" >> build.xml
  echo "<description>$1</description>" >> build.xml
  echo "<property name=\"app\" value=\"$2\"/>" >> build.xml
  echo "<property name=\"version\" value=\"$3\"/>" >> build.xml
  echo "<property name=\"home\" value=\"..\"/>" >> build.xml
  echo "</project>" >> build.xml
  java -cp ../javaforce.jar javaforce.utils.Gen$PKG build.xml null
  rm files.lst
  rm build.xml
}

detectos

package jcifs jfcifs $jcifs_version
package jsvg jfsvg $jsvg_version
package derby jfderby $derby_version
package sshd-core jfsshd-core $sshd_version
package sshd-common jfsshd-common $sshd_version
package sshd-sftp jfsshd-sftp $sshd_version
package sshd-scp jfsshd-scp $sshd_version
package slf4j-api jfslf4j-api $slf4j_version
package slf4j-jdk14 jfslf4j-jdk14 $slf4j_version
package slf4j-nop jfslf4j-nop $slf4j_version
package slf4j-reload4j jfslf4j-reload4j $slf4j_version
package slf4j-simple jfslf4j-simple $slf4j_version
package llrp jfllrp $llrp_version
package filters jffilters $filters_version
package bcprov jfbcprov $bouncycastle_version
package bctls jfbctls $bouncycastle_version
package bcutil jfbcutil $bouncycastle_version
package mina-core jfmina-core $mina_version
package log4j-api jflog4j-api $log4j_version
package log4j-core jflog4j-core $log4j_version
package log4j-1.2-api jflog4j-1.2-api $log4j_version
package mysql-jdbc jfmysql-jdbc $mysql_jdbc_version
package mssql-jdbc jfmssql-jdbc $mssql_jdbc_version
