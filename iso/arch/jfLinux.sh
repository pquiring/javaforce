#!/bin/bash

sudo pacman -S git make arch-install-scripts squashfs-tools libisoburn dosfstools lynx --needed

git clone https://projects.archlinux.org/archiso.git/

sudo make -C archiso install

mkdir -p ~/archlive/out

cp -r /usr/share/archiso/configs/releng/* ~/archlive

#customize begin

cat pacman.conf >> ~/archlive/pacman.conf
cat packages >> ~/archlive/packages.both

#customize end

cd ~/archlive
sudo ./build.sh -v

echo Complete!
