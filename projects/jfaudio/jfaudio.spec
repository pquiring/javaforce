Buildroot: /.
Name: jfaudio
Version: 0.14
Release: 1
Summary: Audio Editor
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, ffmpeg
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0

%description

Audio Editor

%files
