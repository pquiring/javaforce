#!/bin/bash

function build {
  if [ "$1" == "jphonelite-android" ]; then
    return
  fi
  if [ "$1" == "jfrdp" ]; then
    return
  fi
  cd $1
  ant pac
  cd ..
}

for i in *; do
  if [ -d $i ]; then
    build $i
  fi
done
