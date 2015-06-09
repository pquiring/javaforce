Buildroot: /.
Name: jftest3d
Version: 0.1
Release: 1
Summary: Test OpenGL
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

Test OpenGL

%files
