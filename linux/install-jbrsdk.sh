#!/bin/bash

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
update-alternatives --install /usr/bin/java java /usr/lib/jvm/jbrsdk-$VERSION-openjdk-amd64/bin/java 2540
update-alternatives --set java /usr/lib/jvm/jbrsdk-$VERSION-openjdk-amd64/bin/java
