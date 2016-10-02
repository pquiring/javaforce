#!/bin/bash
for f in *.pkg.tar.xz; do
  gpg --detach-sign --no-armor $f
done
repo-add --verify --sign javaforce.db.tar.gz *.pkg.tar.xz
