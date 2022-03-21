Debian/Ubuntu:

  Required packages:
    ant deb

  Compile:
    ant linux

Fedora:

  Note : You will need to setup rpmfusion.org to get access to ffmpeg

  Required packages:
    ant rpm

  Compile:
    ant linux

Arch:

  Required packages :
    ant pac

  Compile:
    ant linux

FreeBSD:

  Required packages :
    ant pkg

  Compile:
    ant freebsd

Raspberry Pi (Debian) (32bit deprecated):

  Same packages as Debian above.

  Compile:
    ant arm32

Raspberry Pi (Debian) (64bit):

  Same packages as Debian above.

  Compile:
    ant arm64

get-bin-*:
  These will download binaries for latest Debian release.
