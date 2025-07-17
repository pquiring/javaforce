VNC Viewer:

 - jfvnc.exe : vnc client

VNC Server:

 - jfvncsvc.exe : service based server which runs in the background
   - jfvncsession.exe : used internally by service for each connection
   - supports Windows and Linux (X11)
 - jfvnccli.exe : app based server which runs as a user app with limited functionality
   - useful in logon scripts, etc.
 - config file : jfvncserver.cfg
   - windows location : %ProgramData% (usually C:\ProgramData)
   - linux location : /etc
   password=password   #must be 8 chars long
   port=5900           #port to listen on (default = 5900)
   web=true            #enable VNC Web access
   webport=5800        #port for web access (http)
   websecureport=5843  #port for web access (https)
   user=username       #user to grab Xauthority from (Linux X11 only)
   display=:0          #display (Linux X11 only)
