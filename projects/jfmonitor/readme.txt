jfMonitor
=========

Enterprise Network / Storage Monitoring Solution

Features:
  - Network Monitoring using ARP requests (PING requests can get blocked)
  - Storage Monitoring
  - email notification

Not Supported (yet):
  - unknown device notification

Web Interface:
  - http://your_host_name

License:
  - LGPL
  - No warranty of any kind is given.  Not liable for data loss or corruption.  Use at your own risk.

Usage:
  - jfMonitor is a multi-client / server setup
  - each client will report file system usage to the server
  - any client (or server) can also ping any locally connected network which requires pcap installed.

FAQ:
  - Q: Can I ping a remote network?
  - A: No, the MAC address is used to identify devices and ARP requests are used instead of actual PING requests
       since a PING can be blocked but ARP requests are generally not blocked.
       This is why the network interface IP address is required in the network monitoring setup section.

WebSite : http://jfmonitor.sf.net

Source Code : https://github.com/pquiring/javaforce/tree/master/projects/jfmonitor

Author : Peter Quiring
eMail : pquiring at gmail dot com

Version : 0.2
Date : 4/25/2022