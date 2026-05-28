#!/bin/bash

if [ "$1" = "" ]; then

  echo Usage : build.sh docker-file

else

  FILE=$1
  BASE=${FILE%.*}

  docker build -f $FILE -t amd64/$BASE ..

fi
