JavaForce SDK
=============

Version 9.3.0

What is it?
===========
JavaForce is a Java library extending the capabilities of Java.

The main library includes a VoIP stack and bindings for FFMPEG, OpenGL, etc.

Includes many apps built around the library such as jPhoneLite, jfVideo Createor, jfAudio, jfMusic, etc.

JF is also the core library in the Java infused Linux Operating system : http://jfLinux.sourceforge.net

JF is also used to create jfCraft, a Minecraft clone : http://jfcraft.sourceforge.net

Projects
========
jPhoneLite - Java VoIP/SIP soft phone
  6 lines, g711, g729a, Xfer, Hold, Conference, contact list, recent list, RFC 2833, etc.

jfVideo Creator - video production

jfPaint - a multi-tabbed image editor

jPBXlite - Java VoIP/SIP PBX
  - extensions, trunks, voicemail, IVRs, conferences

jfBroadcast - VoIP/SIP Auto Dialer System

jfTerm - a great Telnet client application that includes support for:
  Telnet/ANSI (full color).
  SSH (X11) (using JCraft JSch)
  SSL
  Multi-tabbed.
  Copy/Paste.
  Logging.

jfEdit - a multi-tabbed text editor.

jfHex - a multi-tabbed hex editor.

and many more...

Folders
=======
 /          - main folder (run ant here to build /src)
 /src       - the javaforce source files
 /jars      - 3rd party files
 /classes   - javaforce compiled files
 /projects  - source for all sub-projects

Classpath
=========
No special CLASSPATH is required except maybe "." to run applications of course.

Building
========
All projects are built with Apache Ant (available at http://ant.apache.org).
Make sure to run ant in the main folder to build the /src folder and then in any of the apps in /projects.
If building on Windows make sure to copy /native/*.dll to /windwos/system32 since some of the
build tools require them.

Common Ant tasks:
-----------------
compile : compile projects java files
jar : build the projects main jar file
depjars : copy dependant jar files into project folder
install : install files into proper folders (Linux only : "sudo ant install")
deb : build Ubuntu deb file (after install)
rpm : build Fedora rpm file (after install)
msi32 : build Windows msi file (32bit)
msi64 : build Windows msi file (64bit)
dmg : build Mac dmg file (mac only)
genisodmg : build Mac dmg file (cygwin/linux/mac) (uncompressed)
javadoc : create javadoc api help files (open ./javadoc/index.html)

3rd party
=========
All third party dependancies are in /jars

License
=======
JavaForce itself is licensed under the LGPL license which can be read in license.txt.
The MSI installers show the Common Public License 1.0 which is acceptable as well.
The other jars in /jars may each have their own licensing.
  filters.jar - Apache License 2.0 (jhlabs.com)
  bouncycastle.jar - MIT license? (www.bouncycastle.org)
  derby.jar - Apache License 2.0 (db.apache.com/derby)
  jcifs.jar - LGPL (jcifs.samba.org)
  jsp-api.jar & servlet-api.jar - Apache License 2.0 (tomcat.apache.com)
  jsch.jar & jzlib.jar - BSD license (www.jcraft.com)

Enjoy!

Peter Quiring
pquiring@gmail.com

http://javaforce.sourceforge.net

Version 9.3.0

Released : July 8 2015

