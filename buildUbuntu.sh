function build {
  ant jar
  sudo ant install -Dbits=$1
  ant deb -Darch=$2 -Darchext=$3
}

if [ "$1" == "" ]; then
  echo usage : buildUbuntu.sh {ARCH}
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

build $BITS $ARCH $ARCHEXT
