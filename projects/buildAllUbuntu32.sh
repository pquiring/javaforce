function build {
  if [ "$1" == "jphonelite-android" ]; then
    return
  fi
  if [ "$1" == "jfrdp" ]; then
    return
  fi
  cd $1
  ant jar
  if [ "$1" == "plymouth-theme-jflinux" ]; then
    sudo ant install-ubuntu -Dbits=32
  elif [ "$1" == "jflogon" ]; then
    sudo ant install-ubuntu -Dbits=32
  else
    sudo ant install -Dbits=32
  fi
  ant deb32
  cd ..
}

for i in *; do
  if [ -d $i ]; then
    build $i
  fi
done
