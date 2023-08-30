#!/bin/bash
if [ $EUID -ne 0 ]; then
  echo Must be root to run this script.
  exit 1
fi
java -cp /usr/share/java/javaforce.jar:/usr/share/java/jfpbx/bcprov.jar:/usr/share/java/jfpbx/bctls.jar::/usr/share/java/jfpbx/bcutil.jar:/usr/share/java/jfpbx/jfpbx.jar jfpbx.core.Main $*
