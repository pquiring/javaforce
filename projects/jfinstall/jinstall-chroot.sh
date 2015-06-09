#!/bin/bash

CHROOT=$1

shift

if [ "$CHROOT" == "" ]; then
  echo usage : jfinstall-chroot.sh folder cmd [args...]
  exit 1
fi

mount --bind /dev $CHROOT/dev
mount --bind /proc $CHROOT/proc
mount --bind /sys $CHROOT/sys

sudo chroot $CHROOT $*

ERROR=$?

umount $CHROOT/dev
umount $CHROOT/proc
umount $CHROOT/sys

exit $ERROR
