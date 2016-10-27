var ws = new WebSocket("ws://" + location.host + "/ws");

ws.onopen = function (event) {
  var msg = {
    event: "load"
  };
  ws.send(JSON.stringify(msg));
};

ws.onmessage = function (event) {
  var msg = JSON.parse(event.data);
  console.log("event:" + msg.event);
  var element = document.getElementById(msg.id);
  switch (msg.event) {
    case "redir":
      break;
    case "display":
      element.style.display = msg.val;
      break;
    case "gettext":
      sendText(msg.id, element.innerHTML);
      break;
    case "sethtml":
      element.innerHTML = msg.html;
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
    case "setpos":
      element.style.left = msg.x;
      element.style.top = msg.y;
      break;
  }
};

var splitDragging = null;

function onmouseupBody(event, element) {
  if (splitDragging) {
    splitDragging = null;
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

function onmousemoveBody(event, element) {
  if (splitDragging) {
    onmousemoveSplitPanel(event, element);
  }
}

function onClick(event, element) {
  var msg = {
    event: "click",
    id: element.id
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

function openTab(event, idx, tabsid, rowsid) {
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

  var tabs2 = document.getElementById(rowsid);
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
    //left/top current size
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

function onmousemoveDrag(event, element) {
  var top = splitDragging.startY + (event.clientY - splitDragging.mouseY);
  var left = splitDragging.startX + (event.clientX - splitDragging.mouseX);
  dragging.element.style.top = (Math.max(0, top)) + "px";
  dragging.element.style.left = (Math.max(0, left)) + "px";
}

function onClickMenu(event, element) {
  var msg = {
    event: "click",
    id: element.id
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
