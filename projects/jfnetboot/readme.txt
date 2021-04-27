jfNetBoot
=========

jfNetBoot is a PXE service for network booting thin clients.
Currently supports Raspberry PI 4 and amd64 systems.
Server runs on any Debian based amd64 system (recommend a VM)

Provides :
  - TFTP server for the clients to load the boot system.
  - NFS server for the root file system.
  - Each client has persistent file caching on server.

Each client's connection to the NFS server maintains a cache of changed (written) files.
This allows unlimited clients to run off of one NFS file system.

Go to http://jfnetboot.sf.net/help.php for installation help.

Paths:
------
/var/netboot/filesystems = filesystems
  /*/boot = boot images (TFTP)
  /*/root = NFS file systems
/var/netboot/clients = client write cache filesystems

Known issues
------------
 - Currently a blank SD card must be left in the Pi or you get warnings that popup every 10 seconds (and flood the system logs).
   see : https://www.raspberrypi.org/forums/viewtopic.php?t=276264
 - the RPi kernel panics a lot during boot up (usually when it first connects to the RPC service and queries ports)
   reboot, reboot, reboot (always reboot 3 times just to be sure)
   it's a buggy broadcom ethernet driver in linux (always seeing bcmgenet_rx_poll in the trace log)
	 until this is resolved this project is mostly useless
 - The config.sh scripts are not compatible with Ubuntu (use Debian only)
 - The DHCP in PXE Proxy mode is not working for me.  Could be hardware issue, your milage may vary. Recommend putting PXE options into a full DHCP server.
   Note : The DHCP server must be enabled to track clients.  Use the proxy option if needed.

FAQ:
----
Q : What is the client ID?
A : The MAC address of the client.

Q : Is the Pi3 supported?
A : Probably if it's firmware is updated, but it only has a 100MB NIC so performance would be poor.  The Pi4 has a gigabit NIC.

Q : Could I run the server on a Raspberry Pi?
A : Probably (untested), but you wouldn't be able to support x86 clients (unless you used qemu just as the arm64 filesystem is done on an amd64 system).
    And you would need to build from source, only binaries for amd64 are released.
