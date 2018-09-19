function setHTML(id, html) {
  var obj = document.getElementById(id);
  obj.innerHTML = html;
}
function show(id) {
  var obj = document.getElementById(id);
  obj.style.display = '';
}
function hide(id) {
  var obj = document.getElementById(id);
  obj.style.display = 'none';
}
//XML Loading (Requires IE5.5 / Mozilla 4.0 or better)

function xmlcreate() {
  var xmlHttp = null;
  if (window.XMLHttpRequest) {
    // If IE7, Mozilla, Safari, and so on: Use native object
    xmlHttp = new XMLHttpRequest();
  } else {
    if (window.ActiveXObject) {
      // ...otherwise, use the ActiveX control for IE5.x and IE6
      xmlHttp = new ActiveXObject('MSXML2.XMLHTTP.3.0');
    }  else {
      alert("Error initializing XMLHttpRequest!");
    }
  }
  return xmlHttp;
}
var xmlhttp;
var xmlobj;
var xmlstackurl = [];  //stack requires IE5.5/Mozilla 4.0
var xmlstackobj = [];
function xmlRequestGet(url, obj) {
  xmlobj = obj;
  xmlhttp = xmlcreate()
  xmlhttp.onreadystatechange = xmlparser;
  xmlhttp.open("get", url);
  xmlhttp.send("");
}
function xmlRequestPost(url, obj) {
  xmlobj = obj;
  xmlhttp = xmlcreate()
  xmlhttp.onreadystatechange = xmlparser;
  xmlhttp.open("post", url);
  xmlhttp.send("");
}
function xmlparser() {
  if (xmlhttp.readyState != 4) return;
  if (xmlhttp.status == 0 || xmlhttp.status == 200)  {
    var answer = xmlhttp.responseText;
    var node = document.getElementById(xmlobj);
    if (node != null) {
      node.innerHTML = answer;
    }
    xmlprocessqueue();
    return;
  }
  if (xmlhttp.status == 404) {
    var node = document.getElementById(xmlobj);
    if (node != null) {
      node.innerHTML = "Error 404";
    }
    xmlprocessqueue();
    return;
  }
}
function xmladdqueue(url, obj) {
  xmlstackurl.push(url);
  xmlstackobj.push(obj);
}
function xmlprocessqueue() {
  if (xmlstackurl.length == 0) return;
  var url = xmlstackurl.shift();
  var obj = xmlstackobj.shift();
  if ((url == null) || (obj == null)) return;
  xmlRequestGet(url, obj);
}
function link(url, obj) {
  if (obj == null) obj = "cal";
  xmladdqueue(url + webext, obj);
  xmlprocessqueue();
}

function showHelp(name) {
  xmladdqueue( '/static/help_' + name + '.txt', 'helpcontent');
  xmlprocessqueue();
  show('help');
}

function load_preset() {
  var preset = document.getElementById("preset");
  var selected = preset.selectedIndex;
  switch (selected) {
    case 0:
      return;
    case 1:
      xmladdqueue( '/static/preset_ivr_conf.txt', 'script');
      break;
    case 2:
      xmladdqueue( '/static/preset_ivr_conf_video.txt', 'script');
      break;
  }
  xmlprocessqueue();
}
