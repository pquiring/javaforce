#!/bin/bash
echo Building Fedora based jfLinux image \(approx 20 mins\)...
echo start = `date` > time.txt
sudo livecd-creator -c jfLinux.ks -f jfLinux --title=jfLinux --product=jfLinux --cache=/var/cache/live
sudo chown $USER:$USER *.iso
echo end = `date` >> time.txt
cat time.txt
