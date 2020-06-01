Debian/Ubuntu:

  Required packages:
    ant deb

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

  Compile:
    ant linux

Fedora:

  Note : You will need to setup rpmfusion.org to get access to ffmpeg

  Required packages:
    ant rpm

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java

  Compile:
    ant linux

Arch:

  Required packages :
    ant pac

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk

  Compile:
    ant linux

FreeBSD:

  Required packages :
    ant pkg

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk

  Compile:
    ant freebsd

Raspberry Pi (Debian) (32bit deprecated):

  Same packages as Debian above.

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-armhf

  Compile:
    ant arm32

Raspberry Pi (Debian) (64bit):

  Same packages as Debian above.

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-arm64

  Compile:
    ant arm64

