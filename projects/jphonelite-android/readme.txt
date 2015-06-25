jphonelite-droid
================

Welcome to jphonelite for Android.
Converting my Java SIP Phone to Android only took a few weeks.

Features : 6 lines with conference (up to all 6 lines), transfer, hold, dnd, g711a/u, RFC 2833.
g729a is included but disabled by default since it needs a very fast CPU.

Enjoy!

Compiling
---------
Edit local.properties and make sure sdk.dir matches your Android SDK path.
Run 'ant -f sign.xml genkeys'
Run 'ant -f sign.xml depjars'
Run 'ant compile' or 'ant install' or 'ant release'.

'ant -f sign.xml sign-apk' is no longer needed.

Debugging
---------
Run 'adb logcat JPL:I *:S' to view just log output from JPL.
Other channels : JPLMAIN, JPLSOUND, JPLENGINE

http://jphonelite.sourceforge.net
Peter Quiring (pquiring at gmail dot com)
