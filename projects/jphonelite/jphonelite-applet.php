<!--

This will load JPL Applet with a param 'userid'.
The applet will then load "jphonelite-getconfig.php?userid='userid'" to retrieve the XML configuration file.

-->

<html>
<head>
  <title>jPhoneLite</title>
</head>
<body leftmargin=0 rightmargin=0 topmargin=0 bottommargin=0 vlink=ffffff link=ffffff alink=ffffff width=100% height=100%>
<center>
<applet code="PhoneApplet" archive="jphonelite-signed.jar,javaforce-signed.jar,jna-signed.jar,bouncycastle-signed.jar" width=560 height=350>
<param name=userid value="<?php echo($_REQUEST['userid']); ?>">
</applet>
</center>
</body>
</html>
