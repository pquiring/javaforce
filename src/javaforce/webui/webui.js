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
    case "sethtml":
      comp.innerHTML = msg.html;
      break;
    case "setsrc":
      comp.src = msg.src;
      break;
    case "gettext":
      sendText(msg.comp, comp.innerHTML);
      break;
    case "settext":
      comp.innerHTML = msg.text;
      break;
  }
};

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

function openTab(tab, idx) {
  var tabs = document.getElementById(tab.id + "_tabs");
  var nodes = tabs.childNodes;
  var cnt = nodes.length;
  for(i = 0;i < cnt;i++) {
    if (i === idx) {
      nodes[i].className = "tabcontentshown";
    } else {
      nodes[i].className = "tabcontenthidden";
    }
  }

  var tabs2 = document.getElementById(tab.id + "_row");
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
