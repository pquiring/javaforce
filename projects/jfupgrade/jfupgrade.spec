Buildroot: /.
Name: jfupgrade
Version: 0.6
Release: 1
Summary: Java Upgrade Manager
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Java Upgrade Manager

%files
