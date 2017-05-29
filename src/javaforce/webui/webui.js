var ws = new WebSocket("ws://" + location.host + "/webui");

var bindata;

var temp = document.createElement('div');

function load(event) {
  sendSize('body');
  var msg = {
    event: "load"
  };
  ws.send(JSON.stringify(msg));
};

//WebSocket binaryType default is Blob but converting it to TypedArray is very SLOW, so ask to get data in ArrayBuffer format instead
ws.binaryType = "arraybuffer";

ws.onopen = load;

ws.onmessage = function (event) {
  if (event.data instanceof ArrayBuffer) {
    bindata = event.data;
    return;
  }
  var msg = JSON.parse(event.data);
  console.log("event:" + msg.event);
  var element = document.getElementById(msg.id);
  if (element == null) {
    console.log("element not found:" + msg.id);
    return;
  }
  if (element.gl) {
    gl_message(msg, element);
    return;
  }
  switch (msg.event) {
    case "redir":
      load();
      break;
    case "display":
      element.style.display = msg.val;
      break;
    case "gettext":
      sendText(msg.id, element.innerHTML);
      break;
    case "sethtml":
      element.innerHTML = msg.html;
      sendAck(msg.id);
      break;
    case "setsrc":
      element.src = msg.src;
      break;
    case "settext":
      element.innerHTML = msg.text;
      break;
    case "setvalue":
      element.value = msg.value;
      break;
    case "setidx":
      element.selectedIndex = msg.idx;
      break;
    case "setclr":
      element.style.color = msg.clr;
      break;
    case "setbackclr":
      element.style.backgroundColor = msg.clr;
      break;
    case "setborderclr":
      element.style.borderColor = msg.clr;
      break;
    case "setzindex":
      element.style.zIndex = msg.idx;
      break;
    case "addoption":
      var option = document.createElement("option");
      option.value = msg.value;
      option.text = msg.text;
      element.add(option);
      break;
    case "removeoption":
      element.remove(msg.idx);
      break;
    case "setpos":
      element.style.left = msg.x;
      element.style.top = msg.y;
      break;
    case "getpossize":
      sendPosSize(msg.id);
      break;
    case "getpos":
      sendPos(msg.id);
      break;
    case "getsize":
      sendSize(msg.id);
      break;
    case "setsize":
      element.style.width = msg.w;
      element.style.height = msg.h;
      break;
    case "setwidth":
      element.style.width = msg.w;
      break;
    case "setheight":
      element.style.height = msg.h;
      break;
    case "setclass":
      element.className = msg.cls;
      break;
    case "addclass":
      element.className += " " + msg.cls;
      break;
    case "delclass":
      element.className = element.className.replace(" " + msg.cls, "");
      break;
    case "add":
      temp.innerHTML = msg.html;
      element.appendChild(temp.firstChild);
      break;
    case "addbefore":
      temp.innerHTML = msg.html;
      element.insertBefore(temp.firstChild, document.getElementById(msg.beforeid));
      break;
    case "remove":
      var child = document.getElementById(msg.child);
      element.removeChild(child);
      break;
    case "settab":
      openTab(null, parseInt(msg.idx), msg.tabs, msg.row);
      break;
    case "focus":
      element.focus();
      break;
    case "initwebgl":
      gl_init(element);
      break;
  }
};

function sendAck(id) {
  var msg = {
    event: "ack",
    id: id
  };
  ws.send(JSON.stringify(msg));
}

function sendPosSize(id) {
  var element = document.getElementById(id);
  var rect = element.getBoundingClientRect();
  var msg = {
    event: "possize",
    id: id,
    x: Math.floor(rect.left),
    y: Math.floor(rect.top),
    w: Math.floor(rect.width),
    h: Math.floor(rect.height)
  };
  ws.send(JSON.stringify(msg));
}

function sendSize(id) {
  var element = document.getElementById(id);
  var rect = element.getBoundingClientRect();
  var msg = {
    event: "size",
    id: id,
    w: Math.floor(rect.width),
    h: Math.floor(rect.height)
  };
  ws.send(JSON.stringify(msg));
}

function sendPos(id) {
  var element = document.getElementById(id);
  var rect = element.getBoundingClientRect();
  var msg = {
    event: "pos",
    id: id,
    x: Math.floor(rect.left),
    y: Math.floor(rect.top)
  };
  ws.send(JSON.stringify(msg));
}

var splitDragging = null;
var popupDragging = null;

function onmouseupBody(event, element) {
  if (splitDragging) {
    splitDragging = null;
  }
  if (popupDragging) {
    popupDragging = null;
  }
}

function onmousemoveBody(event, element) {
  if (splitDragging) {
    onmousemoveSplitPanel(event, element);
  }
  if (popupDragging) {
    onmousemovePopupPanel(event, element);
  }
}

function onmousedownBody(event, element) {
  console.log("mousedownBody:" + event.x + "," + event.y + "," + element.id);
  var msg = {
    event: "mousedown",
    id: element.id,
    p: event.clientX + "," + event.clientY
  };
  ws.send(JSON.stringify(msg));
}

function onresizeBody(event, element) {
  sendSize('body');
}

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

function onComboBoxChange(event, element) {
  var msg = {
    event: "changed",
    id: element.id,
    index: element.selectedIndex
  };
  ws.send(JSON.stringify(msg));
}

function onTextChange(event, element) {
  var msg = {
    event: "changed",
    id: element.id,
    text: element.value
  };
  ws.send(JSON.stringify(msg));
}

function sendText(id, text) {
  var msg = {
    event: "sendtext",
    id: id,
    text: text
  };
  ws.send(JSON.stringify(msg));
}

function openTab(event, idx, tabsid, rowid) {
  var tabs = document.getElementById(tabsid);
  var nodes = tabs.childNodes;
  var cnt = nodes.length;
  for(i = 0;i < cnt;i++) {
    if (i === idx) {
      nodes[i].className = "tabcontentshown";
    } else {
      nodes[i].className = "tabcontenthidden";
    }
  }

  if (rowid == null) return;

  var tabs2 = document.getElementById(rowid);
  var nodes2 = tabs2.childNodes;
  var cnt2 = nodes2.length;
  for(i = 0;i < cnt2;i++) {
    if (i === idx) {
      nodes2[i].className = "tabactive";
    } else {
      nodes2[i].className = "tabinactive";
    }
  }
}

function onmousedownSplitPanel(event, element, id1, id2) {
  var element1 = document.getElementById(id1);
  var element2 = document.getElementById(id2);
  splitDragging = {
    //mouse coords
    mouseX: event.clientX,
    mouseY: event.clientY,
    //current size
    startX: element1.clientWidth,
    startY: element1.clientHeight,
    //elements
    element: element,
    element1: element1,
    element2: element2
  };
}

function onmousemoveSplitPanel(event, element) {
//  console.log("m=" + event.clientX + "," + event.clientY);
  var width = splitDragging.startX + (event.clientX - splitDragging.mouseX);
  splitDragging.element1.style.width = width + "px";
}

function onmousedownPopupPanel(event, element) {
  var rect = element.getBoundingClientRect();
  popupDragging = {
    //mouse coords
    mouseX: event.clientX,
    mouseY: event.clientY,
    //left/top
    startX: rect.left,
    startY: rect.top,
    //elements
    element: element
  };
}

function onmousemovePopupPanel(event, element) {
  var top = popupDragging.startY + (event.clientY - popupDragging.mouseY);
  var left = popupDragging.startX + (event.clientX - popupDragging.mouseX);
  popupDragging.element.style.top = (Math.max(0, top)) + "px";
  popupDragging.element.style.left = (Math.max(0, left)) + "px";
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

function onSliderMove(event, element) {
  var msg = {
    event: "changed",
    id: element.id,
    pos: element.value
  };
  ws.send(JSON.stringify(msg));
}

function closePanel(event, element) {
  element.style.display = "none";
  var msg = {
    event: "close",
    id: element.id
  };
  ws.send(JSON.stringify(msg));
}

function onResized(event, element) {
  sendSize(element.id);
}

function onMoved(event, element) {
  sendPos(element.id);
}
