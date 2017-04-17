#!/bin/bash

apt --yes install dpkg-dev

rm *.gz 2>/dev/null
rm InRelease 2>/dev/null
rm Release 2>/dev/null
rm Release.gpg 2>/dev/null

dpkg-scanpackages . > Packages

apt-ftparchive release . > TopRelease
mv TopRelease Release

gpg --clearsign -o InRelease Release
gpg -abs -o Release.gpg Release

#gzip InRelease
#gzip Release
#gzip Release.gpg
#gzip Packages

if [ ! -f javaforce.gpg ]; then
  cp ~/.gnupg/pubring.gpg ./javaforce.gpg
  chmod 644 javaforce.gpg
fi
