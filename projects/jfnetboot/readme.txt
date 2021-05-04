jfNetBoot
=========

jfNetBoot is a PXE service for network booting thin clients.
Currently supports Raspberry PI 4 and amd64 systems.
Server runs on any Debian based amd64 system (recommend a VM)

Provides :
  - TFTP server for the clients to load the boot system.
  - Each client has persistent file caching on server (mount overlays)

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
 - The config.sh scripts are not compatible with Ubuntu (use Debian only)

FAQ:
----
Q : What is the client ID?
A : The MAC address of the client.

Q : Is the Pi3 supported?
A : Probably if it's firmware is updated, but it only has a 100MB NIC so performance would be poor.  The Pi4 has a gigabit NIC.

Q : Could I run the server on a Raspberry Pi?
A : Probably (untested), but you wouldn't be able to support x86 clients (unless you used qemu just as the arm64 filesystem is done on an amd64 system).
    And you would need to build from source, only binaries for amd64 are released.
