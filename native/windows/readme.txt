Windows Loader
==============

Desc
----

  - These C++ sources build the Windows loader and native API used by JavaForce
  - java.exe is never used to run Java apps within JavaForce
  - the native API provides features that are currently unavailable in Java


Requirements:
-------------
  - MS Visual C++ compiler (Visual Studio : Full or Build Tools)
    - Run vcvars64.bat to setup build environment
      - there is a shortcut in the start menu under Visual Studio called "Developer Command Prompt"
  - Java and Ant in your path
  - ffmpeg headers

Compiling using pre-built ffmpeg headers/libraries
--------------------------------------------------

  cd ../..
  ant get-ffmpeg-win64-src
  ant get-ffmpeg-win64-bin
  cd native/windows
  ant

  - where ${ffmpeg-version} can be found in ../../versions.xml
  - downloads are available for 5.1.2 , 6.1.2 and 7.0.2 (you must change ..\..\versions.xml)

Compiling using ffmpeg downloaded manually
------------------------------------------

  - you'll need to prepare ffmpeg headers and libraries
  - install cygwin.com (install gcc)
    - required just to create ffmpeg headers
  - download ffmpeg sources to ../../ffmpeg-src/${ffmpeg-version}
    - cd ../../ffmpeg-src/${ffmpeg-version}
    - run "bash configure --disable-x86asm"
    - cygwin must be in path
    - you do not need to make it, only the headers are needed
  - run ant
  - you'll need to find shared libraries from ffmpeg.org and extract to ../../ffmpeg-bin/${ffmpeg-version}
  - I don't recommend trying to compile ffmpeg yourself
  - update ffmpeg-version in ../../versions.xml to match
  - JavaForce currently requires ffmpeg/5.1 or higher

Downloading pre-built binaries
------------------------------
  If you have problems compiling you can always download pre-built binaries hosted on sourceforge:

    ant get-bin

  You still need to download ffmpeg libraries

    cd ../..
    ant get-ffmpeg-win64-bin

Notes
-----
  - apps MUST use the EXACT same MAJOR version of ffmpeg you build against
