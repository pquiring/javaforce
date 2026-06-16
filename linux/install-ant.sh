#!/bin/bash

# installing ant thru debian repo forces default-jre which is still based on Java 21 so install ant manually

if [ ! -f /opt/ant/bin/ant ]; then
  version=1.10.17

  zip=apache-ant-$version-bin.zip

  folder=apache-ant-$version

  cd /opt

  wget https://dlcdn.apache.org/ant/binaries/$zip

  unzip $zip

  mv $folder ant

  echo export PATH=/opt/ant/bin:\$PATH >> ~/.bashrc

  echo Ant Install complete!
  echo Please logout and login for bash PATH to update!
fi
