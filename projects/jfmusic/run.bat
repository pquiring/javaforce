@echo off
copy examples \users\pquiring\Music
java -Djava.app.home=%CD% -cp javaforce.jar;jfmusic.jar MusicApp %1
