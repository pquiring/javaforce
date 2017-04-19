To compile:

  arch = x32 | x64 | a32 | a64

Debian/Ubuntu:

  Required packages:
    ant deb

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

  Compile:
    ant <arch>

Fedora:

  Note : You will need to setup rpmfusion.org to get access to ffmpeg

  Required packages:
    ant rpm

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java

  Compile:
    ant <arch>

Arch:

  Required packages :
    ant pac

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk

  Compile:
    ant <arch>

Raspberry Pi (Debian):

  Same packages as Debian above.

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-armhf

  Compile:
    ant <arch>
