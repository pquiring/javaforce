jpbxlite/0.24
=============

Welcome to jpbxlite.
Java VoIP/SIP PBX.
Supports:
  - extensions
  - voicemail
  - trunks
  - IVR
  - conferences (video supported)
  - queues (ACD)

Notes
=====
 - not recommended for production environments (minimal testing, highly experimental)
 - uses embedded database (derby)
 - includes an embedded web server for configuration
 - trunks should use IP authentication, registration method is still untested.
 - all config changes are instant and don't require a reload (except for the General Settings page)
 - some of the shown config options are not implemented yet
 - your server SHOULD NOT be behind a NATing firewall
 - supports video relaying (VP8/H.263+/H.264/JPEG)

Windows
=======
Recommend using MSI package, and launch with start menu shortcut.
Otherwise use install.bat to place files into required folders and run %ProgramFiles%\jpbxlite\run.bat

Linux
=====
Using jfLinux.org : install the deb/rpm packages and use 'jservice jpbxlite start'.
Otherwise use install.sh to place files into required folders and run /usr/bin/jpbxlite to start the server.

Web Based Configuration
=======================
When the server is running, it provides a web server running on port 80 (and secure 443).
Point your browser to http://127.0.0.1
The Admin button is in the top right corner (kinda hidden).
Default login is admin:admin
From the admin console you can create trunks, extensions, etc.
It is strongly recommended to set the "Local IP" in the general settings page.

First time installation
=======================
When you run jPBXlite the first time the database will be blank which means no plugins get loaded.
Log into the admin console and click on the "Database not init" message.
This will populate the database, including the plugins and menus.
Now "restart" the server and everything should be working.

WebRTC
======
A few simple WebRTC demos are now available on the home page of the PBX.
Basically just copied the source from apprtc.appspot.com
The demo offers conference rooms for two members only.
The PBX only exchanges SDP offers/answers.
Hope to add more functionality in the future, such as registration, invites, etc.
*NEW* WebRTC can now call a standard VoIP phone thru the PBX (only audio seems to work).
Works with Firefox.  Chrome still has issues.
All WebRTC support is highly experimental.

Secure Web Server
=================
In upcoming releases a secure web server may become required, to support WebRTC.
See the General Settings page to automatically generate a self-signed key.
To manually generate the self-signed key using keytool run:
  keytool -genkey -keyalg RSA -alias jpbxlite -keystore jpbx.key -storepass password -validity 3650 -keysize 2048 -dname "CN=jpbxlite.sourceforge.net, OU=jpbxlite, O=JavaForce, C=CA"
For Windows the keystore is stored in %ProgramData%\jpbxlite
For Linux the keystore should be stored in /etc
Restart the server and the Web Server will start on the secure port.
To create a key for signing by a cert auth, refer to keytool for help.

TODO
====
 - more trunk options
 - call recording
 - referLive (non-blind transfers)
 - fault tolerance (ie: resend requests if ACK not received)  (udp transmission is not guaranted)

Enjoy!

by : Peter Quiring (pquiring at gmail dot com)

http://jpbxlite.sourceforge.net
