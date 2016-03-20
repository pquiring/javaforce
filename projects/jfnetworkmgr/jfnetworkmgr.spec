Buildroot: .
Name: jfnetworkmgr
Version: 0.1
Release: 1
Summary: Network Manager
License: LGPL
Distribution: Fedora
Group: Applications/System
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0
%description
Network Manager
%files
