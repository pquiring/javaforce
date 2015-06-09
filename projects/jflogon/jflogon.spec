Buildroot: /.
Name: jflogon
Version: 0.18
Release: 1
Summary: Java Startup and Logon for jfLinux
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, xorg-x11-server-Xorg, plymouth, bluez, pm-utils, samba-winbind-clients, samba-client, wpa_supplicant, numlockx, dosfstools, ntfsprogs
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

Java Startup and Logon Process for jfLinux

%post

if [ $1 -eq 1 ] ; then
  # Initial installation
  /usr/bin/systemctl preset jflogon.service >/dev/null 2>&1 || :
fi

%preun

if [ $1 -eq 0 ] ; then
  # Package removal, not upgrade
  /usr/bin/systemctl --no-reload disable jflogon.service > /dev/null 2>&1 || :
  /usr/bin/systemctl stop jflogon.service > /dev/null 2>&1 || :
fi

%postun

/usr/bin/systemctl daemon-reload >/dev/null 2>&1 || :

%files
