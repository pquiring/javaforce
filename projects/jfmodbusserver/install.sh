#!/bin/bash
cp jfmodbusserver /usr/bin
cp *.jar /usr/share/java
cp *.so /usr/lib
cp jfmodbusserver.service /usr/lib/systemd/system
