function build {
  cd $1
  ant jar
  if [ "$1" == "plymouth-theme-jflinux" ]; then
    sudo ant install-ubuntu
  elif [ "$1" == "jlogon" ]; then
    sudo ant install-ubuntu
  else
    sudo ant install
  fi
  ant deb
  cd ..
}

for i in *; do
  if [ -d $i ]; then
    build $i
  fi
done
