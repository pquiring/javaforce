JavaForce SDK
=============

Version 54.1

Description
===========
JavaForce (JF) is a Java library featuring:

  - SIP/VoIP/RTSP
  - native bindings for FFMPEG, OpenGL and Camera
  - PLC I/O
  - custom native launchers for console apps, desktop apps and system services
  - tasks to package apps (msi, deb, rpm, etc.)

Includes many apps built around the library such as jfPhone, jfVideo Creator, jfAudio, jfMusic, etc.
  - see projects/readme.txt for complete list

JF is also the core library in the Java infused Linux Operating system:
  https://github.com/pquiring/javaforce/tree/master/linux

JF is also used to create jfCraft, a Minecraft clone:
  http://pquiring.github.io/jfcraft

Folders
=======
 /          - main folder (run ant here to build /src)
 /src       - the javaforce source files
 /lib       - generated and dependancies jar files
 /native    - native loaders with JNI bindings for FFMPEG, OpenGL, Camera
 /projects  - source for all sub-projects
 /utils     - utility apps (jnlp)

Getting Started
===============
First get some tools installed, all projects are built with Apache Ant (http://ant.apache.org):
  debian:apt install git ant
  fedora:yum install git ant
  windows:install git, OpenJDK and ant from various sources (msys2, cygwin, oracle, etc.)
Before checking out the source make sure to configure line endings:
  linux/mac : git config --global core.autocrlf input
  windows : git config --global core.autocrlf true
Checkout Javaforce:
  git clone http://github.com/pquiring/javaforce
  cd javaforce
  ant
Next build the native launchers.

Building native loaders with native API (ffmpeg, OpenGL, Camera)
----------------------------------------------------------------
Native loaders are in /native
JavaForce does not use java.exe, instead a custom loader is built, which includes additional native API using JNI.
See readme.txt in each platform folder for more info.
Windows:Requires Visual C++ in your PATH.
  - you can run 'ant get-bin' to download pre-built binaries for Win64
Linux:Debian/Ubuntu:run 'ant deb' to install required devel packages.
Linux:RedHat/Fedora:run 'ant rpm' to install required devel packages.
Linux:Arch:run 'ant pac' to install required devel packages.
Linux:FreeBSD:run 'ant pkg' to install required devel packages.

Building jfLinux
----------------
After building Javaforce and the native components you can run package.sh to package everything.
Supported distros : Debian, Fedora, Arch.
FreeBSD is currently not supported (open issue if you would like to see FreeBSD packager task and repo created)

JavaForce Ant tasks:
--------------------
get-ffmpeg-win64-bin : Download ffmpeg libraries for Win64
get-ffmpeg-win64-src : Download ffmpeg sources for Win64 (to build native loader)
jre-base : pre-link JRE for creating native packages (msi, dmg)
jre-base-desktop : pre-link JRE with desktop support
jre-base-javac : pre-link JRE with java compiler support

Common Ant tasks:
-----------------
compile : compile projects java files
jar : build the projects main jar file
depjars : copy dependant jar files into project folder
javadoc : build javadoc api files
deploy : build maven deployment artifacts
executable : build native launcher
 - a stub launcher for the platform is copied into the project folder and configured to load classpath and start main method
 - a project property "apptype" can be defined as:
     "window" for Window GUI apps (default)
     "console" for console apps (alias "c")
     "service" for service apps (alias "s")
     "server" for service config GUI apps (same as "window" type plus adds "-server" to executable and package names)
     "client" for client config GUI apps (same as "window" type plus adds "-client" to executable and package names)
ffmpeg : copy ffmpeg libraries to project folder
installapp : install files before package creation (Linux only)
deb : build Debian deb file (after install)
 - requires bzip2, binutils, sudo
 - linux packaging requires files.lst
rpm : build Fedora rpm file (after install)
 - requires rpm-build
 - linux packaging requires files.lst
pac : build Arch pac file (after install)
 - linux packaging requires files.lst
msi : build Windows msi file with JRE bundled
 - msi creation requires:
  - wixtoolset v3 in path (http://wixtoolset.org)
  - wix64.xml file
  - jre pre-linked for native packaging (see above)
dmg : build Mac dmg file using hdiutil (mac only)
 - dmg creation requires:
  - jre prep for native packaging (see above)
  - Info.plist, ${app}.icns and macfiles.lst
  - see projects/jfedit or projects/jfpaint for only examples
  - jfimageconvert can convert images to .icns file format (mac icons)

Maven
-----
The Maven repo is used to download dependancies using ant tasks.
The mvn tool is currently not required.
A pom.xml is supplied but is missing dependancies for now, the minimum is included
to create deployment artifacts.

Graal Support
-------------
JavaForce includes some support for building AOT executables using GraalVM (http://www.graalvm.org)
To build a project using Graal use the following Ant Tasks.
  graalagent
    - execute application to build native configuration files
    - run through the application making sure to use all features and then exit
    - these config files are stored in META-INF/native-image and must be included in the application jar file
  graal
    - builds application native Library (should run after agent has generated config files)
    - JavaForce uses graal to build a library instead of an executable so the javaforce launchers can still be used
    - the library generated by graal includes a standard JVM interface
    - library name should be the full class name where main() is defined (ie: javaforce.utils.CopyPath.dll)
GraalVM support for AWT is still a work in progress.

Debugging
---------
The native loaders in /bin enable JMX debugging support on port 9010.
These loaders are used when you run an app with "ant run".
From VisualVM you can connect to the JMX as localhost:9010

Requirements
------------
  - JDK 17+
  - Apache Ant 1.10+
  - Linux : gcc
  - Windows : Visual C++ build tools, wixtoolset v3, AMD64
  - Mac : AMD64
  - FFMpeg 5.1+ sources(headers) + shared libraries
  - glfw for OpenGL support

License
=======
JavaForce is licensed under the LGPL license which can be read in license.txt.
The MSI installers show the Common Public License 1.0 which is acceptable as well.
All third party libraries and tools each have their own licenses.

Enjoy!

Peter Quiring
pquiring@gmail.com

Web : pquiring.github.io/javaforce

Git : github.com/pquiring/javaforce
