Buildroot: /.
Name: jfremote
Version: 0.1
Release: 1
Summary: Java Remote Desktop Manager
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, rdesktop, jfvnc
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Java Remote Desktop Manager

%files
