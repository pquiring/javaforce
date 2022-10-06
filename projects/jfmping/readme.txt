jfmping
=======

Name : Multi Ping Test Tool

Desc : Using ARP requests to continously (ping) a list of IP addresses and
  display results in a chart.  Quickly see if a device goes offline for
  a period of time.

Notes :
  - requires npcap installed : https://npcap.com
  - requires two files:
    ip-config.txt
      iface_ip=1.2.3.4   #interface IP to send ARP requires from (ping)
      delay=1000         #delay (ms) between processing list
    ip-list.txt
      5.6.7.8            #list of IP addresses to ping (one per line)
  - command line running:
    java -cp javaforce.jar;jfmping.jar Main

Version 0.1
