#!/bin/bash
dnf -y install createrepo rpm-sign
cp rpmmacros ~/.rpmmacros
gpg --export -a > RPM-GPG-KEY-javaforce
rpm --resign *.rpm
createrepo .
