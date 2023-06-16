JavaForce SDK
=============

Version 40.0

Description
===========
JavaForce is a Java library featuring:

  - VoIP stack
  - native bindings for FFMPEG, OpenGL and Camera
  - PLC I/O
  - custom native launchers for console apps, desktop apps and system services
  - tasks to package apps (msi, deb, rpm, etc.)

Includes many apps built around the library such as jfPhone, jfVideo Creator, jfAudio, jfMusic, etc.

JF is also the core library in the Java infused Linux Operating system : https://github.com/pquiring/javaforce/tree/master/linux

JF is also used to create jfCraft, a Minecraft clone : http://pquiring.github.io/jfcraft

Projects
========
jfPhone - VoIP soft phone

jfVideo - video production

jfAudio - audio editor

jfPaint - multi-tabbed image editor

jfDVR - Records IP/RTSP/H264 cameras with motion detection

jfBackup - Enterprise tape backup system

and many more... (see projects/readme.txt for complete list)

Folders
=======
 /          - main folder (run ant here to build /src)
 /src       - the javaforce source files
 /lib       - generated and dependancies jar files
 /stubs     - native launcher stubs
 /native    - native library with JNI bindings for FFMPEG, OpenGL, Camera
 /classes   - javaforce compiled files
 /projects  - source for all sub-projects

Getting Started
===============
First get some tools installed, all projects are built with Apache Ant (http://ant.apache.org):
  debian:apt install git ant
  fedora:yum install git ant
  windows:install git, OpenJDK and ant from various sources (msys2, cygwin, oracle, etc.)
Checkout Javaforce:
  git clone http://github.com/pquiring/javaforce
  cd javaforce
  ant
Next build the native library and the native launchers.

Building native library (ffmpeg, OpenGL, Camera)
------------------------------------------------
Native Library is in /native
See readme.txt in each platform folder for more info.
Windows:Requires Visual C++ in your PATH.
  - you can run 'ant get-bin' to download pre-built binaries for Win64
Linux:Debian/Ubuntu:run 'ant deb' to install required devel packages.
Linux:RedHat/Fedora:run 'ant rpm' to install required devel packages.
Linux:Arch:run 'ant pac' to install required devel packages.
Linux:FreeBSD:run 'ant pkg' to install required devel packages.

Building native launchers
-------------------------
Native Launchers are in /stubs
They require the native library be build first.
  - you can run 'ant get-bin' to download pre-built binaries for Win64

Building jfLinux
----------------
After building Javaforce, native library and stubs (launchers) you can run build.sh to build and package everything.
Supported distros : Debian, Fedora, Arch.
FreeBSD is currently not supported (open issue if you would like to see FreeBSD packager task and repo created)

JavaForce Ant tasks:
--------------------
get-ffmpeg-win64 : Download ffmpeg libraries for Win64
get-ffmpeg-mac64 : Download ffmpeg libraries for Mac64
jre-base : pre-link JRE for creating native packages (msi, dmg)
jre-base-desktop : pre-link JRE with desktop support
jre-base-javac : pre-link JRE with java compiler support
jnlp : build JNLP launcher (Java8)

Common Ant tasks:
-----------------
compile : compile projects java files
jar : build the projects main jar file
depjars : copy dependant jar files into project folder
javadoc : build javadoc api files
deploy : build maven deployment artifacts
executable : build native launcher
 - a stub launcher for the platform is copied into the project folder and configured to load classpath and start main method
 - a project property "apptype" can be set to "c" for console apps or "s" for service apps (default is gui app)
ffmpeg : copy ffmpeg libraries to project folder
deb : build Debian deb file (after install)
 - requires bzip2, binutils, sudo
 - linux packaging requires files.lst and linux stub (/stubs/linux)
rpm : build Fedora rpm file (after install)
 - requires rpm-build
 - linux packaging requires files.lst and linux stub (/stubs/linux)
pac : build Arch pac file (after install)
 - linux packaging requires files.lst and linux stub (/stubs/linux)
msi : build Windows msi file with JRE bundled
 - msi creation requires:
  - wixtoolset v3 in path (http://wixtoolset.org)
  - wix64.xml file
  - jre pre-linked for native packaging (see above)
  - windows stub created (/stubs/windows)
dmg : build Mac dmg file using hdiutil (mac only)
 - dmg creation requires:
  - jre prep for native packaging (see above)
  - Info.plist, ${app}.icns and macfiles.lst
  - see projects/jfedit or projects/jfpaint for only examples
  - jfimageconvert can convert images to .icns file format (mac icons)
  - mac stub created (/stubs/mac)

Maven
-----
The Maven repo is used to download dependancies using ant tasks.
The mvn tool is currently not required.
A pom.xml is supplied but is missing dependancies for now, the minimum is included
to create deployment artifacts.

Graal Support
-------------
JavaForce includes some support for building AOT executables using GraalVM (http://www.graalvm.org)
To build a project using Graal you need to add a property to the build.xml
  <property name="graal" value="true"/>
Ant Tasks:
  graal
    - build Graal native Library
  graalagent
    - execute application to build native configuration files
    - these config files are stored in META-INF/native-image and must be included in the application jar file
The ant task 'executable' will generate an executable that will look for the Graal Library before looking for a JVM.
Library name should be the full class name where main() is defined.
  ie: javaforce.utils.CopyPath.dll
GraalVM support for AWT is still a work in progress.

Requirements
------------

  - JDK 11+
  - Apache Ant 1.10.x
  - Windows : VisualC++ compiler (64bit), wixtoolset v3
  - FFMpeg 4.2+ sources(headers) + shared libraries
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
