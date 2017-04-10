function build {
  if [ "$1" == "jphonelite-android" ]; then
    return
  fi
  if [ "$1" == "jfrdp" ]; then
    return
  fi
  cd $1
  ant jar
  sudo ant install -Dbits=$2
  ant deb -Darch=$3 -Darchext=$4
  cd ..
}

if [ "$1" == "" ]; then
  echo usage : buildAllUbuntu.sh {ARCH}
  echo where ARCH=x32,x64,a32,a64
  exit
fi

ARCH=$2
BITS=${ARCH:1:2}

case $2 in
x32)
  ARCHEXT=i386
  ;;
x64)
  ARCHEXT=amd64
  ;;
a32)
  ARCHEXT=armhf
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
    build $i $BITS $ARCH $ARCHEXT
  fi
done
