Buildroot: /.
Name: jfhex
Version: 0.3
Release: 1
Summary: Multi-tabbed hex editor
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Multi-tabbed hex editor

%files
