#!/bin/bash

#This script runs inside the chroot folder

echo Executing in chroot.sh !!!

mount none -t proc /proc
mount none -t sysfs /sys
mount none -t devpts /dev/pts

#stop apt-get from prompting for input
ORGTERM=$TERM
unset TERM
export DEBIAN_FRONTEND=noninteractive

cp /etc/lsb-release /tmp
chmod +x /tmp/lsb-release
. /tmp/lsb-release
rm /tmp/lsb-release
RELEASE=$DISTRIB_CODENAME
VERSION=$DISTRIB_RELEASE

#create fake NetworkManager so casper doesn't create /etc/network/interfaces (doesn't work)
#if [ ! -x /usr/sbin/NetworkManager ]; then
#  ln -s /usr/bin/yes /usr/sbin/NetworkManager
#fi

export HOME=/root
export LC_ALL=C
#Substitute "12345678" with the PPA's OpenPGP ID. (???)
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 12345678
apt-get update
apt-get install --yes dbus
dbus-uuidgen > /var/lib/dbus/machine-id

#work around for bug : https://bugs.launchpad.net/ubuntu/+source/upstart/+bug/430224
#effectively disables upstart in the chroot (otherwise packages fail to install/upgrade)
dpkg-divert --local --rename --add /sbin/initctl
ln -s /bin/true /sbin/initctl

#install some common tools
apt-get install --yes wget unzip

#download JavaForce Repo file
cd /etc/apt/sources.list.d
if [ ! -f javaforce.list ]; then
  echo Download javaforce.list
  wget http://javaforce.sf.net/ubuntu/javaforce.list
fi
cd /etc/apt/trusted.gpg.d
if [ ! -f javaforce.gpg ]; then
  echo Download javaforce.gpg
  wget http://javaforce.sf.net/ubuntu/javaforce.gpg
fi
cd /

#this is needed for add-apt-repository
apt-get install --yes software-properties-common

#add wine repo (ubuntu is always out-dated)
add-apt-repository ppa:ubuntu-wine/ppa

#allow i386 packages for wine
dpkg --add-architecture i386

#add google-chrome repo
wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list

#add darling repo
add-apt-repository ppa:thopiekar/darling
add-apt-repository ppa:thopiekar/gnustep

#update repo
apt-get --yes update
apt-get --yes upgrade

#install chrome
apt-get install --yes google-chrome-stable

#install ffmpeg
apt-get install --yes libav-tools

#install wine
apt-get install --yes wine1.7

#install kernel
apt-get install --yes linux-generic

#install network tools
apt-get install --yes --no-install-recommends wireless-tools

#casper is needed to boot CD-ROM - it will generate a new initramfs
apt-get install --yes casper lupin-casper

#fix casper - this file tries to create a generic /etc/network/interfaces which SLOWS the boot process
rm -f /usr/share/initramfs-tools/scripts/casper-bottom/23networking
#redo initramfs
update-initramfs -u

#install other packages

apt-get install --yes man-db cups psmisc
#gnome-keyring -- still needed???
#libreoffice -- removed to save space -- can install later in japps

#create some special folders (is done in packages - but just in case)
mkdir -p /etc/jinit
mkdir -p /usr/share/jhelp

#install Java packages

apt-get install --yes --no-install-recommends jlogon jdesktop jterm jtxtedit jpaint jinstall jrepo jwelcome jfile jmedia jarchive jparted jview jconfig plymouth-theme-jflinux jcapture japps jremote jupgrade jtaskmgr jcalc

#create /etc/.live
#user is the live username
echo user=ubuntu > /etc/.live
#filesToCopy is for the installer
echo filesToCopy=0 >> /etc/.live
echo casper=true >> /etc/.live

#create motd for terminals
echo Welcome to jfLinux > /etc/motd
echo Please visit http://jflinux.org >> /etc/motd

#run bash for inspection
echo Running bash in chroot for final inspection, exit when done.
bash

#cleanup chroot

rm /var/lib/dbus/machine-id
rm /run/dbus/*

#undo work around for bug : https://bugs.launchpad.net/ubuntu/+source/upstart/+bug/430224
rm /sbin/initctl
dpkg-divert --rename --remove /sbin/initctl

#cleanup old kernel (why?)
#ls /boot/vmlinuz-* > list.txt
#sum=$(cat list.txt | grep '[^ ]' | wc -l)
#if [ $sum -gt 1 ]; then
#dpkg -l 'linux-*' | sed '/^ii/!d;/'"$(uname -r | sed "s/\(.*\)-\([^0-9]\+\)/\1/")"'/d;s/^[^ ]* [^ ]* \([^ ]*\).*/\1/;/[0-9]/!d' | xargs sudo apt-get -y purge
#fi
#rm list.txt

#final cleanup

apt-get clean

#reset TERM
export TERM=$ORGTERM

rm -rf /tmp/*

rm /etc/resolv.conf

umount -lf /proc
umount -lf /sys
umount -lf /dev/pts
exit
