Buildroot: /.
Name: jphonelite
Version: 1.9.4
Release: 1
Summary: Java VoIP/SIP Phone
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

Java VoIP/SIP Phone

%files
