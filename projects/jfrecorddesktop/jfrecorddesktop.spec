Buildroot: /.
Name: jfrecorddesktop
Version: 0.4
Release: 1
Summary: Records Desktop
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, ffmpeg
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Records Desktop to Video file

%files
