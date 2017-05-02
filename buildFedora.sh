function build {
  ant jar
  sudo ant install -Dbits=$1
  ant rpm -Dbits=$1 -Darch=$2 -Darchext=$3
}

if [ "$1" == "" ]; then
  echo usage : buildFedora.sh {ARCH}
  echo where ARCH=x32,x64,a32,a64
  exit
fi

ARCH=$1
BITS=${ARCH:1:2}

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

build $BITS $ARCH $ARCHEXT
