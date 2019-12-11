To compile:

- install cygwin.com (install gcc)
  - required just to create ffmpeg headers
- download ffmpeg and run ./configure in cygwin/bash
  - requires gcc to be present
  - include --disable-yasm if you don't have yasm installed
  - you do not need to make it, only the headers are needed
- install MSVC
  - to compile native library
- ant

Required environment variables:

- JAVA_HOME = JDK home path
- FFMPEG_HOME = FFMPEG home path
