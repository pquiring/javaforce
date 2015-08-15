Buildroot: .
Name: jfrpm
Version: 1.0
Release: 1
Summary: Builds RPM packages
License: LGPL
Distribution: Fedora
Group: Applications/System
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0
%description
Builds RPM packages
%files
