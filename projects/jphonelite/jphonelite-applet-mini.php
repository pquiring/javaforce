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
<applet code="PhoneAppletMini" archive="jphonelite-signed.jar,javaforce-signed.jar,jna-signed.jar,bouncycastle-signed.jar" width=290 height=250><param name=userid value="<?php echo($_REQUEST['userid']); ?>"><param name=number value='18005551234'></applet>
</center>
</body>
</html>
