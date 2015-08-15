Buildroot: /.
Name: jfftp
Version: 0.7
Release: 1
Summary: FTP Manager
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

FTP Manager

%files
