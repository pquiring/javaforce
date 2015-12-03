Buildroot: .
Name: jfdhcp
Version: 0.2
Release: 1
Summary: DHCP Server
License: LGPL
Distribution: Fedora
Group: Applications/System
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0
%description
DHCP Server
%files
