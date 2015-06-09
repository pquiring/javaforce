Buildroot: /.
Name: javaforce
Version: 9.0.0
Release: 1
Summary: JavaForce Core Library
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: java-1.7.0-openjdk, jsch, jzlib, jcifs, fuse, fuse-libs, fuseiso, libcdio

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

JavaForce Core Library

%post
#!/bin/sh

set -e

if [ $1 = "1" ]; then
  update-alternatives --install /usr/bin/update-desktop-database update-desktop-database /usr/bin/jf-update-desktop-database 100
fi

%pre
#!/bin/sh

mkdir -p /usr/share/jhelp

%preun
#!/bin/sh

if [ $1 = "0" ]; then
  update-alternatives --remove update-desktop-database /usr/bin/jf-update-desktop-database
fi

%files
