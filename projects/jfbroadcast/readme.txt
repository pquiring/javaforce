jfBroadcast
===========

AutoDialer system written in Java using JavaForce SDK.
Dials each number in the list broadcasting your pre-recorded message and/or taking a survey.
The listener can be transfered to another number at anytime by pressed the transfer digit (default = 0).

Steps
-----

 1) Define your SIP parameters (auth is optional and defaults to user) (if pass is blank it will use non-registered mode).
 2) Click on "Create List..." (define any name next to it such as dates or names)
 3) Select your list.
 4) Click on "Import List..." (files must be phone number lists - one per line)
 5) Set the # of lines to use (make sure you have enough bandwidth, about 90KiloBits per line for g711 or 40KiloBits per line for g729a)
 6) Define your messages and survey options.
 7) Click "Save Setup" for later use.
 8) Click "Start!"

Notes
-----
WAV files must be 8000hz, 16bit, mono in PCM format (no compression).
Uses an embedded database server which will automatically create itself the first time you run the program.
If you see a lot of 'system too slow : skipCount = x' messages you can either reduce # of lines or try disabling g729a.
The list of numbers is currently not editable.
If your WAV file(s) are too large you may get OutOfMemory exceptions,
  run java.exe with the -Xmx256m option to increase available memory (or make the change in your Java control panel:Java->View->Runtime Parameters).
    java.exe -Xmx256m -cp javaforce.jar;derby.jar;jfbroadcast.jar Broadcast
Anwsering machine detection is accomplished by starting after 1 second of silence after something is heard (thresholds and duration are now adjustable).
You can adjust the delay(ms) that is used between each attempt to start a new call (to prevent flooding your SIP server).
You can use "Fix List..." to change status from "calling" to "new" if you forced program to exit while calling.
Messages should be at least 5 seconds long.
The database was changed in v0.15 so you should export your lists in an older version and import with this version.
The database location and config files location were changed in v0.18:
  Windows : stored in %ProgramData%\jfBroadcast (or %AllUsersProfile%\jfBroadcast for Win2000/XP/2003)
  Unix : stored in /var/lib/jfBroadcast
  If you are upgrading the old location was ./database which you can manually move and rename.
  The config files (broadcast.cfg and broadcast-msgs.cfg) are now stored in the user profile folder (%UserProfile% for Windows)
Please only broadcast to your own customers.  Laws in some countries/states may prohibit contacting unsolicited individuals.

Surveys
-------
Each message can record a response, either a single digit or long responses (which are stored in brackets).
The last message should give thanks and terminate the call.
The system will scan from 1 to 99 for enabled questions, play the message and wait for a response as defined.
The goto action will jump to another message based on what digit the user presses.  A zero disables that digit.
Responses are stored in the 'Survey' column after the call is completed.

Running the app
---------------
For Windows recommend using MSI package and run shortcut in start menu.
Otherwise use run.bat(Windows) or run.sh(Linux) to run the program from the command line.

Command Line Options
--------------------
-start <LIST>
  Starts a list as soon as the program starts.  Useful for starting jfBroadcast at a certain time.
  Example : -start default
-dbpath <PATH>
  Sets the path where the database is stored. (Default depends on OS)
  Example : -dbpath c:\folder
-cfgsuffix <SUFFIX>
  Adds SUFFIX (a simple name) to the config filenames used. (Default = broadcast.cfg and broadcast-msgs.cfg)
  Example : -cfgsuffix i1    (will force broadcasti1.cfg and broadcast-msgsi1.cfg to be used)
-allowdups
  Allows duplicate numbers in lists.  Duplicates will have a comma and number added to the number to make it unique.
  The number after the comma will not actually be dialed, it's used just to track the duplicates.

Note : With -dbpath and -cfgsuffix it is possible to run multiple copies on the same system as long as you
       configure each instance to use different SIP Port ranges that do not overlap.

Donote
------
Please Donate if you find this program useful.
Send $5 US or more via PayPal to pquiring@gmail.com
Thanks!

Author
------
Peter Quiring (pquiring at gmail dot com)

WebSite
-------
http://jfbroadcast.sourceforge.net

