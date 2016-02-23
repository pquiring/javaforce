Buildroot: /.
Name: jphonelite
Version: 1.9.11
Release: 1
Summary: Java VoIP/SIP Phone
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Java VoIP/SIP Phone

%files
