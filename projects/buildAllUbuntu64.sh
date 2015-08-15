function build {
  cd $1
  ant jar
  if [ "$1" == "plymouth-theme-jflinux" ]; then
    sudo ant install-ubuntu -Dbits=64
  elif [ "$1" == "jlogon" ]; then
    sudo ant install-ubuntu -Dbits=64
  else
    sudo ant install -Dbits=64
  fi
  ant deb
  cd ..
}

for i in *; do
  if [ -d $i ]; then
    build $i
  fi
done
