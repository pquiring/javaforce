//jphonelite javascript sample

function addDigit(digit) {
  var dial = document.getElementById('dial');
  dial.value = dial.value + digit;
}

function clearDial() {
  var dial = document.getElementById('dial');
  dial.value = '';
}

function end() {
  document.jPhoneLiteApplet.getBasePhone().callEDT("end");
}

function call() {
  var dial = document.getElementById('dial');
  document.jPhoneLiteApplet.getBasePhone().callEDT("setDial", dial.value);
  document.jPhoneLiteApplet.getBasePhone().callEDT("call");
}

//NOTE : Don't use div.style.display = "none" cause this causes the applet to reload.
var orgAppletHeight = 0;
function toggleAppletVisible() {
  var div = document.getElementById('applet');
  var height = div.offsetHeight;
  if (height == 0) {
    div.style.height = orgAppletHeight;
  } else {
    orgAppletHeight = height;
    div.style.height = 0;
  }
}

function updateStatus() {
  var value = document.jPhoneLiteApplet.getBasePhone().callEDTreturn("getLineStatus");
  var status = document.getElementById('status');
  status.value = value;
}

function init() {
  var applet = document.jPhoneLiteApplet;
  if (applet == null) {
    //give more time to load applet
    setTimeout("init()", 1000);
    return;
  }
  applet.getBasePhone().callEDT("selectLine", "0");
  setInterval("updateStatus()", 100);
}

setTimeout("init()", 1000);  //give applet time to load
