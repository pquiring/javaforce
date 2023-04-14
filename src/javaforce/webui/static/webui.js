var ws;

var bindata;

var body = document.getElementById('body');
var orghtml = body.innerHTML;
var temp = document.createElement('div');
var root;

var delay = 500;

function getWidth(element) {
  var str = element.style.width;
  if (str === null || str.length === 0 || str.endsWith("%")) {
    return element.offsetWidth;
  }
  if (str.endsWith("px")) {
    str = str.substring(0, str.length - 2);
  }
  return parseInt(str);
}

function getHeight(element) {
  var str = element.style.height;
  if (str === null || str.length === 0 || str.endsWith("%")) {
    return element.offsetHeight;
  }
  if (str.endsWith("px")) {
    str = str.substring(0, str.length - 2);
  }
  return parseInt(str);
}

function load() {
  var msg = {
    event: "load"
  };
  ws.send(JSON.stringify(msg));
}

function wsopen(event) {
  sendSize('body');
  load();
};

function wsclose(event) {
  body.innerHTML = orghtml;
  setTimeout(connect, delay);
}

function connect() {
  ws = new WebSocket("ws://" + location.host + "/webui");
  //WebSocket binaryType default is Blob but converting it to TypedArray is very SLOW, so ask to get data in ArrayBuffer format instead
  ws.binaryType = "arraybuffer";
  ws.onopen = wsopen;
  ws.onclose = wsclose;
  ws.onmessage = wsevent;
}

function wsevent(event) {
  if (event.data instanceof ArrayBuffer) {
    bindata = event.data;
    return;
  }
  var msg = JSON.parse(event.data);
  console.log("event:" + msg.event + " id=" + msg.id + " tid=" + msg.tid);
  var element = document.getElementById(msg.id);
  if (element === null) {
    console.log("element not found:" + msg.id);
    return;
  }
  if (element.gl) {
    gl_message(msg, element);
    return;
  }
  switch (msg.event) {
    case "initwebgl":
      gl_init(element);
      break;
    case "redir":
      load();
      break;
    case "openurl":
      window.open(msg.url);
      break;
    case "display":
      element.style.display = msg.val;
      break;
    case "gettext":
      sendText(msg.id, element.innerHTML);
      break;
    case "settitle":
      document.title = msg.title;
      break;
    case "sethtml":
      element.innerHTML = msg.html;
      sendOnLoaded(msg.id);
      break;
    case "setroot":
      root = document.getElementById(msg.root);
      root.dispatchEvent(new Event('resize'));
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
    case "setmarginleft":
      element.style.marginLeft = msg.px + "px";
      break;
    case "setmargintop":
      element.style.marginTop = msg.px + "px";
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
    case "setwidthtoparent":
      element.style.width = element.parentElement.offsetWidth;
      break;
    case "setheighttoparent":
      element.style.height = element.parentElement.offsetHeight;
      break;
    case "setsizetoparent":
      element.style.width = element.parentElement.offsetWidth;
      element.style.height = element.parentElement.offsetHeight;
      break;
    case "setsizetoparent2":
      element.style.width = getWidth(element.parentElement.parentElement);
      element.style.height = getHeight(element.parentElement.parentElement);
      break;
    case "setsizetoparent3":
      element.style.width = element.parentElement.parentElement.parentElement.offsetWidth;
      element.style.height = element.parentElement.parentElement.parentElement.offsetHeight;
      break;
    case "setclass":
      element.className = msg.cls;
      break;
    case "addclass":
      element.classList.add(msg.cls);
      break;
    case "delclass":
      element.classList.remove(msg.cls);
      break;
    case "add":
      temp.innerHTML = msg.html;
      element.appendChild(temp.firstChild);
      temp.innerHTML = '';
      break;
    case "addbefore":
      temp.innerHTML = msg.html;
      element.insertBefore(temp.firstChild, document.getElementById(msg.beforeid));
      temp.innerHTML = '';
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
    case "setchecked":
      element.checked = msg.state === "true";
      break;
    case "ping":
      sendPong(msg.id);
      break;
    case "onresize":
      element.onresize();
      break;
    case "enabledrag":
      enableDrag(msg.id, msg.type, msg.x1, msg.y1, msg.x2, msg.y2);
      break;
    case "drawrect":
      var ctx = element.getContext('2d');
      ctx.strokeStyle = msg.clr;
      ctx.strokeRect(msg.x, msg.y, msg.w, msg.h);
      break;
    case "media_set_source":
      media_set_source(element, msg.src);
      break;
    case "media_set_live_source":
      media_set_live_source(element, msg.codecs);
      break;
    case "media_set_capture":
      media_set_capture(element, msg.audio, msg.video);
      break;
    case "media_uninit":
      media_uninit(element);
      break;
    case "media_add_buffer":
      media_add_buffer(element);
      break;
    case "media_play":
      media_play(element);
      break;
    case "media_pause":
      media_pause(element);
      break;
    case "media_stop":
      media_stop(element);
      break;
    case "media_seek":
      media_seek(element, msg.time);
      break;
  }
};

function sendPong(id) {
  var msg = {
    event: "pong",
    id: id
  };
  ws.send(JSON.stringify(msg));
}

function sendOnLoaded(id) {
  var msg = {
    event: "onloaded",
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

function sendSliderPos(id, pos) {
  var msg = {
    event: "sliderpos",
    id: id,
    pos: pos
  };
  ws.send(JSON.stringify(msg));
}

function sendMarginLeft(id) {
  var element = document.getElementById(id);
  var msg = {
    event: "marginleft",
    id: id,
    m: Math.floor(element.style.marginleft)
  };
  ws.send(JSON.stringify(msg));
}

var splitDragging = null;
var sliderDragging = null;
var popupDragging = null;

function onmouseupBody(event, element) {
  if (splitDragging) {
    splitDragging = null;
  }
  if (sliderDragging) {
    sliderDragging = null;
  }
  if (popupDragging) {
    popupDragging = null;
  }
}

function onmousemoveBody(event, element) {
  if (splitDragging) {
    onmousemoveSplitPanel(event, element);
  }
  if (sliderDragging) {
    onmousemoveSlider(event, element);
  }
  if (popupDragging) {
    onmousemovePopupPanel(event, element);
  }
}

function onmousedownBody(event, element) {
  var msg = {
    event: "mousedown",
    id: element.id,
    p: event.clientX + "," + event.clientY
  };
  ws.send(JSON.stringify(msg));
}

function onresizeBody(event, element) {
  if (ws === null) return;
  sendSize('body');
  if (root !== null) {
    root.dispatchEvent(new Event('resize'));
  }
}

function onresizePanel2(event, element, child) {
  child.style.width = getWidth(element);
  child.style.height = getHeight(element);
  child.dispatchEvent(new Event('resize'));
}

function onresizePanel(event, element, childid) {
  var child = document.getElementById(childid);
  onresizePanel2(event, element, child);
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

function onCheckBoxChange(event, element) {
  var msg = {
    event: "changed",
    id: element.id,
    on: element.checked
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
  var node;
  for(i = 0;i < cnt;i++) {
    node = nodes[i];
    if (i === idx) {
      node.classList.add("tabcontentshown");
      node.classList.remove("tabcontenthidden");
    } else {
      node.classList.add("tabcontenthidden");
      node.classList.remove("tabcontentshown");
    }
  }

  onresizeTabPanel(event, tabsid);

  if (rowid === null) return;

  var row = document.getElementById(rowid);
  nodes = row.childNodes;
  cnt = nodes.length;
  for(i = 0;i < cnt;i++) {
    node = nodes[i];
    if (i === idx) {
      node.classList.add("tabactive");
      node.classList.remove("tabinactive");
    } else {
      node.classList.add("tabinactive");
      node.classList.remove("tabactive");
    }
  }
}

function onresizeTabPanel(event, tabsid) {
  var tabs = document.getElementById(tabsid);
  var nodes = tabs.childNodes;
  var cnt = nodes.length;
  var maxWidth = tabs.offsetWidth;
  var maxHeight = tabs.offsetHeight;
  var width, height;
  for(i = 0;i < cnt;i++) {
    width = nodes[i].offsetWidth;
    if (width > maxWidth) maxWidth = width;
    height = nodes[i].offsetHeight;
    if (height > maxHeight) maxHeight = height;
  }
  tabs.style.width = maxWidth;
  tabs.style.height = maxHeight;
}

function onresizeSplitPanelWidth(event, element, id1, id2, id3) {
  var element1 = document.getElementById(id1);
  var element2 = document.getElementById(id2);
  var element3 = document.getElementById(id3);
  var width = getWidth(element);
  var width1 = element1.offsetWidth;
  var width2 = element2.offsetWidth;
  var width3 = width - width1 - width2;
  var height = getHeight(element);
  element3.style.width = width3 + "px";
  element3.style.height = height + "px";
  element3.parentElement.style.width = width3 + "px";
  element3.parentElement.style.height = height + "px";
  element3.dispatchEvent(new Event('resize'));
}

function onresizeSplitPanelHeight(event, element, id1, id2, id3) {
  var element1 = document.getElementById(id1);
  var element2 = document.getElementById(id2);
  var element3 = document.getElementById(id3);
  var height = getHeight(element);
  var height1 = element1.offsetHeight;
  var height2 = element2.offsetHeight;
  var height3 = height - height1 - height2;
  var width = getWidth(element);
  element3.style.width = width + "px";
  element3.style.height = height3 + "px";
  element3.parentElement.style.width = width + "px";
  element3.parentElement.style.height = height3 + "px";
  element3.dispatchEvent(new Event('resize'));
}

function onmousedownSplitPanel(event, element, id1, id2, id3, top, dir) {
  event.preventDefault();
  var top_element = document.getElementById(top);
  var element1 = document.getElementById(id1);
  var element2 = document.getElementById(id2);
  var element3 = document.getElementById(id3);
  splitDragging = {
    //mouse coords
    mouseX: event.clientX,
    mouseY: event.clientY,
    //current size
    startX: element1.offsetWidth,
    startY: element1.offsetHeight,
    //elements
    top_element: top_element,
    element: element,
    element1: element1,
    element2: element2,
    element3: element3,
    //ids
    top : top,
    id1 : id1,
    id2 : id2,
    id3 : id3,
    //direction
    dir: dir
  };
}

function onmousemoveSplitPanel(event, element) {
  switch (splitDragging.dir) {
    case 'h':
      var height = splitDragging.startY + (event.clientY - splitDragging.mouseY);
      splitDragging.element1.style.height = height + "px";  //c
      splitDragging.element1.parentElement.style.height = height + "px";  //t
      onresizePanel2(new Event('resize'), splitDragging.element1, splitDragging.element1.firstChild);
      onresizeSplitPanelHeight(new Event('resize'), splitDragging.top_element, splitDragging.id1, splitDragging.id2, splitDragging.id3);
      break;
    case 'v':
      var width = splitDragging.startX + (event.clientX - splitDragging.mouseX);
      splitDragging.element1.style.width = width + "px";  //c
      splitDragging.element1.parentElement.style.width = width + "px";  //t
      onresizePanel2(new Event('resize'), splitDragging.element1, splitDragging.element1.firstChild);
      onresizeSplitPanelWidth(new Event('resize'), splitDragging.top_element, splitDragging.id1, splitDragging.id2, splitDragging.id3);
      break;
  }
}

function onmousedownSlider(event, element, sliderid, dir, max) {
  event.preventDefault();
  sliderDragging = {
    //mouse coords
    mouseX: event.clientX,
    mouseY: event.clientY,
    //current size
    startX: parseInt(element.style.marginLeft),
    startY: parseInt(element.style.marginTop),
    //direction
    dir: dir,
    //max
    max: max,
    //elements
    element: element,
    sliderid: sliderid
  };
}

function onmousemoveSlider(event, element) {
  var pos;
  if (sliderDragging.dir === "h") {
    pos = sliderDragging.startX + (event.clientX - sliderDragging.mouseX);
    if (pos > sliderDragging.max) pos = sliderDragging.max;
    if (pos < 0) pos = 0;
    sliderDragging.element.style.marginLeft = pos + "px";
  } else {
    pos = sliderDragging.startY + (event.clientY - sliderDragging.mouseY);
    if (pos > sliderDragging.max) pos = sliderDragging.max;
    if (pos < 0) pos = 0;
    sliderDragging.element.style.marginTop = pos + "px";
  }
  sendSliderPos(sliderDragging.sliderid, pos);
}

function onmousedownPopupPanel(event, element) {
  event.preventDefault();
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

function enableDragg(_id, _type, _x1, _y1, _x2, _y2) {
  var pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;
  var id = _id;
  var x1 = _x1, y1 = _y1, x2 = _x2, y2 = _y2;
  var type = _type;
  var element = document.getElementById(id);
  element.onmousedown = dragMouseDown;

  function dragMouseDown(e) {
    e.preventDefault();
    // get the mouse cursor position at start of drag
    pos3 = e.clientX;
    pos4 = e.clientY;
    document.onmouseup = stopDragElement;
    // call a function whenever the cursor moves
    document.onmousemove = elementDrag;
  }

  function elementDrag(e) {
    e.preventDefault();
    // calculate the new cursor position
    pos1 = pos3 - e.clientX;
    pos2 = pos4 - e.clientY;
    pos3 = e.clientX;
    pos4 = e.clientY;
    // set the element's new position
    var x = (element.offsetLeft - pos1);
    var y = (element.offsetTop - pos2);
    // perform bounds
    if (x1 !== -1) {
      if (x < x1) x = x1;
    }
    if (y1 !== -1) {
      if (y < y1) y = y1;
    }
    if (x2 !== -1) {
      if (x > x2) x = x2;
    }
    if (y2 !== -1) {
      if (y > y2) y = y2;
    }
    element.style.left = x + "px";
    element.style.top = y + "px";
//    sendPos(id);  //use onMoved() event instead
  }

  function stopDragElement() {
    // stop moving when mouse button is released
    document.onmouseup = null;
    document.onmousemove = null;
  }
}

var rect = {};

function onMouseDownCanvas(event, element) {
  rect.x1 = event.offsetX;
  rect.y1 = event.offsetY;
  rect.drag = true;
}

function onMouseUpCanvas(event, element) {
  if (rect.drag) {
    var msg = {
      event: "drawrect",
      id: element.id,
      x: rect.x1,
      y: rect.y1,
      w: rect.w,
      h: rect.h
    };
    ws.send(JSON.stringify(msg));
    rect.drag = false;
    var ctx = element.getContext('2d');
    ctx.clearRect(0,0,element.width,element.height);
  }
}

function onMouseMoveCanvasDrawRect(event, element) {
  if (rect.drag) {
    rect.w = event.offsetX - rect.x1;
    rect.h = event.offsetY - rect.y1;
    var ctx = element.getContext('2d');
    ctx.clearRect(0,0,element.offsetWidth,element.offsetHeight);
    ctx.setLineDash([]);
    ctx.strokeStyle = "#FF0000";
    ctx.strokeRect(rect.x1, rect.y1, rect.w, rect.h);
    console.log("rect=" + rect.x1 + "," + rect.y1 + "," + rect.w + "," + rect.h);
  }
}

setTimeout(connect, delay);
