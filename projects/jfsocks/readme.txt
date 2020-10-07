SOCKS4/4a/5 Server
==================

Socks4/4a/5 server with SSL Support.
Includes a sample client that redirects a local port thru the SOCKS server to a remote host/port.
Great for connecting to insecure services behind a firewall (such as RDP).

Supports:
 - IP4
 - domain names
 - Secure SSL
 - SOCKS5 auth type 0x02 only (plain text:therefore use secure mode if using authentication)

Not supported:
 - IP6

Notes:
 - if using socks5 authentication you should disable socks4 which doesn't require authorization.

WebSite: http://jfsocks.sourceforge.net

Source : http://javaforce.sourceforge.net
