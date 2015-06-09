function build {
  if [ "$1" == "jrepo" ]; then
    return
  fi
  cd $1
  ant jar
  if [ "$1" == "plymouth-theme-jflinux" ]; then
    sudo ant install-fedora
  elif [ "$1" == "jlogon" ]; then
    sudo ant install-fedora
  else
    sudo ant install
  fi
  if [ "$1" == "jlogon" ]; then
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
