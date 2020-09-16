SOCKS4/4a/5 Server
==================

Socks4/4a/5 server with a sample client that redirects a local port thru the SOCKS server.
Supports making server secure with SSL.
Great for connecting to insecure services behind a firewall (such as RDP).

Server supports:
 - IP4
 - domain names
 - Secure SSL
 - SOCKS5 auth type 0x02 only (plain text:therefore use secure mode if using authentication)

Server does not support:
 - IP6

Redirect Client supports:
 - IP4
 - Secure SSL

Redirect Client does not support:
 - IP6
 - domain names (mostly used to connect to IP's behind the firewall)

WebSite: http://jfsocks.sourceforge.net

Source : http://javaforce.sourceforge.net
