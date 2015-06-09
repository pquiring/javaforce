Buildroot: /.
Name: jflogon
Version: 0.1
Release: 1
Summary: Java Startup and Logon
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, xorg-x11-server-Xorg
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%post
#! /bin/sh

set -e

chkconfig --add jflogond

%preun
#!/bin/sh

set -e

if [ -x "/etc/init.d/jflogond" ]; then
#  service jflogond stop || exit $?
fi

%postun
#!/bin/sh -e

chkconfig --del jflogond

%description

Java Startup and Logon Process

%files
