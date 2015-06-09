Buildroot: /.
Name: jfparted
Version: 0.4
Release: 1
Summary: Java Partition Editor
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, parted
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

Java Partition Editor

%files
