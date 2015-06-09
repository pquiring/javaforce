Buildroot: /.
Name: jfburn
Version: 0.2
Release: 1
Summary: Disc Burning
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, wodim
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

Disc Burning

%files
