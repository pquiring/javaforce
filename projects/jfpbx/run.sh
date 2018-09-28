#!/bin/bash
if [ $EUID -ne 0 ]; then
  echo Must be root to run this script.
  exit 1
fi
java -cp /usr/share/java/javaforce.jar:/usr/share/java/jpbx/bouncycastle.jar:/usr/share/java/jpbx/derby.jar:/usr/share/java/jpbx/jfpbxcore.jar jpbx.core.Main $*
