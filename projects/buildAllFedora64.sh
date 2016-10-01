function build {
  if [ "$1" == "jrepo" ]; then
    return
  fi
  if [ "$1" == "jphonelite-android" ]; then
    return
  fi
  if [ "$1" == "jfrdp" ]; then
    return
  fi
  cd $1
  ant jar
  sudo ant install -Dbits=64
  if [ "$1" == "jflogon" ]; then
    sudo ant rpm64
  else
    ant rpm64
  fi
  cd ..
}

for i in *; do
  if [ -d $i ]; then
    build $i
  fi
done
