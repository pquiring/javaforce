Buildroot: .
Name: jfnettools
Version: 0.1
Release: 1
Summary: Network Admin Tools
License: LGPL
Distribution: Fedora
Group: Applications/System
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0
%description
Network Admin Tools
%files
