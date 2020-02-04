To compile:

- install cygwin.com (install gcc)
  - required just to create ffmpeg headers
- download ffmpeg and run "bash configure --disable-x86asm"
  - requires gcc to be present
  - you do not need to make it, only the headers are needed
- install MSVC
  - to compile native library
- ant

Required environment variables:

- JAVA_HOME = JDK home path
- FFMPEG_HOME = FFMPEG home path
