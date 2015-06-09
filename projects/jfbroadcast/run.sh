#!/bin/bash
if [ $EUID -ne 0 ]; then
  echo Must be root to run this script.
  exit 1
fi
java -cp javaforce.jar:derby.jar:jfbroadcast.jar Broadcast $*
