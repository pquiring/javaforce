jfLinux
=======

Desc:
  This folder contains scripts to install the JavaForce Linux (jfLinux) package repos.
  jfLinux is a complete desktop environment (DE) using the JavaForce library and related apps.
  The installation script will install the package repo and optionally install the DE.
  Installing the DE is not required to use other JF apps.

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
  wget http://pquiring.github.io/javaforce/linux/install.sh
  chmod +x install.sh
  ./install.sh

Follow prompts to install the JF Desktop Environment if desired.

install.sh parameters
---------------------
  --desktop=yes|no|ask : install Desktop Environment (default = ask)
  --javaforce=yes|no : install JavaForce core package (default = yes)
  --update=yes|no : re-download gpg key (default=no)

Currently supported
-------------------
  debian : amd64 arm64 (includes Ubuntu)
  fedora : amd64 arm64
  arch : amd64
Full details are here : https://pquiring.github.io/repo/javaforce-status.txt

key expired
-----------
For systems that had the JavaForce repos added prior to version 79.0
the key used to sign packages hosted on sourceforge.net expired on 2025-2-6.
Debian users will need to re-run install.sh with --update=yes to re-download the new key.
Fedora users should automatically update since the key is refereced in the javaforce.repo file.
Arch users may need to re-run install.sh with --update=yes
The new key will expire in 50 years (2075).

Note about Arch installation
----------------------------
  Arch Linux has a very simple installation guide or a script to automate the task which I recommend using.
  The installation script will throw an exception unless you prep the keyring.
  To install from the script run these commands:
    pacman-key --init
    pacman-key --populate archlinux
    archinstall
  Within the install program make sure to select destination drive, drive partitioning, select minimal profile, set root password,
  create user and setup networking manually.

Thanks!
