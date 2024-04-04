#!/bin/bash

# uninstall all packages

function uninstall {
  cd $1
  ant uninstallapp
  cd ..
}

for i in *; do
  if [ -d $i ]; then
    uninstall $i
  fi
done
