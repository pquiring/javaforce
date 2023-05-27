<?php

function startsWith($haystack, $needle) {
  // search backwards starting from haystack length characters from the end
  return $needle === "" || strrpos($haystack, $needle, -strlen($haystack)) !== FALSE;
}

function endsWith($haystack, $needle) {
  // search forward starting from end minus needle length characters
  return $needle === "" || (($temp = strlen($haystack) - strlen($needle)) >= 0 && strpos($haystack, $needle, $temp) !== FALSE);
}

$ch = curl_init();

$headers = $_REQUEST['headers'];
$post = $_REQUEST['post'];
$secure = $_REQUEST['secure'];

//decode parts

$headers2 = urldecode($headers);

$headers3 = split("\r\n", $headers2);

$ln0 = $headers3[0];  //GET http://page.html HTTP/1.x

$parts = split(" ", $ln0);

$method = $parts[0];  //GET or POST
$url = $parts[1];
$ver = $parts[2];  //HTTP/1.0 or HTTP/1.1

if (startsWith($url, "/")) {
  //URL does not include host
  foreach($headers3 as $ln) {
    if (startsWith($ln, "Host: ")) {
//      error_log("Host=" . $ln);
      if ($secure == "true")
        $url = "https://" . substr($ln, 6) . $url;
      else
        $url = substr($ln, 6) . $url;
      break;
    }
  }
}

//error_log("URL=" . $url);

array_shift($headers3);  //remove request line

curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers3);
curl_setopt($ch, CURLOPT_HEADER, 1);
curl_setopt($ch, CURLOPT_HTTP_VERSION, CURL_HTTP_VERSION_1_0);

if ($method == "POST") {
  curl_setopt($ch, CURLOPT_POST, 1);
  curl_setopt($ch, CURLOPT_POSTFIELDS, $post);
}

curl_exec($ch);

curl_close($ch);

?>