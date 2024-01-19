@echo off
echo "rtsp://camera:camera123@10.66.132.101/cam/realmonitor?channel=1&subtype=0"
..\..\bin\jfexec -cp javaforce.jar;jfmedia.jar MediaApp %1
