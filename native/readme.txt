This folder contains all native loaders which register native methods and starts main program entry points.

Platforms folders:
 - linux
 - windows
 - mac

Each folder will generate loaders and an optional shared library that can be used if the loaders are not used.
So if you prefer to use java.exe (or /usr/bin/java) just make sure jfnative64.dll (.so .dylib) is present.

FFM will look for jfnative64 shared library in predefined locations if the JF native loaders were not used:
  Windows: %ProgramData%\javaforce
  Linux: /usr/lib
This is true in webapp environments where the native loaders are not used (ie: tomcat).
