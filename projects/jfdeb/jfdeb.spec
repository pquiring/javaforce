Buildroot: .
Name: jfdeb
Version: 1.0
Release: 1
Summary: Builds DEB packages
License: LGPL
Distribution: Fedora
Group: Applications/System
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0
%description
Builds DEB packages
%files
