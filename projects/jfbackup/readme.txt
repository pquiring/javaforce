jfBackup
========

Enterprise Tape Backup Solution for Windows Server 2008R2 x64 or better.

Features:
  - Volume Shadow Backups (backup open files)
  - Full Backups of Remote and Local Volumes (client/server model)
  - Tape Changer (multi-tape backups w/ barcode reader support)
  - software compression (zlib)

Not Supported (yet):
  - Incremental Backups
  - Image Backups (bare-metal restores)
  - SQL Backups
  - File Meta Data (file attributes, ACLs, etc.)
  - Restore in place (restored files need to be moved back into place)

Web Interface:
  - http://your_host_name:34003

Tested hardware:
  - Quantum Superloader 3
  - 1.5TB LTO-5 tapes

TCP Ports:
  - 33200 & 34003

Notes:
  - tapetool.exe - is an easy to use command line interface to use tape drive/media changer.
  - tapes that are prefixed with "CLN" or suffixed with "CU" are assumed to be cleaning tapes (TODO : add config option for prefix)
  - jfBackup can be installed on Windows 10 x64 in server mode but local volumes will not be available for backup (vss create shadow is not available)
  - restored files are placed in C:\restored

License:
  - LGPL
  - No warranty of any kind is given.  Not liable for data loss or corruption.  Use at your own risk.

WebSite : http://jfbackup.sf.net

Source Code : https://github.com/pquiring/javaforce/tree/master/projects/jfbackup

Author : Peter Quiring
eMail : pquiring at gmail dot com

Version : 0.19
Date : 10/31/2020
