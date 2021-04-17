#!/bin/bash

function build {
  if [ "$1" == "jphonelite-android" ]; then
    return
  fi
  if [ "$1" == "jfrdp" ]; then
    return
  fi
  cd $1
  if [ "$1" == "jflogon" ]; then
    sudo ant rpm
  else
    ant rpm
  fi
  cd ..
}

for i in *; do
  if [ -d $i ]; then
    build $i
  fi
done
