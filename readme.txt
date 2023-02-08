JavaForce SDK
=============

Version 37.2

What is it?
===========
JavaForce is a Java library extending the capabilities of Java.

The main library includes a VoIP stack and bindings for FFMPEG, OpenGL and Camera.

Custom native launchers for Console/GUI apps and system services.

Tasks to package apps (msi, deb, rpm, etc.)

Includes many apps built around the library such as jfPhone, jfVideo Creator, jfAudio, jfMusic, etc.

JF is also the core library in the Java infused Linux Operating system : http://jfLinux.sourceforge.net

JF is also used to create jfCraft, a Minecraft clone : http://pquiring.github.io/jfcraft

Projects
========
jfPhone - VoIP soft phone

jfVideo - video production

jfAudio - audio editor

jfPaint - multi-tabbed image editor

jfDVR - Records IP/RTSP/H264 cameras with motion detection

jfBackup - Enterprise tape backup system

and many more...

Folders
=======
 /          - main folder (run ant here to build /src)
 /src       - the javaforce source files
 /jars      - 3rd party files
 /stubs     - native launcher stubs
 /native    - native library with JNI bindings for FFMPEG, OpenGL, Camera
 /classes   - javaforce compiled files
 /projects  - source for all sub-projects

Building
========
All projects are built with Apache Ant (http://ant.apache.org).
Make sure to run ant in the main folder to build the /src folder and then in any of the apps in /projects.
Do not run ant in a sudo environment, some environment variables such as HOSTTYPE are missing.

Common Ant tasks:
-----------------
compile : compile projects java files
jar : build the projects main jar file
depjars : copy dependant jar files into project folder
executable : build native launcher
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
  - jre pre-linked for native packaging (see below)
  - windows stub created (/stubs/windows)
dmg : build Mac dmg file using hdiutil (mac only)
 - dmg creation requires:
  - jre prep for native packaging (see below)
  - Info.plist, ${app}.icns and macfiles.lst
  - see projects/jfedit or projects/jfpaint for only examples
  - jfimageconvert can convert images to .icns file format (mac icons)
  - mac stub created (/stubs/mac)
javadoc : create javadoc api help files (open ./javadoc/index.html)

JavaForce Ant tasks:
--------------------
ffmpeg-win64 : Download ffmpeg libraries for Win64
ffmpeg-mac64 : Download ffmpeg libraries for Mac64
jre-base : pre-link JRE for creating native packages (msi, dmg)
jre-base-desktop : pre-link JRE with desktop support
jre-base-javac : pre-link JRE with java compiler support

Building native library (ffmpeg, OpenGL, Camera)
------------------------------------------------
Native Library is in /native
See readme.txt in each platform folder for more info.
  - you can run 'ant get-bin' to download pre-built binaries for Win64

Building native launchers
-------------------------
Native Launchers are in /stubs
They require the native library be build first.
Windows:Requires Visual C++ in your PATH.
  - you can run 'ant get-bin' to download pre-built binaries for Win64
Linux:Debian/Ubuntu:run 'ant deb' to install required packages.
Linux:RedHat/Fedora:run 'ant rpm' to install required packages.
  - you can run 'ant get-bin-x86' or 'ant get-bin-arm' to download pre-build binaries for Linux64

Graal Support
-------------
JavaForce includes some support for building AOT executables using GraalVM (http://www.graalvm.org)
To build a project using Graal you need to add a property to the build.xml
  <property name="graal" value="true"/>
Ant Tasks:
  graal : build Graal native Library
  graalagent : execute application to build native configuration files (sometimes not necessary)
The ant task 'executable' will generate an executable that will look for the Graal Library before looking for a JVM.
Library name should be the full class name where main() is defined.
  ie: javaforce.utils.CopyPath.dll
GraalVM support for AWT is still unfinished and probably will never be.

Requirements
------------

  - JDK 17+
  - Apache Ant 1.9.8+
  - Windows : VisualC++ compiler (64bit), wixtoolset v3
  - FFMpeg 4.2+ sources(headers) + shared libraries
  - glfw for OpenGL support

License
=======
JavaForce itself is licensed under the LGPL license which can be read in license.txt.
The MSI installers show the Common Public License 1.0 which is acceptable as well.
The other jars in /jars each have their own licensing:
  filters.jar - Apache License 2.0 (http://www.jhlabs.com)
  bouncycastle.jar - MIT license? (http://www.bouncycastle.org)
  derby.jar - Apache License 2.0 (http://db.apache.org/derby)
  jcifs.jar - LGPL (http://jcifs.samba.org)
  jsp-api.jar & servlet-api.jar - Apache License 2.0 (http://tomcat.apache.org)
  jsch.jar & jzlib.jar - BSD license (http://www.jcraft.com)
  llrp.jar - Apache License 2.0 (http://llrp.org)
Other components:
  wixtoolset v3 - Microsoft Reciprocal License (MS-RL) (http://wixtoolset.org)
  FFMpeg - various
  glfw - zlib/libpng (http://github.com/glfw/glfw)

Enjoy!

Peter Quiring
pquiring@gmail.com

Web : pquiring.github.io/javaforce

Git : github.com/pquiring/javaforce
