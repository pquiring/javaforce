Buildroot: /.
Name: jfaudio
Version: 0.14
Release: 1
Summary: Audio Editor
License: LGPL
Distribution: Fedora
Group: Applications/System
Requires: javaforce, ffmpeg
BuildArch: noarch

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.noarch.rpm
%define _unpackaged_files_terminate_build 0

%description

Audio Editor

%files
