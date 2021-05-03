#!/bin/bash
if [ $EUID -ne 0 ]; then
  echo Must be root to run this script.
  exit 1
fi
java -cp /usr/share/java/javaforce.jar:/usr/share/java/jfpbx/bouncycastle.jar:/usr/share/java/jfpbx/derby.jar:/usr/share/java/jfpbx/jfpbxcore.jar jfpbx.core.Main $*
