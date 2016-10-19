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
  var comp = document.getElementById(msg.comp);
  switch (msg.event) {
    case "redir":
      break;
    case "hide":
      break;
    case "show":
      break;
    case "gettext":
      sendText(msg.comp, comp.innerHTML);
      break;
    case "sethtml":
      comp.innerHTML = msg.html;
      break;
    case "setsrc":
      comp.src = msg.src;
      break;
    case "settext":
      comp.innerHTML = msg.text;
      break;
    case "setvalue":
      comp.value = msg.value;
      break;
  }
};

var splitDragging = null;

function onmouseupBody(e) {
  if (splitDragging) {
    splitDragging = null;
  }
}

function onmousemoveBody(e) {
  if (splitDragging) {
    onmousemoveSplitPanel(e);
  }
}

function onClick(comp) {
  var msg = {
    event: "click",
    comp: comp.id
  };
  ws.send(JSON.stringify(msg));
}

function onComboBoxChange(comp) {
  var msg = {
    event: "changed",
    comp: comp.id,
    index: comp.selectedIndex
  };
  ws.send(JSON.stringify(msg));
}

function onTextChange(comp) {
  var msg = {
    event: "changed",
    comp: comp.id,
    text: comp.value
  };
  ws.send(JSON.stringify(msg));
}

function sendText(comp, text) {
  var msg = {
    event: "sendtext",
    comp: comp,
    text: text
  };
  ws.send(JSON.stringify(msg));
}

function openTab(idx, tabsid, rowsid) {
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

function onmousedownSplitPanel(e, e1, e2) {
  var ele1 = document.getElementById(e1);
  var ele2 = document.getElementById(e2);
  splitDragging = {
    //mouse coords
    mouseX: window.event.clientX,
    mouseY: window.event.clientY,
    //left/top current size
    startX: ele1.clientWidth,
    startY: ele1.clientHeight,
    //elements
    element1: ele1,
    element2: ele2
  };
//  console.log("m=" + window.event.clientX + "," + window.event.clientY);
//  console.log("o=" + e.offsetLeft + "," + e.offsetTop);
//  console.log("s=" + ele1.clientWidth + "," + ele1.clientHeight);
}

function onmousemoveSplitPanel(e) {
//  console.log("m=" + window.event.clientX + "," + window.event.clientY);
  var width = splitDragging.startX + (window.event.clientX - splitDragging.mouseX);
  splitDragging.element1.style.width = width + "px";
}

function onmousemoveDrag(e) {
  var top = splitDragging.startY + (window.event.clientY - splitDragging.mouseY);
  var left = splitDragging.startX + (window.event.clientX - splitDragging.mouseX);
  dragging.element.style.top = (Math.max(0, top)) + "px";
  dragging.element.style.left = (Math.max(0, left)) + "px";
}
