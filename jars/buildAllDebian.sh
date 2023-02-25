#!/bin/bash

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
  java -cp javaforce.jar javaforce.utils.GenDEB $2 $3 ..
  rm files.lst
  rm build.xml
}

. ./package-all.sh
