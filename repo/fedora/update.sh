#!/bin/bash
cp ../../data/rpmmacros ~/.rpmmacros
gpg --export -a > RPM-GPG-KEY-javaforce
rpm --resign *.rpm
createrepo .
