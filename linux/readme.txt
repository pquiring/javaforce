jfLinux
=======

Desc:
  jfLinux is a complete desktop environment (DE) using the JavaForce library and related apps.

Status:
  Currently the DE is based on X11.  There are plans to implement Wayland support but that may have to wait
until Java itself supports Wayland which is in development, see Wakefield project : https://openjdk.org/projects/wakefield/

Instructions
------------

To install on a minimal Linux system follow these steps:

1) Install minimal linux system
  Debian netinst (uncheck any desktop environment during package selection)
  Fedora Server netinst
  Arch minimal Profile
2) From command prompt type:
  wget https://raw.githubusercontent.com/pquiring/javaforce/master/linux/install.sh
  chmod +x install.sh
  ./install.sh

Follow prompts to install the JF Desktop Environment if desired.

Currently supported:
  debian : amd64
  fedora : amd64
  arch : amd64

Note about Arch installation:
  Arch Linux has a very simple installation guide or a script to automate the task which I recommend using.
  The installation script will throw an exception unless you prep the keyring.
  To install from the script run these commands:
    pacman-key --init
    pacman-key --populate archlinux
    archinstall
  Within the install program make sure to select destination drive, drive partitioning, select minimal profile, set root password,
  create user and setup networking manually.

Thanks!
