Buildroot: /.
Name: jfvnc
Version: 0.1
Release: 1
Summary: Java VNC Client
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

Java VNC Client

%files
