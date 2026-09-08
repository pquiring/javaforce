#!/bin/bash

VERSION=25.04
FILE=jbrsdk-25.0.4.1-linux-x64-b583.48.tar.gz

cd /opt

wget https://cache-redirector.jetbrains.com/intellij-jbr/$FILE
cd jbrsdk-$VERSION-amd64
tar xf $FILE
mv jbrsdk-25.0.4.1-linux-x64-b583.48 jbrsdk-$VERSION-amd64

cd /usr/lib/jvm
wget http://pquiring.github.io/javaforce/linux/.jbrsdk-$VERSION-openjdk-amd64.jinfo

update-java-alternatives -s jbrsdk-$VERSION-openjdk-amd64
