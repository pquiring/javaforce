Buildroot: /.
Name: jfhexbig
Version: 0.3
Release: 1
Summary: Sector Hex editor (for large files)
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Sector Hex editor (for large files)

%files
