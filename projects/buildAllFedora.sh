function build {
  if [ "$1" == "jphonelite-android" ]; then
    return
  fi
  if [ "$1" == "jfrdp" ]; then
    return
  fi
  cd $1
  ant jar
  sudo ant install
  if [ "$1" == "jflogon" ]; then
    sudo ant rpm -Darch=$2 -Darchext=$3
  else
    ant rpm -Darch=$2 -Darchext=$3
  fi
  cd ..
}

if [ "$1" == "" ]; then
  echo usage : buildAllFedora.sh {ARCH}
  echo where ARCH=x32,x64,a32,a64
  exit
fi

ARCH=$1

case $1 in
x32)
  ARCHEXT=i686
  ;;
x64)
  ARCHEXT=x86_64
  ;;
a32)
  ARCHEXT=armv7hl
  ;;
a64)
  ARCHEXT=aarch64
  ;;
*)
  echo Invalid arch!
  exit
  ;;
esac

for i in *; do
  if [ -d $i ]; then
    build $i $ARCH $ARCHEXT
  fi
done
