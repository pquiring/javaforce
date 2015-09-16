function build {
  if [ "$1" == "jrepo" ]; then
    return
  fi
  cd $1
  ant jar
  if [ "$1" == "plymouth-theme-jflinux" ]; then
    sudo ant install-fedora -Dbits=64
  elif [ "$1" == "jlogon" ]; then
    sudo ant install-fedora -Dbits=64
  else
    sudo ant install -Dbits=64
  fi
  if [ "$1" == "jlogon" ]; then
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
