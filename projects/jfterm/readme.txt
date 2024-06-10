Terminal client

Supports:
  Telnet/ANSI codes (full color)
  UTF8 or ASCII8
  SSH
  SSH Key Authentication (leave password blank)
  X11 Forwarding
  SSL
  Com Ports (Windows/Linux only)
  local shell (bash) via pty (Linux only)
  Multi-Tabbed
  Scroll Back buffer
  Copy/Paste
  Scripts

Notes:
  In the Site Manager you can click on a folder and
    click on Connect and it will open a tab for
    each site.
  Keep the scroll back buffer small (under 100) if
    you plan on opening a lot of tabs or you'll
    run out of memory.
  The signed applet stores your settings locally
    just like the full app and is safe to use.
  For com ports:
    - 8bit data, 1bit stop, no parity is assumed
    - for Windows : host = com#,baud (if com port is > 9 then host = \\.\com##,baud)
    - for Linux : host = /dev/ttyS#,baud (baud=9600->115200) (you may need to add user to 'dialout' group to access port)
  Sometimes the font metrics are not accurate.
    In the settings you can adjust the font size (Width=X,Height=Y,Descent=Base)

Scripting support:
  Language syntax:
    #comments
    Type "text"
    Wait "text"
    Sleep seconds
    HitKey VK_key
  Example:
    Type "username"
    HitKey VK_ENTER
    Wait ">"
    Type "password"
    HitKey VK_ENTER
  Some VK_...: VK_ENTER, VK_TAB, VK_SPACE, etc.

Linux support:
  Recommend you install with deb or rpm package.
  Otherwise you should install from source.
  The jar files MUST be placed in /usr/share/java so jfTerm can execute bash properly.

Author : Peter Quiring

URL : http://jfterm.sourceforge.net

Part of the JavaForce SDK : http://javaforce.sourceforge.net
