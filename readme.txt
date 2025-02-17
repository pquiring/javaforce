JavaForce SDK
=============

Version 79.1

Description
===========
JavaForce (JF) is a Java library featuring:

  - SIP/VoIP/RTSP
  - native bindings for FFmpeg, OpenGL, OpenCL and Camera
  - PLC I/O
  - custom native loaders for console apps, desktop apps and system services
  - tasks to package apps (msi, deb, rpm, etc.)

Includes many apps built around the library such as jfPhone, jfVideo Creator, jfAudio, jfMusic, etc.
  - see projects/readme.txt for complete list

JF is also the core library in jfLinux, a Java infused Linux Operating system:
  https://github.com/pquiring/javaforce/tree/master/linux

JF is also used to create jfCraft, a Minecraft clone:
  https://github.com/pquiring/jfcraft

Folders
=======
 /          - main folder (run ant here to build /src)
 /src       - the javaforce source files
 /lib       - generated and dependancies jar files
 /native    - native loaders that load the java system and classpath, register native methods and start program main method
 /projects  - source for all sub-projects
 /iso       - scripts to build jfLinux iso (outdated)
 /linux     - scripts to install JavaForce repo for Linux
 /windows   - scripts to install JavaForce repo for Windows
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
Checkout Javaforce and a specific version:
  git clone http://github.com/pquiring/javaforce
  cd javaforce
  git checkout tags/79.1
  ant
Next build the native loaders.

Building native loaders with native API (FFmpeg, OpenGL, OpenCL, Camera)
------------------------------------------------------------------------
Native loaders are in /native
JavaForce does not use java.exe, instead a custom loader is built, which includes additional native API using JNI.
The native methods are also placed in a shared library in case you prefer not to use the custom loaders.
See readme.txt in each platform folder for more info.

Building jfLinux (optional)
----------------
After building Javaforce and the native components you can run package.sh to build and package everything for a Linux repo.
Supported distros : Debian, Fedora, Arch.
FreeBSD is currently not supported (open issue if you would like to see FreeBSD packager task and repo created)
All packages are stored in /repo and can then be processed and uploaded to a repo server.
Pre-built packages are hosted on sourceforge.net which can be installed through the jfLinux install process (see /linux).

JavaForce Ant tasks:
--------------------
get-ffmpeg-win64-bin : Download ffmpeg libraries for Win64
get-ffmpeg-win64-src : Download ffmpeg sources for Win64 (to build native loader)
  ffmpeg-win64 versions 5.1.2 , 6.1.2 , 7.0.2 are now available (you must edit versions.xml)
jre-all : pre-link JRE with all modules
jre-base : pre-link JRE with minimal modules for console apps/services
jre-base-desktop : pre-link JRE with desktop support
jre-base-javac : pre-link JRE with java compiler support

Common Ant tasks:
-----------------
compile : compile projects java files
jar : build the projects main jar file
depjars : copy dependant jar files into project folder
run : execute program from command line (with debugging support enabled)
javadoc : build javadoc api files
deploy : build maven deployment artifacts (requires pom.xml) and publish to sonatype
  - must define 'maven' property which is groupId
  - auth token must be defined in ~/.m2/settings.xml
executable : build native loader
 - a stub loader for the platform is copied into the project folder and configured to load classpath and start main method
 - a project property "apptype" can be defined as:
     "window" for Window GUI apps (default)
     "console" for console apps (alias "c")
     "service" for service apps (alias "s")
     "server" for service config GUI apps (same as "window" type plus adds "-server" to executable and package names)
     "client" for client config GUI apps (same as "window" type plus adds "-client" to executable and package names)
ffmpeg : copy ffmpeg libraries to project folder (Windows only)
installapp : install files before package creation (Linux only)
deb : build Debian deb file (after installapp)
 - requires bzip2, binutils, sudo
 - linux packaging requires files.lst
rpm : build Fedora rpm file (after installapp)
 - requires rpm-build
 - linux packaging requires files.lst
pac : build Arch pac file (after installapp)
 - linux packaging requires files.lst
msi : build Windows msi file with JRE bundled
 - msi creation requires:
  - wixtoolset v4+ in path (http://wixtoolset.org)
  - install wix tool (requires dotnet 6+)
    dotnet tool install --global wix
  - wix64.xml file
  - jre pre-linked for native packaging (see jre-* tasks above)
  - install wix extensions:
    wix extension add -g WixToolset.UI.wixext
    wix extension add -g WixToolset.Firewall.wixext
    wix extension add -g WixToolset.Util.wixext
  - older wix3 files must be upgraded:
    wix convert wix64.xml
dmg : build Mac dmg file using hdiutil (mac only)
 - dmg creation requires:
  - jre pre-linked for native packaging (see jre-* tasks above)
  - Info.plist, ${app}.icns and macfiles.lst
  - see projects/jfedit or projects/jfpaint for only examples
  - jfimageconvert can convert images to .icns file format (mac icons)

Maven
-----
JavaForce is available in Maven Central : io/github/com/pquiring/javaforce
Ant tasks are used to download dependancies and upload projects to Maven Central.
The mvn tool is not required (way too complex).
A minimal pom.xml is required to upload projects to Maven Central (sonatype).

Graal Support
-------------
JavaForce includes some support for building AOT executables using GraalVM (http://www.graalvm.org)
To build a project using Graal use the following Ant Tasks.
  graalagent
    - execute application and monitor JNI usage to build native configuration files
    - run through the application making sure to use all features and then exit
    - GRAALARGS can be configured in app cfg file for console apps to force required program flow to be monitored
  graal
    - builds application native Library (should run after agent has generated config files)
    - JavaForce uses graal to build a library instead of an executable so the javaforce loaders can still be used
    - the library generated by graal includes a standard JVM interface
    - library name should be the full class name where main() is defined (ie: javaforce.utils.CopyPath.dll)
GraalVM support for AWT is still a work in progress.

Debugging
---------
The native loaders in /bin enable JMX debugging support on port 9010.
These loaders are used when you run an app with "ant run".
From VisualVM you can connect to the JMX as localhost:9010
You can also enable debug support by adding DEBUG=true to the project .cfg file to have it enabled in the generated executable.
Also try adding -Xlog:gc*:gc.log to the OPTIONS= in the project .cfg file.  Then while the app is running use 'tail -f gc.log' from a terminal to watch memory usage.
This should be removed for any public release.

Requirements
------------
  - JDK 17+
  - Apache Ant 1.10+
  - Linux : gcc
  - Windows : VC++, wixtoolset 4+
  - Mac : gcc (Xcode)
  - FFmpeg 5.1+
  - glfw 3.4 (OpenGL support)

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
