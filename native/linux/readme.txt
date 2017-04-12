To compile:

  Must compile on a 64bit host (both 32/64bit images are created).

Ubuntu:

  Required packages :
    sudo apt-get install g++ openjdk-8-jdk ant libx11-dev libfuse-dev libpam0g-dev libavcodec-dev libavformat-dev libavutil-dev libswscale-dev mesa-common-dev libxcursor-dev libxrandr-dev libxinerama-dev libxi-dev libxt-dev

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

  Run 'ant'

Fedora:

  Required packages :
    sudo yum install gcc-c++ java-1.8.0-openjdk-devel ant libX11-devel fuse-devel pam-devel ffmpeg-devel mesa-libGL-devel libXcursor-devel libXrandr-devel libXinerama-devel libXi-devel libXt-devel

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java

  Run 'ant'

Arch:

  Required packages :
    sudo pacman -S jdk8-openjdk apache-ant fuse pam ffmpeg mesa xproto libxcursor libxrandr libxinerama libxi libxt

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk

  Run 'ant'

Raspberry Pi (Ubuntu):

  Same packages as Ubuntu above.

  Required environment variable:
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-armhf

  Run 'ant pi'

  Only the 32bit image is created.
