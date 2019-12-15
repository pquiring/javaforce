var imgcnt = 0;
var playing = false;
var onpower = false;

function onBattery(charging) {
  if (onpower !== charging) {
    var icon = document.getElementById('chargingicon');
    if (charging) {
      icon.src = "/green";
    } else {
      icon.src = "/red";
    }
  }
  onpower = charging;
}

window.onload = function () {
  navigator.getBattery().then(function(battery) {
    function updateBatteryStatus(battery) {
      document.querySelector('#charging').textContent = battery.charging ? 'charging' : 'not charging';
      document.querySelector('#level').textContent = battery.level;
      document.querySelector('#dischargingTime').textContent = battery.dischargingTime / 60;
      onBattery(battery.charging);
    }
    // Update the battery status initially when the promise resolves ...
    updateBatteryStatus(battery);

    // .. and for any subsequent updates.
    battery.onchargingchange = function () {
      updateBatteryStatus(battery);
    };

    battery.onlevelchange = function () {
      updateBatteryStatus(battery);
    };

    battery.ondischargingtimechange = function () {
      updateBatteryStatus(battery);
    };
  });
  refresh();
};

//websocket stuff

/*

var ws;

var bindata;

var delay = 500;

function wsopen(event) {
};

function wsclose(event) {
  if (play) setTimeout(connect, delay);
}

function connect() {
  ws = new WebSocket("ws://" + location.host + "/websocket");
  //WebSocket binaryType default is Blob but converting it to TypedArray is very SLOW, so ask to get data in ArrayBuffer format instead
  ws.binaryType = "arraybuffer";
  ws.onopen = wsopen;
  ws.onclose = wsclose;
  ws.onmessage = wsevent;
}

function disconnect() {
  ws.close();
}

function wsevent(event) {
  if (event.data instanceof ArrayBuffer) {
    bindata = event.data;
    return;
  }
  var msg = JSON.parse(event.data);
  console.log("event:" + msg.event);
  switch (msg.event) {
    case "updateimage":
      updateImage();
      break;
  }
};

function onClick(event, element) {
  var msg = {
    event: "click",
    id: element.id,
    ck: event.ctrlKey,
    ak: event.altKey,
    sk: event.shiftKey
  };
  ws.send(JSON.stringify(msg));
}

function onMouseEnter(event, element) {
  var rect = element.getBoundingClientRect();
  var msg = {
    event: "mouseenter",
    id: element.id,
    x: Math.floor(rect.left),
    y: Math.floor(rect.top),
    w: Math.floor(rect.width),
    h: Math.floor(rect.height)
  };
  ws.send(JSON.stringify(msg));
}

function onMouseMove(event, element) {
  var msg = {
    event: "mousemove",
    id: element.id
  };
  ws.send(JSON.stringify(msg));
}

function onMouseDown(event, element) {
  var msg = {
    event: "mousedown",
    id: element.id,
    p: event.clientX + "," + event.clientY
  };
  ws.send(JSON.stringify(msg));
}

function onMouseUp(event, element) {
  var msg = {
    event: "mouseup",
    id: element.id,
    p: event.clientX + "," + event.clientY
  };
  ws.send(JSON.stringify(msg));
}

function onKeyDown(event, element) {
  var msg = {
    event: "keydown",
    id: element.id,
    ck: event.ctrlKey,
    ak: event.altKey,
    sk: event.shiftKey,
    keyCode: event.keyCode,
    charCode: event.charCode
  };
  ws.send(JSON.stringify(msg));
}

function onKeyUp(event, element) {
  var msg = {
    event: "keyup",
    id: element.id,
    ck: event.ctrlKey,
    ak: event.altKey,
    sk: event.shiftKey,
    keyCode: event.keyCode,
    charCode: event.charCode
  };
  ws.send(JSON.stringify(msg));
}

*/

function toggle() {
  var req = new XMLHttpRequest();
  req.open("GET", "/toggle");
  req.send();
}

function play() {
  try {
    var audio = document.getElementById('audio');
    var promise = audio.play();
    playing = true;
    toggle();
//    connect();
  } catch (err) {
    console.log("playing failed");
  }
}

function pause() {
  try {
    var audio = document.getElementById('audio');
    var promise = audio.pause();
    playing = false;
    toggle();
//    disconnect();
  } catch (err) {
    console.log("pausing failed");
  }
}

function timer() {
  if (onpower) {
    if (!playing) play();
  } else {
    if (playing) pause();
  }
}

function start() {
  var controls = document.getElementById('controls');
  controls.style.display = 'none';
  play();
  setInterval(timer, 1000);
}

function refresh() {
  var screen = document.getElementById('screen');
  var top = document.getElementById('top');
  var width = document.body.clientWidth;
  var topheight = top.clientHeight;
  var height = document.body.clientHeight - topheight - 5;
  screen.src = '/screen?x=' + width + '&y=' + height + '&id=' + imgcnt;
  imgcnt++;
}

function touch(event) {
  var screen = document.getElementById('screen');
  var top = document.getElementById('top');
  var topheight = top.clientHeight;
  var width = screen.clientWidth;
  var height = screen.clientHeight;
  var req = new XMLHttpRequest();
  var mx = event.clientX;
  var my = event.clientY - topheight;
  req.open("GET", "/touch?x=" + width + "&y=" + height + "&mx=" + mx + "&my=" + my);
  req.send();
}
