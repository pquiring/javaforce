Buildroot: /.
Name: jfpaint
Version: 0.20
Release: 1
Summary: Java Paint image editor
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Java Paint image editor

%files
