#!/bin/bash

# NOTE : you must run repo/docker-init.sh to setup docker/qemu

if [ "$1" = "" ]; then

  echo Usage : build.sh docker-file

else

  FILE=$1
  BASE=${FILE%.*}

  docker build --platform linux/arm64 -f $FILE -t arm64v8/$BASE ..

fi
