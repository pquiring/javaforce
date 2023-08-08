jfftp
=====

Java App/Applet FTP Client.  Built using the JavaForce SDK.

Supports : ftp(21), ftps(990), sftp(22), smb(445)

Import sites from FileZilla3 exported sites.

Settings are stored safely on your local system.

WebSite : http://jfftp.sourceforge.net

By : Peter Quiring (pquiring at gmail dot com)

Required Libraries:
 - jcraft/jsch package for SSH support (http://www.jcraft.com/jsch/)
   - copy their sources into /src
 - jcifs package for SMB support (Windows Shares) (http://jcifs.samba.org)
   - copy their sources into /src
   - delete the http/https folders since they are not needed if you have compile errors (they require Java EE)

To compile from command line (after you copy in required libraries):
  cd /
  ant
  cd /projects/jfftp
  ant

TODO:
  - add keep-alives
  - more testing on various servers
  - support permissions via SMB protocol

