<!--

This file should output the XML configuration (see .jphone.xml - usually stored in your user profile folder)
This is a sample that you should complete to suit your needs.

Note : If you place XML data right inside this php file you should omit the XML header because it will cause an error 500 in PHP.

-->
<?php

//retrieve XML data for user based on $_REQUEST['userid'] and output it

//the simplest method is to just have the XML files on your webserver with the same filename as the userid

//file method:

$body = file_get_contents($_REQUEST['userid'] . ".xml");
echo($body);

//but this could potentially expose the XML files making it possible for someone to see someone elses passwords
//unless you could block access to the XML files
//v0.15 - this is less a problem now since passwords are encrypted

//if you want you could have the XML files loaded into a database for added security

//sql method:

$sql = mysql_connect('localhost', 'root', 'password');
$res = mysql_query("select xml from jphoneliteconfigs where userid='" . $_REQUEST['userid'] . "'");
$row = mysql_fetch_row($res);
echo($row[0]);

//So you would create a table like this:
//  create table jphoneliteconfigs (xml blob, userid varchar(32));
//and then insert XML files into it for each user using INSERT or LOAD DATA
//but this is a bit more complex

?>
