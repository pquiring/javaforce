Buildroot: .
Name: jfwebproxy
Version: 1.5
Release: 1
Summary: WebBased Proxy Server
License: LGPL
Distribution: Fedora
Group: Applications/System
BuildArch: @ARCH@

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.@ARCH@.rpm
%define _unpackaged_files_terminate_build 0
%description
WebBased Proxy Server
%files
