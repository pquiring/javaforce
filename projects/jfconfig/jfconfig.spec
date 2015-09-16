Buildroot: /.
Name: jfconfig
Version: 0.12
Release: 1
Summary: Control Center
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, jparted, pciutils
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Control Center

%files
