#!/bin/bash

#
# Create jfLinux from scratch
#
# NOTE:Make sure to change ARCH to amd64 for 64bit systems
# Valid options are : i386 and amd64
# i686 (HOSTTYPE) is not valid
#
# SEE: https://help.ubuntu.com/community/LiveCDCustomizationFromScratch
#

ARCH=amd64

cp /etc/lsb-release /tmp
chmod +x /tmp/lsb-release
. /tmp/lsb-release
rm /tmp/lsb-release
RELEASE=$DISTRIB_CODENAME
VERSION=$DISTRIB_RELEASE

IMAGE_LABEL="jfLinux \(u$VERSION\) $ARCH"

if [ ! -x /usr/bin/jfr ]; then
  echo Please install JavaForce!
  exit
fi

jfr RELEASE $RELEASE default.sources.list

if [ ! -x /usr/sbin/debootstrap ]; then
  sudo apt-get install --yes debootstrap
fi
if [ ! -x /usr/bin/mksquashfs ]; then
  sudo apt-get install --yes squashfs-tools
fi

mkdir -p ~/.work/chroot
sudo mkdir -p ~/.work/chroot/etc/sudoers.d
sudo mkdir -p ~/.work/chroot/etc/jconfig.d

sudo cp chroot.sh ~/.work/chroot
cp isolinux.cfg ~/.work
cp default.sources.list ~/.work
cp /etc/apt/sources.list ~/.work
cp interfaces ~/.work
cp nsswitch.conf ~/.work
cp hosts ~/.work
cp hostname ~/.work
cp network.xml ~/.work
cp resolv.conf ~/.work

cd ~/.work

if [ ! -f chroot/bin/bash ]; then
  sudo debootstrap --arch=$ARCH $RELEASE chroot
fi

MAKEBACKUP=false
if [ "$MAKEBACKUP" = "true" ]; then
  #Note:copy ~/.work to a backup so you only have to do debootstrap once
  echo Make a backup of ~/.work \(use sudo - some files are owned by root\)
  exit
fi

#these are temp - fixed later
sudo cp /etc/hosts chroot/etc/hosts
sudo cp /etc/resolv.conf chroot/etc/resolv.conf
#NOTE:If your repo is not up to date you may have problems. If that occurs
#     copy default.sources.list instead.  The kernel can cause problems.
sudo cp sources.list chroot/etc/apt/sources.list

sudo mount --bind /dev chroot/dev
sudo mount --bind /proc chroot/proc
sudo mount --bind /sys chroot/sys

#run 'chroot' on folder 'chroot' with script '/chroot.sh'
sudo chroot chroot /chroot.sh

sudo umount chroot/dev
sudo umount chroot/proc
sudo umount chroot/sys

#patch some files
sudo cp -f interfaces chroot/etc/network
sudo cp -f nsswitch.conf chroot/etc
sudo cp -f hosts chroot/etc
sudo cp -f network.xml chroot/etc/jconfig.d
sudo cp -f hostname chroot/etc
sudo cp -f resolv.conf chroot/etc

sudo rm -f chroot/chroot.sh

sudo umount -lf chroot/dev

#reset back to default sources after chroot
sudo cp default.sources.list chroot/etc/apt/sources.list

#install needed cd iso creation tools

sudo apt-get install --yes isolinux squashfs-tools genisoimage
#sbm???

mkdir -p image/{casper,isolinux,install}

for file in chroot/boot/vmlinuz-*; do sudo cp $file image/casper/vmlinuz; done

for file in chroot/boot/initrd.img-*; do sudo cp $file image/casper/initrd.gz; done

cp /usr/lib/ISOLINUX/isolinux.bin image/isolinux/
cp /usr/lib/syslinux/modules/bios/ldlinux.c32 image/isolinux/

cp /boot/memtest86+.bin image/install/memtest
cp /boot/sbm.img image/install/

echo jfLinux >image/isolinux/isolinux.txt

cp isolinux.cfg image/isolinux/isolinux.cfg

#create CD manifest

sudo chroot chroot dpkg-query -W --showformat='${Package} ${Version}\n' | sudo tee image/casper/filesystem.manifest
sudo cp -v image/casper/filesystem.manifest image/casper/filesystem.manifest-desktop
REMOVE='casper user-setup discover1 xresprobe os-prober'
for i in $REMOVE
do
        sudo sed -i "/${i}/d" image/casper/filesystem.manifest-desktop
done

#fix permissions/ownership

sudo chmod 0644 image/casper/*
sudo chown $USER:$USER image/casper/*

#compress chroot

if [ -f image/casper/filesystem.squashfs ]; then
  sudo rm image/casper/filesystem.squashfs
fi
sudo mksquashfs chroot image/casper/filesystem.squashfs

#write filesystem.size

printf $(sudo du -sx --block-size=1 chroot | cut -f1) > image/casper/filesystem.size

#calc MD5

cd image
find . -type f -print0 | xargs -0 md5sum | grep -v "\./md5sum.txt" > md5sum.txt
cd ..

#create the image

if [ -f jflinux.iso ]; then
  sudo rm -f jflinux.iso
fi

cd image
sudo mkisofs -r -V "$IMAGE_LABEL" -cache-inodes -J -l -b isolinux/isolinux.bin -c isolinux/boot.cat -no-emul-boot -boot-load-size 4 -boot-info-table -o ../jflinux.iso .
cd ..
