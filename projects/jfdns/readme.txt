DNS Server
==========

A simple DNS Server:
 - supports A,CNAME,MX,AAAA

Sample config:

[global]
uplink=8.8.8.8
allow=.*[.]google[.]com
deny=*
[records]
mydomain.com,cname,3600,www.mydomain.com
www.mydomain.com,a,3600,192.168.0.2

The allow/deny are optional to control which domains are allow to be passed up to uplink dns.
Denied names are sent to example.com
Recommended allow domains for Windows Network Connection check:
    https://blogs.technet.microsoft.com/networking/2012/12/20/the-network-connection-status-icon/
    www[.]msftncsi[.]com
Recommended allow domains for Windows Update:
    https://technet.microsoft.com/cs-cz/library/bb693717.aspx

WebSite: http://jfdns.sourceforge.net

Source : http://javaforce.sourceforge.net
