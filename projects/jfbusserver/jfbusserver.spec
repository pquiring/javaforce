Buildroot: .
Name: jfbusserver
Version: 0.1
Release: 1
Summary: Bus Server
License: LGPL
Distribution: Fedora
Group: Applications/System
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0
%description
Bus Server
%files
