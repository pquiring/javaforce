jfDVR
=====

Records IP cameras over RTSP (h264/h265).

Typical video URLs:
  rtsp://username:password@192.168.1.15/cam/realmonitor?channel=1&subtype=0
  rtsp://username:password@192.168.1.16/profile4
Typical picture URL:
  http://username:password@192.168.1.16/cgi-bin/snapshot.cgi

iSpy Connect can help detect URLs.

Includes motion detection recording.

Does NOT re-encode video if recording but just saves live stream into mp4 files.

Known issues:
 - motion detection still needs work

Downloads & Bug Reports : jfdvr.sf.net
Source : github.com/pquiring/javaforce/tree/master/projects/jfdvr

Author : Peter Quiring (pquiring at gmail dot com)
