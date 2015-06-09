<!--

This will load JPL Applet with a param 'userid'.
The applet will then load "jphonelite-getconfig.php?userid='userid'" to retrieve the XML configuration file.

-->
<html>
<head>
  <title>jPhoneLite</title>
</head>
<body leftmargin=0 rightmargin=0 topmargin=0 bottommargin=0 vlink=ffffff link=ffffff alink=ffffff width=100% height=100% bgcolor=777777>
<script type="text/javascript" src="jphonelite.js"></script>
<center>
<table>
<tr><td colspan=3>Dial:<input id='dial' readonly></td></tr>
<tr><td colspan=3>Status:<input id='status' readonly></td></tr>
<tr><td><button onclick="addDigit(1);">1</button></td><td><button onclick="addDigit(2);">2</button></td><td><button onclick="addDigit(3);">3</button></td></tr>
<tr><td><button onclick="addDigit(4);">4</button></td><td><button onclick="addDigit(5);">5</button></td><td><button onclick="addDigit(6);">6</button></td></tr>
<tr><td><button onclick="addDigit(7);">7</button></td><td><button onclick="addDigit(8);">8</button></td><td><button onclick="addDigit(9);">9</button></td></tr>
<tr><td><button onclick="addDigit('*');">*</button></td><td><button onclick="addDigit(0);">0</button></td><td><button onclick="addDigit('#');">#</button></td></tr>
<tr><td><button onclick="call();">Call</button></td><td><button onclick="clearDial();">Clear</button></td><td><button onclick="end();">End</button></td></tr>
</table>
<button onclick="toggleAppletVisible();">Toggle Applet Visible</button>
<div id="applet" style="overflow-y:hidden;">
<applet code="PhoneApplet" archive="jphonelite-signed.jar,javaforce-signed.jar,jna-signed.jar,bouncycastle-signed.jar" name="jPhoneLiteApplet" width=560 height=350>
<param name="isJavaScript" value="true"/>
<param name=userid value="<?php echo($_REQUEST['userid']); ?>">
</applet>
</div>
</center>
</body>
</html>
