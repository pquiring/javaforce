Buildroot: /.
Name: jfile
Version: 0.12
Release: 1
Summary: File Manager
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, samba
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

File Manager

%files
