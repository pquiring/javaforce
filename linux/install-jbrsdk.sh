#!/bin/bash

function install() {
  update-alternatives --install /usr/bin/$1 $1 /usr/lib/jvm/jbrsdk-$VERSION-openjdk-amd64/bin/$1 2540
  update-alternatives --set $1 /usr/lib/jvm/jbrsdk-$VERSION-openjdk-amd64/bin/$1
}

VERSION=25.0.4
FILE=jbrsdk-25.0.4.1-linux-x64-b583.48.tar.gz

cd /usr/lib/jvm

wget https://cache-redirector.jetbrains.com/intellij-jbr/$FILE
tar xf $FILE
mv jbrsdk-25.0.4.1-linux-x64-b583.48 jbrsdk-$VERSION-openjdk-amd64

#install with update-java-alternatives (not working)
#wget http://pquiring.github.io/javaforce/linux/jbrsdk-$VERSION-openjdk-amd64.jinfo
#mv jbrsdk-$VERSION-openjdk-amd64.jinfo .jbrsdk-$VERSION-openjdk-amd64.jinfo
#update-java-alternatives -s jbrsdk-$VERSION-openjdk-amd64

#install manually
install java
install javac
install javadoc
install jlink
install jimage
install keytool
