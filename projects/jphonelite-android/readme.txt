jphonelite-android
==================

Welcome to jphonelite for Android.

Features : 6 lines with conference (up to all 6 lines), transfer, hold, dnd, g711a/u, g722, RFC 2833.
g729a is included but disabled by default since it needs a very fast CPU.

Enjoy!

Compiling
---------
Edit local.properties and make sure sdk.dir matches your Android SDK path.
Run 'ant -f tools.xml genkeys'
Run 'ant -f tools.xml depjars'
Run 'ant compile' or 'ant install' or 'ant release'.

Debugging
---------
Run 'adb logcat JAVAFORCE:I *:S' to view just log output from JPL.

http://jphonelite.sourceforge.net
Peter Quiring (pquiring at gmail dot com)
