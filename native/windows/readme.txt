To compile:

- install cygwin.com (install gcc)
  - required just to create ffmpeg headers
- download ffmpeg and run "bash configure --disable-x86asm"
  - requires gcc to be present
  - you do not need to make it, only the headers are needed
  - you MUST use the same version you run against
- install MSVC
  - to compile native library
- ant

Required environment variables:

- FFMPEG_HOME = FFMPEG home path
