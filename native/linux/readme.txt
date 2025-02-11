Linux Loader
============

Desc
----

  - These C++ sources build the Linux loader and native API used by JavaForce
  - /usr/bin/java is never used to run Java apps within JavaForce
  - the native API provides features that are currently unavailable in Java

Requirements
------------
  - to install required packages to compile the loader:
    chmod +x deps.sh
    sudo ./deps.sh

Compiling (Linux)
-----------------
  ant

Compiling (FreeBSD)
-------------------
  ant freebsd
