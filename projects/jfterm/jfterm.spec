Buildroot: /.
Name: jfterm
Version: 0.18
Release: 1
Summary: Java Terminal Emulator
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, jsch, jzlib, openssh-server
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Java Terminal Emulator

%files
