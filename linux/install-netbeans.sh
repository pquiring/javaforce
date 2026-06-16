#!/bin/bash

# Netbeans is not available in Debian repo

if [ ! -f /opt/netbeans/bin/netbeans ]; then
  version=30

  zip=netbeans-$version-bin.zip

  #no special folder

  cd /opt

  wget https://dlcdn.apache.org/netbeans/netbeans/$version/$zip

  unzip $zip

  #no special folder

  echo export PATH=/opt/netbeans/bin:\$PATH >> ~/.bashrc

  echo NetBeans Install complete!
  echo Please logout and login for bash PATH to update!
fi
