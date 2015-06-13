To compile:

- install cygwin.com
- install mingw 32/64 bit compiler packages
- download ffmpeg and run ./configure in cygwin/bash
  - requires gcc to be present
  - include --disable-yasm if you don't have yasm installed
  - you do not need to make it, only the headers are needed

Required environment variables:

- JAVA_HOME = JDK home path
- FFMPEG_HOME = FFMPEG home path
