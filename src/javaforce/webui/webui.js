var ws;

var bindata;

var body = document.getElementById('body');
var orghtml = body.innerHTML;
var temp = document.createElement('div');

var delay = 500;

var audioCtx = new AudioContext();
var audioOscillator;
var audioOscillatorFreq;

function alarm() {
  if (audioOscillator === null) return;
  if (audioOscillatorFreq === 440) {
    audioOscillatorFreq = 660;
  } else {
    audioOscillatorFreq = 440;
  }
  audioOscillator.frequency.value = audioOscillatorFreq;
  setTimeout(alarm, 1000);
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
  console.log("event:" + msg.event + " to " + msg.id);
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
    case "setselected":
      element.selected = msg.state;
      break;
    case "ping":
      sendPong(msg.id);
      break;
    case "onresize":
      element.onresize();
      break;
    case "audio-alarm-start":
      audioOscillator = audioCtx.createOscillator();
      audioOscillator.type = 'square';
      audioOscillatorFreq = 440;
      audioOscillator.frequency.value = audioOscillatorFreq;
      audioOscillator.connect(audioCtx.destination);
      audioOscillator.start();
      setTimeout(alarm, 1000);
      break;
    case "audio-alarm-stop":
      audioOscillator.stop();
      audioOscillator = null;
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
  if (ws == null) return;
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

function onCheckBoxChange(event, element) {
  var msg = {
    event: "changed",
    id: element.id,
    on: element.on
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
  var maxWidth = tabs.clientWidth;
  var maxHeight = tabs.clientHeight;
  var width, height;
  for(i = 0;i < cnt;i++) {
    width = nodes[i].clientWidth;
    if (width > maxWidth) maxWidth = width;
    height = nodes[i].clientHeight;
    if (height > maxHeight) maxHeight = height;
  }
  tabs.style.width = maxWidth;
  tabs.style.height = maxHeight;
}

function onresizeSplitDividerWidth(event, element, id1, id2, id3) {
  var element1 = document.getElementById(id1);
  var element2 = document.getElementById(id2);
  var element3 = document.getElementById(id3);
  var width1 = element1.clientWidth;
  var width2 = element2.clientWidth;
  var maxWidth = width1;
  if (width2 > width1) maxWidth = width2;
  element3.style.width = maxWidth + "px";
}

function onresizeSplitDividerHeight(event, element, id1, id2, id3) {
  var element1 = document.getElementById(id1);
  var element2 = document.getElementById(id2);
  var element3 = document.getElementById(id3);
  var height1 = element1.clientHeight;
  var height2 = element2.clientHeight;
  var maxHeight = height1;
  if (height2 > height1) maxHeight = height2;
  element3.style.height = maxHeight + "px";
}

function onmousedownSplitPanel(event, element, id1, id2) {
  event.preventDefault();
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
  var width = splitDragging.startX + (event.clientX - splitDragging.mouseX);
  splitDragging.element1.style.width = width + "px";
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
  if (sliderDragging.dir == "h") {
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

setTimeout(connect, delay);
