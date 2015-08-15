Buildroot: /.
Name: jftorrent
Version: 0.4
Release: 1
Summary: Torrent Client, Tracker, Maker all-in-one
License: LGPL
Distribution: Fedora
Group: Applications/System
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Torrent Client, Tracker, Maker all-in-one

%files
