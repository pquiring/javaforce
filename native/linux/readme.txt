To compile:

Must compile on a 64bit host (both 32/64bit images are created).

Ubuntu:

Required packages :
sudo apt-get install g++ openjdk-8-jdk ant libx11-dev libfuse-dev libpam0g-dev libavcodec-dev libavformat-dev libavutil-dev libswscale-dev mesa-common-dev libxcursor-dev libxrandr-dev libxinerama-dev libxi-dev

To build the 32bit image:
sudo apt-get install libc6-dev:i386 libx11-dev:i386 libxrandr-dev:i386 libxinerama-dev:i386 libxi-dev:i386 libxcursor-dev:i386 libpam0g-dev:i386

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

Fedora:

Required packages :
sudo yum install gcc-c++ java-1.8.0-openjdk-devel ant libX11-dev fuse-devel pam-devel ffmpeg-devel mesa-libGL-devel libXcursor-devel libXrandr-devel libXinerama-devel libXi-devel libXt-devel

export JAVA_HOME=/usr/lib/jvm/java

