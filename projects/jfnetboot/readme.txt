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

Configuring DHCP:
-----------------
Option 66 is required to specify the TFTP Server (jfNetBoot).
Option 67 is ignored for RPi clients but MUST be 'boot/pxelinux.0' for x86 clients.
jfDHCP is a DHCP service that supports custom options if needed.
jfNetBoot currently does not provide a DHCP service.

Configuring a Pi4 for network booting:
--------------------------------------
By default the Pi4 EEPROM settings do not include Network Booting and must be enabled.
Download the Raspberry OS Lite and install onto SD Card.
Boot Pi from SD Card.
Login as 'pi' (password:raspberry)
Run 'sudo bash'
Run 'rpi-eeprom-config > boot.conf'
Run 'vi boot.conf'
Change:
  BOOT_ORDER=0xf41
    to
  BOOT_ORDER=0xf21
Save boot.conf (ESC:wq)
Run 'rpi-eepromp-config --apply boot.conf'
Reboot to apply eeprom settings.
Shutdown and remove SD Card.
See : https://www.raspberrypi.org/documentation/hardware/raspberrypi/bcm2711_bootloader_config.md
Hopefully future Pi's will include eeprom editing like those found in a PC BIOS.

Paths:
------
/var/netboot/filesystems = filesystems
  /*/boot = boot images (TFTP)
  /*/root = NFS file systems
/var/netboot/clients = client write cache filesystems

Setup default filesystem for ARM clients:
-----------------------------------------
Download Debian RPi4 Image to /var/netboot/filesystems/default-arm
see https://raspi.debian.net/daily-images/
Then type these commands:
  apt install p7zip-full
  cd /var/netboot/filesystems/default-arm
  7z x debian_?.xz
  7z x debian_?
  cd boot
  7z x ../0.fat
  cd /etc
  echo /var/netboot/filesystems/default-arm/1.img /var/netboot/filesystems/default-arm/root ext4 ro 0 0 >> fstab
  reboot

Setup default filesystem for x86 clients:
-----------------------------------------
Type these commands on the server:
  apt install pxelinux syslinux-common debootstrap
  cd /var/netboot/filesystems/default-x86
  mkdir boot
  mkdir root
  cp /usr/lib/PXELINUX/pxelinux.0 boot
  cp /usr/lib/syslinux/modules/bios/* boot
  cp /boot/initrd.img-*-amd64 boot/initrd.img
  cp /boot/vmlinuz-*-amd64 boot/vmlinuz
  cd root
  debootstrap buster .
  cp /etc/passwd etc
  cp /etc/group etc
  cp /etc/shadow etc
  chroot .
  passwd -d root
  exit

Preparing first client:
-----------------------
After your first client boots up, login as root and run /netboot/config.sh:
  cd /netboot
  . ./config.sh
This script will automate the installation of required packages,config them and setup other boot up scripts.
Create a normal user called 'user'
  adduser user
Then reboot and shutdown the client.
Now from the console you can clone the client's file system (while it's shutdown) so that other clients can use the same file system.
This allows for build once, use many.

Setup files
-----------
/netboot/config.sh - installs packages, configures them and installs autostart
/netboot/autostart - openbox autostart script (executes /netboot/command.sh)
/netboot/command.sh - client specifc file (see Commands in web console)
/netboot/default.html - default HTML content to display if no command is configured

Known issues
------------
 - Currently a blank SD card must be left in the Pi or you get warnings that popup every 10 seconds (and flood the system logs).
   see : https://www.raspberrypi.org/forums/viewtopic.php?t=276264
 - the RPi kernel panics a lot during boot up (usually when it first connects to the RPC service and queries ports)
   reboot, reboot, reboot (always reboot 3 times just to be sure)
   it's a buggy broadcom ethernet driver in linux (always seeing bcmgenet_rx_poll in the trace log)
	 until this is resolved this project is mostly useless
 - The config.sh scripts are not compatible with Ubuntu (use Debian only)

FAQ:
----
Q : What is the client ID?
A : The Raspberry PI provides a unique 32bit ID during TFTP downloads.
    For x86 clients the MAC address is used (lower 32bits only) This means x86 clients MUST be on the same subnet as the server.

Q : Is the Pi3 supported?
A : Probably if it's firmware is updated, but it only has a 100MB NIC so performance would be poor.  The Pi4 has a gigabit NIC.

Q : Could I run the server on a Raspberry Pi?
A : Probably (untested), but you wouldn't be able to support x86 clients (unless you copied the files from a running x86 system).
    And you would need to build from source, only binaries for amd64 are released.
