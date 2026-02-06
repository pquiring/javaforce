#!/bin/bash
cd /opt
VERSION=28
wget https://dlcdn.apache.org/netbeans/netbeans/$VERSION/netbeans-$VERSION-bin.zip
unzip netbeans-$VERSION-bin.zip
echo NetBeans installed to /opt/netbeans
echo Run /opt/netbeans/bin/netbeans
