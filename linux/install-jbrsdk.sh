#!/bin/bash

VERSION=25.0.4
FILE=jbrsdk-25.0.4.1-linux-x64-b583.48.tar.gz

cd /usr/lib/jvm

wget https://cache-redirector.jetbrains.com/intellij-jbr/$FILE
tar xf $FILE
mv jbrsdk-25.0.4.1-linux-x64-b583.48 jbrsdk-$VERSION-openjdk-amd64

wget http://pquiring.github.io/javaforce/linux/jbrsdk-$VERSION-openjdk-amd64.jinfo
mv jbrsdk-$VERSION-openjdk-amd64.jinfo .jbrsdk-$VERSION-openjdk-amd64.jinfo

update-java-alternatives -s jbrsdk-$VERSION-openjdk-amd64
