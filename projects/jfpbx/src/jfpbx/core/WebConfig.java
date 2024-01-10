package jfpbx.core;

/**
 * WebConfig service for jfPBX
 *
 * @author pquiring
 *
 * Created : Sept 14, 2013
 */

import jfpbx.db.ExtensionRow;
import jfpbx.db.Database;
import jfpbx.db.RouteRow;
import jfpbx.db.TrunkRow;
import jfpbx.db.RouteTableRow;
import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.service.*;

public class WebConfig implements WebHandler {
  public static int http_port, https_port, sip_port;
  public static boolean hideAdmin, disableWebRTC;
  private WebRTC webrtc;
  private WebServer http, https;

  public boolean start() {
    JFLog.log("Starting Web Server on port " + http_port);
    http = new WebServer();
    http.start(this, http_port, false);
    if (new File(Paths.etc + "jfpbx.key").exists()) {
      System.setProperty("javax.net.ssl.keyStore", Paths.etc + "jfpbx.key");
      System.setProperty("javax.net.ssl.keyStorePassword", "password");
      JFLog.log("Starting Web Server on port " + https_port + " (secure)");
      https = new WebServer();
      https.start(this, https_port, true);
      if (!disableWebRTC) startWebRTC();
    }
    return true;
  }

  private void startWebRTC() {
    webrtc = new WebRTC();
    webrtc.init();
    http.setWebSocketHandler(webrtc);
    if (https != null) https.setWebSocketHandler(webrtc);
  }

  public void stop() {
    if (http != null) {
      http.stop();
      http = null;
    }
    if (https != null) {
      https.stop();
      https = null;
    }
    if (webrtc != null) {
//      webrtc.stop();
      webrtc = null;
    }
  }

  public void doPost(WebRequest req, WebResponse res) {
    try {doRequest(req, res);} catch (Exception e) {JFLog.log(e);}
  }

  public void doGet(WebRequest req, WebResponse res) {
    try {doRequest(req, res);} catch (Exception e) {JFLog.log(e);}
  }

  private HashMap<String, byte[]> cache = new HashMap<String, byte[]>();

  public void doStatic(WebRequest req, WebResponse res) throws Exception {
    String url = req.getURL();
    if (url.equals("/favicon.ico")) {
      url = "/static/jfpbx.ico";
    }
    int idx = url.lastIndexOf('/');
    String file = url.substring(idx+1);
    byte data[];
    if (file.endsWith(".class")) {
      res.setStatus(404, "Not Found");
      return;
    }
    if (file.endsWith(".css")) {
      res.setContentType("text/css");
    }
    else if (file.endsWith(".js")) {
      res.setContentType("text/javascript");
    }
    else if (file.endsWith(".png")) {
      res.setContentType("image/png");
    }
    else if (file.endsWith(".ico")) {
      res.setContentType("image/x-icon");
    }
    else if (file.endsWith(".txt")) {
      res.setContentType("text/plain");
    }
    data = (byte[])cache.get(file);
    if (data != null) {
      res.getOutputStream().write(data);
      return;
    }
    data = JF.readAll(getClass().getClassLoader().getResourceAsStream(file));
    if (data == null || data.length == 0) {
      res.setStatus(404, "Not Found");
      return;
    }
    cache.put(file, data);
    res.getOutputStream().write(data);
  }

  private void writeHeaders(OutputStream os) throws Exception {
    StringBuilder html = new StringBuilder();
    html.append("<html>");
    html.append("<head>");
    html.append("  <title>jfPBX</title>");
    html.append("  <link rel=stylesheet href='/static/style.css' type='text/css'>");
    html.append("  <script type='text/javascript' src='/static/style.js'></script>");
    html.append("</head>");
    html.append("<body leftmargin=1 rightmargin=1 topmargin=1 bottommargin=1 vlink=ffffff link=ffffff alink=ffffff width=100% height=100%>");
    os.write(html.toString().getBytes());
  }

  public void doIndex(WebRequest req, WebResponse res) throws Exception {
    writeHeaders(res.getOutputStream());
    StringBuilder html = new StringBuilder();
    if (!hideAdmin) {
      html.append("<div style='float:right;'><a href='/login'>Admin</a></div>");
    }
    html.append("<center>");
    html.append("<img src='/static/logo.png'><br>");
    html.append("<br>");
    if (!disableWebRTC) {
      html.append("<t3>WebRTC Conferencing (experimental)</t3><br>");
      html.append("<form action=webrtc1 method=get>");
      html.append("Room Number:<input name=room size=10>");
      html.append("<input type=submit value='Enter Room'> (2 per room)<br><br><br>");
      html.append("</form>");
      html.append("<t3>WebRTC Phone (experimental)</t3><br>");
      html.append("<form action='https://" + req.getHost() + ":" + https_port + "/webrtc2' method=post>");
      html.append("Your Number:<input name=user size=10><br>");
      html.append("Password:<input type=password name=pass size=10><br>");
      html.append("Dial Number:<input name=dial size=10><br>");
      html.append("<input type=submit value='Make Call'>");
      html.append("</form>");
    }
    res.getOutputStream().write(html.toString().getBytes());
  }

  public String ValidUser(String user, String passmd5) {
    String res = Database.getUserPassword(user);
    if (res == null) return "Database not setup";
    return (res.equalsIgnoreCase(passmd5) ? "ok" : "Invalid Login");
  }

  public void doLogin(WebRequest req, WebResponse res) throws Exception {
    writeHeaders(res.getOutputStream());
    String query = req.getQueryString();
    String args[];
    if (query != null) args = query.split("&"); else args = new String[0];
    String user = "", pass = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "user": user = value; break;
        case "pass": pass = value; break;
      }
    }
    String msg = "";
    if ((user.length() > 0 && pass.length() > 0)) {
      //attempt login
      MD5 md5 = new MD5();
      md5.init();
      md5.add(pass.getBytes(),0,pass.length());
      String passmd5 = new String(md5.byte2char(md5.done()));
      //check database
      msg = ValidUser(user, passmd5);
      if (msg.equals("ok")) {
        req.session.setAttribute("id", user);
        res.sendRedirect("/admin");
        return;
      }
    }
    StringBuilder html = new StringBuilder();

    html.append("<center>");
    html.append("<div style='background-color: #dddddd; width: 320px;'>");
    if (msg.length() > 0) {
      html.append("<font color=#ff0000>");
      html.append(msg);
      html.append("<br></font>");
    }
    html.append("<form action='/login'>");
    html.append("Username : <input name=user><br>");
    html.append("Password : <input type=password name=pass><br>");
    html.append("<input type=submit value='Login'>");
    html.append("</form>");
    html.append("</div>");

    html.append("<br><br>");

    res.getOutputStream().write(html.toString().getBytes());
  }

  public void doLogout(WebRequest req, WebResponse res) throws Exception {
    req.session.setAttribute("id", null);
    res.sendRedirect("/");
  }

  private String doAdminPage(String args[]) {
    String verb = "", current = "", p1 = "", p2 = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "current": current = value; break;
        case "p1": p1 = value; break;
        case "p2": p2 = value; break;
      }
    }
    String msg = "";
    if (verb.equals("changepass")) {
      if (!p1.equals(p2)) {
        msg = "Passwords do not match!";
      } else if (p1.length() < 5) {
        msg = "Password too short (min = 5)";
      } else {
        MD5 md5 = new MD5();
        md5.init();
        md5.add(current.getBytes(),0,current.length());
        String currentmd5 = new String(md5.byte2char(md5.done()));
        String storedmd5 = Database.getUserPassword("admin");
        if ((storedmd5 == null) || (!storedmd5.equals(currentmd5))) {
          msg = "Incorrect current password";
        } else {
          md5.init();
          md5.add(p1.getBytes(),0,p1.length());
          String passmd5 = new String(md5.byte2char(md5.done()));
          if (Database.setUserPassword("admin", passmd5)) {
            msg = "Unable to change password (DB error)";
          } else {
            msg = "Password changed";
          }
        }
      }
    }
    StringBuilder html = new StringBuilder();
    html.append("<center>");
    html.append("Change Admin Password:<br>");
    html.append("<div style='background-color: #dddddd; width: 320px;'>");
    if (msg.length() > 0) {
      html.append("<font color=#ff0000>");
      html.append(msg + "<br>");
      html.append("</font>");
    }
    html.append(form("core", "admin"));
    html.append("<input type=hidden name=verb value='changepass'>");
    html.append("<table>");
    html.append("<tr><td nowrap>Current Password: </td><td> <input type=password name=current></td></tr>");
    html.append("<tr><td nowrap>New Password : </td><td> <input type=password name=p1></td></tr>");
    html.append("<tr><td nowrap>Confirm Password : </td><td> <input type=password name=p2></td></tr>");
    html.append("</table>");
    html.append("<input type=submit value=Change>");
    html.append("</form>");
    html.append("</div>");

    return html.toString();
  }

  private String doBlankPage(String args[]) {
    return "Please select an option on the left.";
  }

  private String doConfPage(String args[]) {
    return "Conferences are special IVRs.  To create a conference, create a new IVR and load the preset \"Conference\".<br>\n" +
           "To create a video conference select the \"Video Conference\" preset.<br>\n" +
           "Then change the $adminpass and $userpass variables for the admin and user pass codes.<br>\n";
  }

  private String doExtsPage(String args[]) {
    String verb = "", number = "", editext = "", cloneStart = "", cloneCount = "", display = "", cid = "", pass = "", routetable = "default", msg = "", sure = "", vm = "", vmpass = "";
    String email = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "ext": number = value; break;
        case "editext": editext = value; break;
        case "cloneStart": cloneStart = value; break;
        case "cloneCount": cloneCount = value; break;
        case "display": display = value; break;
        case "cid": cid = value; break;
        case "pass": pass = value; break;
        case "routetable": routetable = value; break;
        case "sure": sure = value; break;
        case "vm": vm = value; break;
        case "vmpass": vmpass = value; break;
        case "email": email = value; break;
      }
    }
    number = numbersOnly(number);
    cid = numbersOnly(cid);
    vmpass = numbersOnly(vmpass);
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        Database.deleteExtension(number);
        msg = "Extension deleted";
      } else {
        msg = "Please confirm delete action";
      }
      number = "";
    }
    if (verb.equals("view")) {
      editext = number;
      ExtensionRow ext = Database.getExtension(number);
      if (ext != null) {
        display = ext.display;
        cid = ext.cid;
        pass = ext.password;
        routetable = ext.routetable;
        vm = ext.voicemail ? "true" : "false";
        vmpass = ext.voicemailpass;
        email = ext.email;
      }
    }
    if (verb.equals("add") || verb.equals("edit")) {
      if (number.length() == 0)  {
        msg = "Invalid extension number";
      } else {
        if (verb.equals("add") && Database.extensionExists(number)) {
          msg = "Destination already exists with that number";
        }
      }
      if (display.length() == 0)  {
        msg = "Invalid display name";
      }
      if (pass.length() == 0)  {
        msg = "Invalid password";
      }
      if (msg.length() > 0) {
        if (verb.equals("add")) verb = ""; else verb = "view";
      }
    }
    if (verb.equals("add")) {
      ExtensionRow ext = new ExtensionRow();
      ext.number = number;
      ext.display = display;
      ext.cid = cid;
      ext.password = pass;
      ext.routetable = routetable;
      ext.voicemail = vm.equals("true");
      ext.voicemailpass = vmpass;
      ext.email = email;
      Database.addExtension(ext);
      if (msg.length() == 0) {
        msg = "Extension added";
        number = "";
        display = "";
        cid = "";
        pass = "";
        routetable = "default";
        vm = "";
        vmpass = "";
        email = "";
      }
    }
    if (verb.equals("edit")) {
      ExtensionRow ext = Database.getExtension(number);
      if (ext == null) {
        msg = "Extension not found";
        verb = "view";
      } else {
        ext.display = display;
        ext.cid = cid;
        ext.password = pass;
        ext.routetable = routetable;
        ext.voicemail = vm.equals("true");
        ext.voicemailpass = vmpass;
        ext.email = email;
        Database.saveExtensions();
        if (msg.length() == 0) {
          msg = "Extension edited";
          number = "";
          display = "";
          cid = "";
          pass = "";
          routetable = "default";
          vm = "";
          vmpass = "";
          email = "";
        } else {
          verb = "view";
        }
      }
    }
    if (verb.equals("clone")) {
      verb = "";
      ExtensionRow ext = Database.getExtension(number);
      cid = ext.cid;
      pass = ext.password;
      routetable = ext.routetable;
      vm = ext.voicemail ? "true" : "false";
      vmpass = ext.voicemailpass;
      email = ext.email;
      try {
        int start = Integer.valueOf(cloneStart);
        int count = Integer.valueOf(cloneCount);
        if (count <=0 || count > 1000) throw new Exception("max 1000 clones");
        int added = 0;
        for(int idx=start;count > 0;count--,idx++) {
          ext = new ExtensionRow();
          number = "" + idx;
          ext.number = number;
          display = "" + idx;
          ext.display = display;
          ext.routetable = routetable;
          ext.voicemail = vm.equals("true");
          ext.voicemailpass = vmpass;
          ext.email = email;
          Database.addExtension(ext);
          added++;
        }
        msg = "Cloning complete (" + added + " extensions added)";
      } catch (Exception e) {
        msg = "Cloning failed:" + e.toString();
      }
      number = "";
      display = "";
      cid = "";
      pass = "";
      routetable = "default";
      vm = "";
      vmpass = "";
      email = "";
    }
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font><br>");
    if (verb.equals("view")) {
      html.append(form("core", "exts"));
      html.append("<input type=hidden name=verb value=del><input type=hidden name=ext value=" + SQL.quote(number) + ">");
      html.append("<input type=submit value='Delete Extension'> <input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "exts"));
    if (verb.equals("cloneForm")) {
      html.append("<input type=hidden name=verb value=clone>");
      html.append("<table>");
      html.append("<tr><td>Extension #:</td><td><input name=ext value=" + SQL.quote(number) + " readonly></td></tr>");
      html.append("<tr><td>Start:</td><td><input name=cloneStart></td></tr>");
      html.append("<tr><td>Count:</td><td><input name=cloneCount></td></tr>");
      html.append("<tr><td colspan=2>NOTE : Existing extension in clone range may be altered!</td></tr>");
      html.append("</table>");
      html.append("<input type=submit value=Clone>");
    } else {
      html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
      if (verb.equals("view")) html.append("<input type=hidden name=editext value=" + SQL.quote(editext) + ">");
      html.append("<table>");
      html.append("<tr><td>Extension #:</td><td><input name=ext value=" + SQL.quote(number) + "></td><td>");
      if (verb.equals("view")) {
        html.append(link("core", "exts", "verb=cloneForm&ext=" + editext, "Clone"));
      }
      html.append("</td></tr>");
      html.append("<tr><td>Display Name:</td><td><input name=display value=" + SQL.quote(display) + "></td><td>(usually same as extension #)</td></tr>");
      html.append("<tr><td>Outbound Caller ID #:</td><td><input name=cid value=" + SQL.quote(cid) + "></td><td>(optional) (used if call sent to trunk)</td></tr>");
      html.append("<tr><td>SIP Password:</td><td><input name=pass value=" + SQL.quote(pass) + "></td></tr>");
      html.append("<tr><td>Route Table:</td><td><input name=routetable value=" + SQL.quote(routetable) + "></td></tr>");
      html.append("<tr><td>VoiceMail:</td><td><input type=checkbox name=vm " + (vm.equals("true") ? "checked" : "") + "></td></tr>");
      html.append("<tr><td>VM Password:</td><td><input name=vmpass value=" + SQL.quote(vmpass) + "></td></tr>");
      html.append("<tr><td>EMail:</td><td><input name=email value=" + SQL.quote(email) + "></td></tr>");
      html.append("</table>");
      html.append("<input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + ">");
    }
    html.append("</form>");
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    html.append("<div class=menuitem>" + link("core", "exts", "", "Add New") + "</div>");
    ExtensionRow[] exts = Database.getExtensions(ExtensionRow.EXT);
    if (exts != null) {
      for(int a=0;a<exts.length;a++) {
        html.append("<div class=menuitem>" + link("core", "exts", "verb=view&ext=" + exts[a].number, exts[a].display + "&lt;" + exts[a].number + "&gt;") + "</div>");
      }
    }
    html.append("</td></tr>");
    html.append("</table>");

    return html.toString();
  }

  private String doInRoutesPage(String args[]) {
    StringBuilder html = new StringBuilder();

    String verb = "", name = "", cid = "", did = "", dest = "", msg = "", editroute= "", sure = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "route": name = value; break;
        case "editroute": editroute = value; break;
        case "cid": cid = value; break;
        case "did": did = value; break;
        case "dest": dest = value; break;
        case "sure": sure = value; break;
      }
    }
    cid = numbersOnly(cid);
    did = numbersOnly(did);
    dest = numbersOnly(dest);

    if (name.length() == 0) verb = "";
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        Database.deleteInRoute(name);
        msg = "Route deleted";
      } else {
        msg = "Please confirm delete action";
      }
      name = "";
    }
    if (verb.equals("view")) {
      editroute = name;
      RouteRow row = Database.getInRoute(name);
      if (row != null) {
        cid = row.cid;
        did = row.did;
        dest = row.dest;
      }
    }
    if (verb.equals("add") || verb.equals("edit")) {
      if ((cid.length() == 0) && (did.length() == 0)) {
        msg = "You must define DID and/or CID";
        if (verb.equals("add")) verb = ""; else verb = "view";
      }
      if (dest.length() == 0) {
        msg = "Destination must be defined";
        if (verb.equals("add")) verb = ""; else verb = "view";
      }
    }
    if (verb.equals("add")) {
      RouteRow route = new RouteRow();
      route.name = name;
      route.cid = cid;
      route.did = did;
      route.dest = dest;
      Database.addInRoute(route);
      msg = "Route added";
    }
    if (verb.equals("edit")) {
      RouteRow route = Database.getInRoute(name);
      if (route != null) {
        route.cid = cid;
        route.did = did;
        route.dest = dest;
        msg = "Route edited";
        name = "";
        cid = "";
        did = "";
        dest = "";
      } else {
        msg = "Route not found";
        verb = "view";
      }
    }

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) {
      html.append("<font color=#ff0000>" + msg + "</font><br>");
    }
    if (verb.equals("view")) {
      html.append(form("core", "inroutes"));
      html.append("<input type=hidden name=verb value=del><input type=hidden name=route value=" + SQL.quote(name) + "><input type=submit value='Delete route'>");
      html.append("<input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "inroutes"));
    html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
    if (verb.equals("view")) {
      html.append("<input type=hidden name=editroute value=" + SQL.quote(editroute) + ">");
    }
    html.append("<table>");
    html.append("<tr><td> Route Name: </td><td> <input name=route value=" + SQL.quote(name) + "</td></tr>");
    html.append("<tr><td> Dialed # (DID): </td><td> <input name=did value=" + SQL.quote(did) + "></td><td>(optional)</td></tr>");
    html.append("<tr><td> Caller # (CID): </td><td> <input name=cid value=" + SQL.quote(cid) + "></td><td>(optional)</td></tr>");
    html.append("<tr><td> Destination #: </td><td> <input name=dest value=" + SQL.quote(dest) + "></td><td>(Extension, IVR, etc.)</tr>");
    html.append("<tr><td> <input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + "></td></tr>");
    html.append("</table>");
    html.append("</form>");
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    html.append("<div class=menuitem>" + link("core", "inroutes", "", "Add New") + "</div>");
    RouteRow[] routes = Database.getInRoutes();
    if (routes != null) {
      for(int a=0;a<routes.length;a++) {
        cid = routes[a].cid;
        did = routes[a].did;
        dest = routes[a].dest;
        if (cid.length() == 0) cid = "any CID";
        if (did.length() == 0) did = "any DID";
        html.append("<div class=menuitem>" + link("core", "inroutes", "verb=view&route=" + routes[a].name,
          "&lt;" + routes[a].name + "&gt;" + cid + "/" + did) + "</div>");
      }
    }
    html.append("</td></tr></table>");

    return html.toString();
  }

  private String doIVRPage(String args[]) {
    String verb = "", number = "", editext = "", display = "", script = "", sure = "", msg = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "ext": number = value; break;
        case "display": display = value; break;
        case "script": script = value; break;
        case "editext": editext = value; break;
        case "sure": sure = value; break;
      }
    }
    number = numbersOnly(number);
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        Database.deleteIVR(number);
        msg = "IVR deleted";
      } else {
        msg = "Please confirm delete action";
      }
      number = "";
    }
    if (verb.equals("view")) {
      ExtensionRow ivr = Database.getIVR(number);
      editext = number;
      display = ivr.display;
      script = ivr.script;
    }
    if (verb.equals("add") || verb.equals("edit")) {
      if (number.length() == 0)  {
        msg = "Invalid IVR number";
      } else {
        if (verb.equals("add") && Database.extensionExists(number)) {
          msg = "Destination already exists with that number";
        }
      }
      if (display.length() == 0)  {
        msg = "Invalid display name";
      }
      if (msg.length() > 0) {
        if (verb.equals("add")) verb = ""; else verb = "view";
      }
    }
    if (verb.equals("add")) {
      ExtensionRow ivr = new ExtensionRow();
      ivr.number = number;
      ivr.display = display;
      ivr.script = script;
      Database.addIVR(ivr);
      msg = "IVR added";
      number = "";
      display = "";
      script = "";
    }
    if (verb.equals("edit")) {
      ExtensionRow ivr = Database.getIVR(number);
      ivr.display = display;
      ivr.script = script;
      Database.saveIVRs();
      if (msg.length() == 0) {
        msg = "IVR edited";
        number = "";
        display = "";
        script = "";
      }
    }
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) {
      html.append("<font color=#ff0000>" + msg + "</font><br>");
    }
    if (verb.equals("view")) {
      html.append(form("core", "ivrs"));
      html.append("<input type=hidden name=verb value=del><input type=hidden name=ext value=" + SQL.quote(number));
      html.append("><input type=submit value='Delete IVR'>" + "<input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "ivrs"));
    html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
    if (verb.equals("view")) {
      html.append("<input type=hidden name=editext value=" + SQL.quote(editext) + ">");
    }
    html.append("<table>");
    html.append("<tr><td>IVR #:</td><td><input name=ext value=" + SQL.quote(number) + ">");
    html.append("</td></tr>");
    html.append("<tr><td>Name:</td><td><input name=display value=" + SQL.quote(display) + "></td></tr>");
    html.append("<tr><td>Script:</td><td><textarea id=script name=script cols=40 rows=20>" + script + "</textarea></td><td><a href=\"javascript:showHelp('ivr');\">Help</a><br>");
    html.append("Presets:<select id=preset><option>-none-</option><option>Conference</option><option>Video Conference</option></select> <a href=\"javascript:load_preset();\">Load</a></td></tr>");
    html.append("</table>");
    html.append("<input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + ">");
    html.append("</form>");
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    html.append("<div class=menuitem>" + link("core", "ivrs", "", "Add New") + "</div>");
    ExtensionRow[] ivrs = Database.getIVRs();
    if (ivrs != null) {
      for(int a=0;a<ivrs.length;a++) {
        number = ivrs[a].number;
        display = ivrs[a].display;
        script = ivrs[a].script;
        html.append("<div class=menuitem>" + link("core", "ivrs", "verb=view&ext=" + number, display + "&lt;" + number + "&gt;") + "</div>");
      }
    }
    html.append("</td></tr>");
    html.append("</table>");

    return html.toString();
  }

  private String doQueuesPage(String args[]) {
    String verb = "", number = "", editext = "", display = "", agents = "", sure = "", msg = "", message = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "ext": number = value; break;
        case "display": display = value; break;
        case "agents": agents = value.replaceAll("\r\n", ","); break;
        case "message": message = value; break;
        case "editext": editext = value; break;
        case "sure": sure = value; break;
      }
    }
    if (message.length() == 0) message = "acd-wait-for-agent";
    number = numbersOnly(number);
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        Database.deleteQueue(number);
        msg = "Queue deleted";
      } else {
        msg = "Please confirm delete action";
      }
      number = "";
    }
    if (verb.equals("view")) {
      editext = number;
      ExtensionRow queue = Database.getQueue(number);
      display = queue.display;
      agents = queue.agents;
      message = queue.message;
    }
    if (verb.equals("add") || verb.equals("edit")) {
      if (number.length() == 0)  {
        msg = "Invalid Queue number";
      } else {
        if (verb.equals("add") && Database.extensionExists(number)) {
          msg = "Destination already exists with that number";
        }
      }
      if (display.length() == 0)  {
        msg = "Invalid display name";
      }
      if (msg.length() > 0) {
        if (verb.equals("add")) verb = ""; else verb = "view";
      }
    }
    if (verb.equals("add")) {
      ExtensionRow queue = new ExtensionRow();
      queue.number = number;
      queue.display = display;
      queue.agents = agents;
      queue.message = message;
      Database.addQueue(queue);
      if (msg.length() == 0) {
        msg = "Queue added";
        number = "";
        display = "";
        agents = "";
      }
    }
    if (verb.equals("edit")) {
      ExtensionRow queue = Database.getQueue(number);
      if (queue != null) {
        queue.display = display;
        queue.agents = agents;
        queue.message = message;
        Database.saveQueues();
      }
      msg = "Queue edited";
      number = "";
      display = "";
      agents = "";
    }
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) {
      html.append("<font color=#ff0000>" + msg + "</font><br>");
    }
    if (verb.equals("view")) {
      html.append(form("core", "queues"));
      html.append("<input type=hidden name=verb value=del><input type=hidden name=ext value=" + SQL.quote(number));
      html.append("><input type=submit value='Delete Queue'>" + "<input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "queues"));
    html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
    if (verb.equals("view")) {
      html.append("<input type=hidden name=editext value=" + SQL.quote(editext) + ">");
    }
    html.append("<table>");
    html.append("<tr><td>Queue #:</td><td><input name=ext value=" + SQL.quote(number) + ">");
    html.append("</td></tr>");
    html.append("<tr><td>Name:</td><td><input name=display value=" + SQL.quote(display) + "></td></tr>");
    html.append("<tr><td>Agents:</td><td><textarea id=agents name=agents cols=40 rows=20>" + agents.replaceAll(",", "\r\n") + "</textarea> (list agents one per line)</td></tr>");
    html.append("<tr><td>Member Join Message:</td><td>");
    ArrayList<String> wavFiles = new ArrayList<String>();
    try {
      File f = new File(Paths.sounds + Paths.lang + "/");
      File fs[] = f.listFiles();
      if (fs != null) {
        Arrays.sort(fs);
        for(int a=0;a<fs.length;a++) {
          String file = fs[a].getName();
          if (!file.endsWith(".wav")) continue;
          file = file.substring(0, file.length() - 4);
          if (!file.equals("acd-wait-for-agent") && isBuiltinSound(file)) continue;
          wavFiles.add(file);
        }
      }
    } catch (Exception e2) {
      html.append(e2.toString());
    }
    html.append(select("message", message, wavFiles.toArray(new String[0])));
    html.append(" (use " + link("core", "msgs", "", "Messages") + " page to upload new files)</td></tr>");
    html.append("</table>");
    html.append("<input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + ">");
    html.append("</form>");
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    html.append("<div class=menuitem>" + link("core", "queues", "", "Add New") + "</div>");
    ExtensionRow[] queues = Database.getQueues();
    if (queues != null) {
      for(int a=0;a<queues.length;a++) {
        number = queues[a].number;
        display = queues[a].display;
        agents = queues[a].agents;
        message = queues[a].message;
        html.append("<div class=menuitem>" + link("core", "queues", "verb=view&ext=" + number, display + "&lt;" + number + "&gt;") + "</div>");
      }
    }
    html.append("</td></tr>");
    html.append("</table>");

    return html.toString();
  }

  private String doMsgsPage(String args[], WebRequest req) {
    String verb = "", file = "", oldfile = "", newfile = "", sure = "", msg= "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "file": file = value; break;
        case "oldfile": oldfile = value; break;
        case "newfile": newfile = value; break;
        case "sure": sure = value; break;
      }
    }
    if ( (file.indexOf("..") != -1) || (file.indexOf("/") != -1) || (file.indexOf("\\") != -1) ) {
      return "ERROR : invalid filename.";
    }
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        if (isBuiltinSound(file)) {
          msg = "Can not delete built-in message";
        } else {
          try {
            File f = new File(Paths.sounds + Paths.lang + "/" + file + ".wav");
            f.delete();
            msg = "Deleted file : " + f.getName();
          } catch (Exception e1) {
            msg = "Unable to delete file";
          }
        }
      } else {
        msg = "Please confirm delete action";
      }
    }
    if (verb.equals("upload")) {
      try {
        if (WebUpload.isMultipartContent(req)) {
          WebUpload webUpload = new WebUpload();
          WebUpload.WebFile files[] = webUpload.parseRequest(req);
          JFLog.log("files=" + files.length);
          for(int a=0;a<files.length;a++) {
            String fileName = files[a].name;
            JFLog.log("filename=" + fileName);
            if (!fileName.toLowerCase().endsWith(".wav")) {
              msg += "Not a wav:" + fileName + "<br>";
              continue;
            }
            String dirName = Paths.sounds + Paths.lang + "/";
            try {
              File saveTo = new File(dirName + fileName);
              files[a].write(saveTo);
              msg += "Uploaded:" + fileName + "<br>";
            } catch (Exception e3) {
              msg += "Upload failed:" + fileName + ":" + e3 + "<br>";
            }
          }
        } else {
          msg += "No upload data found.";
        }
      } catch (Exception e) {
        msg += e;
      }
    }
    if (verb.equals("rename")) {
      verb = "";
      if (isBuiltinSound(oldfile) || isBuiltinSound(newfile)) {
        msg = "Can not rename build-in sound";
      } else {
        File oldFile = new File(Paths.sounds + Paths.lang + "/" + oldfile + ".wav");
        File newFile = new File(Paths.sounds + Paths.lang + "/" + newfile + ".wav");
        oldFile.renameTo(newFile);
        msg = "File renamed";
      }
    }
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font><br>");
    if (verb.equals("view")) {
      html.append(form("core", "msgs"));
      html.append("<input type=hidden name=verb value=del><input type=hidden name=file value=" + SQL.quote(file));
      html.append("><input type=submit value='Delete Message'>" + "<input type=checkbox name=sure>I'm Sure</form>");
    }
    if (verb.equals("renameForm")) {
      html.append(form("core", "msgs"));
      html.append("Not implemented Yet!<br>");
      html.append("<input type=hidden name=verb value=rename>");
      html.append("<table>");
      html.append("<tr><td>Old Filename:</td><td><input name=oldfile value=" + SQL.quote(oldfile) + " readonly></td></tr>");
      html.append("<tr><td>New Filename:</td><td><input name=newfile></td></tr>");
      html.append("</table>");
      html.append("<input type=submit value=Rename>");
      html.append("</form>");
    } else if (verb.equals("view")) {
      html.append("<table>");
      html.append("<tr><td>Filename:</td><td>" + file);
      html.append(link("core", "msgs", "verb=renameForm&oldfile=" + file, "Rename"));
      html.append("</td></tr>");
      html.append("</table>");
    } else {
      html.append(formUpload("core", "msgs", "verb=upload"));
      html.append("<table>");
      html.append("<tr><td>Filename:</td><td><input type=file name=file></td><td><input type=submit value='Upload'></td></tr>");
      html.append("</table>");
      html.append("</form>");
      html.append("Wav files must be 8000Hz, mono, 16bit in PCM format.<br>");
    }
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    html.append("<div class=menuitem>" + link("core", "msgs", "", "Add New") + "</div>");
    try {
      File f = new File(Paths.sounds + Paths.lang + "/");
      File fs[] = f.listFiles();
      if (fs != null) {
        Arrays.sort(fs);
        for(int a=0;a<fs.length;a++) {
          file = fs[a].getName();
          if (!file.endsWith(".wav")) continue;
          file = file.substring(0, file.length() - 4);
          html.append("<div class=menuitem>" + link("core", "msgs", "verb=view&file=" + file, file + "&lt;" + (isBuiltinSound(file) ? "builtin" : "custom") + "&gt;") + "</div>");
        }
      }
    } catch (Exception e2) {
      html.append(e2.toString());
    }
    html.append("</td></tr>");
    html.append("</table>");

    return html.toString();
  }

  private String doOutRoutesPage(String args[]) {
    String verb = "", table = "", name = "", priority = "", cid = "", patterns = "", trunks = "", msg = "", editroute= "", t1 = "", t2 = "", t3 = "", t4 = "", sure = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "routetable": table = value; break;
        case "route": name = value; break;
        case "priority": priority = value; break;
        case "editroute": editroute = value; break;
        case "cid": cid = value; break;
        case "patterns": patterns = value; break;
        case "t1": t1 = value; break;
        case "t2": t2 = value; break;
        case "t3": t3 = value; break;
        case "t4": t4 = value; break;
        case "sure": sure = value; break;
      }
    }
    cid = numbersOnly(cid);
    trunks = t1;
    if (t2.length() > 0) {if (trunks.length() > 0) trunks += ":"; trunks += t2;}
    if (t3.length() > 0) {if (trunks.length() > 0) trunks += ":"; trunks += t3;}
    if (t4.length() > 0) {if (trunks.length() > 0) trunks += ":"; trunks += t4;}
    priority = numbersOnly(priority);
    if (priority.length() == 0) priority = "0";

    if (table.length() == 0) verb = "";
    if (verb.equals("deltable")) {
      if (table.equals("default")) {
        msg = "Can not delete default table";
      } else {
        Database.deleteOutRouteTable(table);
        msg = "Table deleted";
      }
      table = "";
    }
    if (verb.equals("addtable")) {
      RouteTableRow row = new RouteTableRow();
      row.name = table;
      Database.addOutRouteTable(row);
      msg = "Table added";
    }

    if (name.length() == 0) verb = "";
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        Database.deleteOutRouteTable(table);
        msg = "Route deleted";
      } else {
        msg = "Please confirm delete action";
      }
      name = "";
    }
    if (verb.equals("view")) {
      editroute = name;
      RouteRow row = Database.getOutRoute(table, name);
      if (row != null) {
        priority = row.priority;
        cid = row.cid;
        patterns = row.patterns;
        trunks = row.trunks;
        String lns[] = trunks.split(":");
        if (lns.length > 0) t1 = lns[0];
        if (lns.length > 1) t2 = lns[1];
        if (lns.length > 2) t3 = lns[2];
        if (lns.length > 3) t4 = lns[3];
      }
    }
    if (verb.equals("add")) {
      patterns = patterns.replaceAll("\r\n", ":");
      patterns = patternsOnly(patterns);
      RouteRow row = new RouteRow();
      row.name = name;
      row.priority = priority;
      row.cid = cid;
      row.patterns = patterns;
      row.trunks = trunks;
      Database.addOutRoute(table, row);
      msg = "Route added";
      name = "";
      priority = "";
      cid = "";
      patterns = "";
      trunks = "";
      t1 = "";
      t2 = "";
      t3 = "";
      t4 = "";
    }
    if (verb.equals("edit")) {
      patterns = patterns.replaceAll("\r\n", ":");
      patterns = patternsOnly(patterns);
      RouteRow row = Database.getOutRoute(table, name);
      row.name = name;
      row.priority = priority;
      row.cid = cid;
      row.patterns = patterns;
      row.trunks = trunks;
      Database.saveOutRoutes(table);
      msg = "Route edited";
      name = "";
      priority = "";
      cid = "";
      patterns = "";
      t1 = "";
      t2 = "";
      t3 = "";
      t4 = "";
    }
    TrunkRow[] rows = Database.getTrunks();
    ArrayList<String> strs = new ArrayList<String>();
    for(int a=0;a<rows.length;a++) {
      strs.add(rows[a].name);
    }
    String[] list = strs.toArray(new String[strs.size()]);
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    html.append("<div class=table>");
    html.append("<div class=menucat>Routing Tables</div>");
    RouteTableRow[] tables = Database.getOutRouteTables();
    if (tables != null) {
      for(int a=0;a<tables.length;a++) {
        if (table.equals(tables[a])) {
          html.append("<div class=menuitemselected>");
        } else {
          html.append("<div class=menuitem>");
        }
        html.append(link("core", "outroutes", "routetable=" + tables[a].name, tables[a].name) + "</div>");
      }
    }
    html.append(form("core", "outroutes") + "<input name=routetable><input type=hidden name=verb value=addtable><input type=submit value='Add Table'></form><br>");
    html.append(form("core", "outroutes") + "<input name=routetable><input type=hidden name=verb value=deltable><input type=submit value='Delete Table'></form>");
    html.append("</div>");
    html.append("<hr>");
    if (table.length() > 0) {
      if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font><br>");
      if (verb.equals("view")) {
        html.append(form("core", "outroutes") + "<input type=hidden name=routetable value=" + table);
        html.append("><input type=hidden name=verb value=del><input type=hidden name=route value=" + SQL.quote(name) + "><input type=submit value='Delete route'>");
        html.append("<input type=checkbox name=sure>I'm Sure</form>");
      }
      html.append(form("core", "outroutes"));
      html.append("<input type=hidden name=routetable value=" + SQL.quote(table) + ">");
      html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
      if (verb.equals("view")) {
        html.append("<input type=hidden name=editroute value=" + SQL.quote(editroute) + ">");
      }
      html.append("<table>");
      html.append("<tr><td> Route Name: </td><td> <input name=route value=" + SQL.quote(name) + "></td></tr>");
      html.append("<tr><td> Priority: </td><td> <input name=priority value=" + SQL.quote(priority) + "></td></tr>");
      html.append("<tr><td> Default Caller ID: </td><td> <input name=cid value=" + SQL.quote(cid) + "></td><td>(optional)</td></tr>");
      html.append("<tr><td> Dial Patterns: </td><td> <textarea name=patterns cols=20 rows=10>");
      String lns[] = patterns.split(":");
      for(int a=0;a<lns.length;a++) html.append(lns[a]);
      html.append("</textarea>");
      html.append("</td><td>");
      html.append("If dialed # matches any patterns then this route is used to send call to trunks listed.<br>");
      html.append("If this route is used then dialed # will be modified before sending to trunk.<br>");
      html.append("<pre>");
      html.append(" #=matches exact digit (0-9)\r\n");
      html.append(" X=matches 0-9\r\n");
      html.append(" Z=matches 1-9\r\n");
      html.append(" N=matches 2-9\r\n");
      html.append(" x|y = removes 'x' if 'xy' matches\r\n");
      html.append(" x+y = adds 'x' if 'y' matches\r\n");
      html.append(" .=wildcard (last char only)\r\n");
      html.append("</pre>");
      html.append("</td></tr>");
      html.append("<tr><td> Trunks: </td><td>");
      html.append(select("t1", t1, list) + "<br>");
      html.append(select("t2", t2, list) + "<br>");
      html.append(select("t3", t3, list) + "<br>");
      html.append(select("t4", t4, list) + "<br>");
      html.append("</td></tr>");
      html.append("<tr><td> <input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + "></td></tr>");
      html.append("</table>");
      html.append("</form>");
    }
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    if (table.length() == 0) {
      html.append("<div class=menuitem>Select a Routing Table</div>");
    } else {
      html.append("<div class=menuitem>" + link("core", "outroutes", "routetable=" + table, "Add New") + "</div>");
      RouteRow[] routes = Database.getOutRoutes(table);
      if (routes != null) {
        for(int a=0;a<routes.length;a++) {
          html.append("<div class=menuitem>" + link("core", "outroutes", "routetable=" + table + "&verb=view&route=" + routes[a].name, "&lt;" + routes[a].name + "&gt;") + "</div>");
        }
      }
    }
    html.append("</td></tr></table>");

    return html.toString();
  }

  public String getCfg(String cfg[][], String id) {
    for(int a=0;a<cfg.length;a++) {
      if (cfg[a][0].equals(id)) return cfg[a][1];
    }
    return "";
  }
  public String checked(String in) {
    if (in.equals("true")) return "checked";
    return "";
  }

  private String doSettingsPage(String args[], WebRequest req) {
    String verb = "", port = "", msg = "", anon = "", route = "", rtpmin = "", rtpmax = "", videoCodecs = "";
    String relayAudio = "", relayVideo = "", moh = "";
    String http = "", https = "", hideAdmin = "", disableWebRTC = "";
    String valid = "", dname = "";
    String smtp_server = "", smtp_from_email = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "port": port = value; break;
        case "anon": anon = value; break;
        case "route": route = value; break;
        case "rtpmin": rtpmin = value; break;
        case "rtpmax": rtpmax = value; break;
        case "videoCodecs": videoCodecs = value.toUpperCase(); break;
        case "relayAudio": relayAudio = value; break;
        case "relayVideo": relayVideo = value; break;
        case "moh": moh = value; break;
        case "http": http = value; break;
        case "https": https = value; break;
        case "hideAdmin": hideAdmin = value; break;
        case "disableWebRTC": disableWebRTC = value; break;
        case "valid": valid = value; break;
        case "dname": dname = value; break;
        case "smtp_server": smtp_server = value; break;
        case "smtp_from_email": smtp_from_email = value; break;
      }
    }
    StringBuilder html = new StringBuilder();

    html.append("<font color=#ff0000>");
    html.append(msg);
    html.append("</font>");
    html.append("General Settings:<br>");
    html.append("<div class=table>");
    if (verb.equals("save")) {
      if (anon.equals("on")) anon = "true"; else anon = "false";
      if (route.equals("on")) route = "true"; else route = "false";
      if (relayAudio.equals("on")) relayAudio = "true"; else relayAudio = "false";
      if (relayVideo.equals("on")) relayVideo = "true"; else relayVideo = "false";
      if (hideAdmin.equals("on")) hideAdmin = "true"; else hideAdmin = "false";
      if (disableWebRTC.equals("on")) disableWebRTC = "true"; else disableWebRTC = "false";
      int irtpmin, irtpmax;
      try {
        irtpmin = Integer.valueOf(rtpmin);
        if (irtpmin % 2 == 1) irtpmin++;  //must be even
        irtpmax = Integer.valueOf(rtpmax);
        if (irtpmax % 2 == 1) irtpmax++;  //must be even
        if ((irtpmin < 1024) || (irtpmin > 65534-1000)) irtpmin = 32768;
        if ((irtpmax < 1024) || (irtpmax > 65535)) irtpmax = 65535;
        if ((irtpmin > irtpmax) || (irtpmax - irtpmin < 1000)) {
          irtpmin = 32768;
          irtpmax = 65535;
        }
      } catch (Exception e) {
        JFLog.log(e);
        irtpmin = 32768;
        irtpmax = 65535;
      }
      Database.setConfig("port", port);
      Database.setConfig("anonymous", anon);
      Database.setConfig("route", route);
      Database.setConfig("rtpmin", rtpmin);
      Database.setConfig("rtpmax", rtpmax);
      Database.setConfig("videoCodecs", videoCodecs);
      Database.setConfig("relayAudio", relayAudio);
      Database.setConfig("relayVideo", relayVideo);
      Database.setConfig("moh", moh);
      Database.setConfig("http", http);
      Database.setConfig("https", https);
      Database.setConfig("hideAdmin", hideAdmin);
      Database.setConfig("disableWebRTC", disableWebRTC);
      Database.setConfig("smtp_server", smtp_server);
      Database.setConfig("smtp_from_email", smtp_from_email);
      msg = "Settings saved";
    }
    //NOTE : The -debug option is important to prevent KeyTool from executing System.exit()
    if (verb.equals("sslSelf")) {
      if (KeyMgmt.keytool(new String[] {
        "-genkey", "-debug", "-alias", "jfpbx", "-keypass", "password", "-storepass", "password",
        "-keystore", Paths.etc + "jfpbx.key", "-validity", valid, "-dname", dname,
        "-keyalg" , "RSA", "-keysize", "2048"
      })) {
        msg = "Generated self-signed SSL Certificate";
      } else {
        msg = "KeyTool Error";
      }
    }
    html.append("<font color=#ff0000>" + msg + "</font><br>");
    html.append(form("core", "settings"));

    html.append("<input type=hidden name='verb' value='save'>");
    html.append("SIP Port : <input name=port value=" + Database.getConfig("port") + "><br>");
    html.append("RTP Port Min : <input name=rtpmin value=" + Database.getConfig("rtpmin") + "> (1024-64534) (default:32768)<br>");
    html.append("RTP Port Max : <input name=rtpmax value=" + Database.getConfig("rtpmax") + "> (2023-65535) (default:65535)<br>");
    html.append("RTP Port Range must include at least 1000 ports and start on an even port number.<br>");
    html.append("<input type=checkbox name=anon " + checked(Database.getConfig("anonymous")) + "> Anonymous Inbound Calls (allows calls from any source to extensions, voicemail, IVRs, etc.)<br>");
    html.append("<input type=checkbox name=route " + checked(Database.getConfig("route")) + "> Route Calls (route calls from one trunk to another) [not implemented yet]<br>");
    html.append("Video Conference Codecs : <input name=videoCodecs value=" + Database.getConfig("videoCodecs") + "> [comma list : H263,H263-1998,H263-2000,H264,VP8]<br>");
    html.append("<input type=checkbox name=relayAudio " + checked(Database.getConfig("relayAudio")) + "> Relay Audio Media (recommended)<br>");
    html.append("<input type=checkbox name=relayVideo " + checked(Database.getConfig("relayVideo")) + "> Relay Video Media (optional)<br>");
    html.append("Music on Hold : ");
    ArrayList<String> wavFiles = new ArrayList<String>();
    try {
      File f = new File(Paths.sounds + Paths.lang + "/");
      File fs[] = f.listFiles();
      if (fs != null) {
        Arrays.sort(fs);
        for(int a=0;a<fs.length;a++) {
          String file = fs[a].getName();
          if (!file.endsWith(".wav")) continue;
          file = file.substring(0, file.length() - 4);
          if (isBuiltinSound(file)) continue;
          wavFiles.add(file);
        }
      }
    } catch (Exception e2) {
      html.append(e2.toString());
    }
    html.append(select("moh", Database.getConfig("moh"), wavFiles.toArray(new String[0])));
    html.append(" (use " + link("core", "msgs", "", "Messages") + " page to upload new files)<br>");
    html.append("Web HTTP Port : <input name=http value=" + Database.getConfig("http") + "><br>");
    html.append("Web HTTPS Port : <input name=https value=" + Database.getConfig("https") + "><br>");
    html.append("<input type=checkbox name=hideAdmin " + checked(Database.getConfig("hideAdmin")) + "> Hide Admin Link on Home Page<br>");
    html.append("<input type=checkbox name=disableWebRTC " + checked(Database.getConfig("disableWebRTC")) + "> Disable WebRTC support<br>");
    html.append("SMTP Server : <input name=smtp_server value=" + Database.getConfig("smtp_server") + "><br>");
    html.append("SMTP From Email : <input name=smtp_from_email value=" + Database.getConfig("smtp_from_email") + "><br>");
    html.append("<br>");
    html.append("<input type=submit value='Save'>");
    html.append("</form>");
    html.append("<br><br><br>");
    html.append("Secure Web SSL Cerificate:<br><br>");
    html.append("Generate self-signed Key/Certificate Pair:<br>");
    html.append(form("core", "settings"));
    html.append("dname:<input name=dname value='CN=" + req.getHost() + ", OU=jfpbx, O=JavaForce, C=CA' size=50><br>");
    html.append("valid(days):<input name=valid value='3650' size=10><br>");
    html.append("<input type=hidden name='verb' value='sslSelf'>");
    html.append("<input type=submit value='Generate'>");
    html.append("</form><br><br>");
    html.append("All options on this page will not take effect until the server is restarted.<br>");
    html.append("</div>");

    return html.toString();
  }

  private String doStatusPage(String args[]) {
    String verb = "", msg = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
      }
    }
    StringBuilder html = new StringBuilder();

    if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font>");
    html.append("<div class=table>");
    html.append("<div class=notice>Notices</div>");
    String res = Database.getConfig("version");
    if (res == null) {
      html.append("<div class=noticeitem>");
      html.append(link("core", "status", "verb=initdb", "Database not initialized"));
      html.append("</div>");
    } else if (!res.equals(Database.dbVersion)) {
      html.append("<div class=noticeitem>");
      html.append(link("core", "status", "verb=upgradedb", "Database upgrade required"));
      html.append("</div>");
    }
    res = Database.getUserPassword("admin");
    if ((res == null) || (res.equals("21232f297a57a5a743894a0e4a801fc3"))) {
      html.append("<div class=noticeitem>");
      html.append(link("core", "admin", "", "Default Admin Password in use"));
      html.append("</div>");
    }
    html.append("</div>");

    return html.toString();
  }

  private boolean validRegister(String reg) {
    // user:pass@host[:port]
    int i1 = reg.indexOf(':');
    if (i1 == -1) return false;
    int i2 = reg.indexOf('@');
    if (i2 == -1) return false;
    String user = reg.substring(0, i1);
    if (user.length() == 0) return false;
    String pass = reg.substring(i1+1, i2);
    if (pass.length() == 0) return false;
    String host = reg.substring(i2+1);
    if (host.length() == 0) return false;
    return true;
  }

  private String doTrunksPage(String args[]) {
    String verb = "", name = "", cid = "", host = "", xip = "", register = "", doregister = "", msg = "", outrules = "", inrules = "", edittrunk= "", sure = "";
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "verb": verb = value; break;
        case "trunk": name = value; break;
        case "edittrunk": edittrunk = value; break;
        case "cid": cid = value; break;
        case "host": host = value; break;
        case "xip": xip = value; break;
        case "register": register = value; break;
        case "doregister": doregister = value; break;
        case "outrules": outrules = value; break;
        case "inrules": inrules = value; break;
        case "sure": sure = value; break;
      }
    }
    cid = numbersOnly(cid);
    if (name.length() == 0) verb = "";
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        Database.deleteTrunk(name);
        msg = "Trunk deleted";
      } else {
        msg = "Please confirm delete action";
      }
      name = "";
    }
    if (verb.equals("view")) {
      edittrunk = name;
      TrunkRow row = Database.getTrunk(name);
      if (row != null) {
        host = row.host;
        xip = row.xip;
        cid = row.cid;
        outrules = row.outrules;
        inrules = row.inrules;
        register = row.register;
        doregister = row.doRegister() ? "on" : "";
      }
    }
    if (verb.equals("add") || verb.equals("edit")) {
      if (!validRegister(register)) {
        msg = "Invalid Register";
        verb = "";
      }
    }
    if (verb.equals("add")) {
      outrules = outrules.replaceAll("\r\n", ":");
      outrules = patternsOnly(outrules);
      inrules = inrules.replaceAll("\r\n", ":");
      inrules = patternsOnly(inrules);
      register = register;
      TrunkRow row = new TrunkRow();
      row.name = name;
      row.host = host;
      row.xip = xip;
      row.cid = cid;
      row.inrules = inrules;
      row.outrules = outrules;
      row.register = register;
      row.setRegister(doregister.equals("on"));
      Database.addTrunk(row);
      msg = "Trunk added";
      name = "";
      host = "";
      xip = "";
      cid = "";
      register = "";
      doregister = "";
      outrules = "";
      inrules = "";
    }
    if (verb.equals("edit")) {
      outrules = outrules.replaceAll("\r\n", ":");
      outrules = patternsOnly(outrules);
      inrules = inrules.replaceAll("\r\n", ":");
      inrules = patternsOnly(inrules);
      TrunkRow row = Database.getTrunk(name);
      if (row != null) {
        row.name = name;
        row.host = host;
        row.xip = xip;
        row.cid = cid;
        row.inrules = inrules;
        row.outrules = outrules;
        row.register = register;
        row.setRegister(doregister.equals("on"));
        Database.saveTrunks();
        msg = "Trunk edited";
        name = "";
        host = "";
        xip = "";
        cid = "";
        register = "";
        doregister = "";
        outrules = "";
        inrules = "";
      } else {
        msg = "Failed to edit";
        verb = "view";
      }
    }
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font><br>");
    if (verb.equals("view")) {
      html.append(form("core", "trunks") + "<input type=hidden name=verb value=del><input type=hidden name=trunk value=" + SQL.quote(name));
      html.append("><input type=submit value='Delete Trunk'>" + "<input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "trunks"));
    html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
    if (verb.equals("view")) html.append("<input type=hidden name=edittrunk value=" + SQL.quote(edittrunk) + ">");
    html.append("<table>");
    html.append("<tr><td> Trunk: </td><td> <input name=trunk value=" + SQL.quote(name) + "</td></tr>");
    html.append("<tr><td> Host: </td><td> <input name=host value=" + SQL.quote(host) + "></td><td>domain_or_ip[:port] (default port = 5060)</td></tr>");
    html.append("<tr><td> External IP: </td><td> <input name=xip value=" + SQL.quote(xip) + "></td><td>(optional) (default = auto detect)</td></tr>");
    html.append("<tr><td nowrap> Override Caller ID: </td><td> <input name=cid value=" + SQL.quote(cid) + "></td><td>(optional)</td></tr>");
    html.append("<tr><td> Register String: </td><td> <input name=register value=" + SQL.quote(register) + "></td><td>(optional) user:pass@host[:port][/did]</td></tr>");
    html.append("<tr><td> <input type=checkbox name=doregister " + (doregister.equals("on") ? "checked" : "") + "> Register with trunk (optional)</td></tr>");
    html.append("<tr><td> Dial Out Rules: </td><td> <textarea name=outrules cols=20 rows=10>");
      String[] out_lns = outrules.split(":");
      for(int a=0;a<out_lns.length;a++) {html.append(out_lns[a]);html.append("\r\n");}
    html.append("</textarea>");
    html.append("</td><td>");
    html.append("(optional)<br>");
    html.append("Used to modify # dialed.<br>");
    html.append("If no matches are found the # is dialed as is.<br>");
    html.append("<pre>");
    html.append(" #=matches exact digit (0-9)\r\n");
    html.append(" X=matches 0-9\r\n");
    html.append(" Z=matches 1-9\r\n");
    html.append(" N=matches 2-9\r\n");
    html.append(" x|y = removes 'x' if 'xy' matches\r\n");
    html.append(" x+y = adds 'x' if 'y' matches\r\n");
    html.append(" .=wildcard (last char only)\r\n");
    html.append("</pre>");
    html.append("</td></tr>");
    html.append("<tr><td> Dial In Rules: </td><td> <textarea name=inrules class=aligntop cols=20 rows=10>");
      String[] in_lns = inrules.split(":");
      for(int a=0;a<in_lns.length;a++) {html.append(in_lns[a]);html.append("\r\n");}
    html.append("</textarea>");
    html.append("</td><td>");
    html.append("(optional) [not implemented yet]<br>");
    html.append("<pre>");
    html.append(" #=matches exact digit (0-9)\r\n");
    html.append(" X=matches 0-9\r\n");
    html.append(" Z=matches 1-9\r\n");
    html.append(" N=matches 2-9\r\n");
    html.append(" x|y = removes 'x' if 'xy' matches\r\n");
    html.append(" x+y = adds 'x' if 'y' matches\r\n");
    html.append(" .=wildcard (last char only)\r\n");
    html.append("</pre>");
    html.append("</td></tr>");
    html.append("<tr><td> <input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + "></td></tr>");
    html.append("</table>");
    html.append("</form>");
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
      html.append("<div class=menuitem>" + link("core", "trunks", "", "Add New") + "</div>");
      TrunkRow[] trunks = Database.getTrunks();
      if (trunks != null) {
        for(int a=0;a<trunks.length;a++) {
          html.append("<div class=menuitem>" + link("core", "trunks", "verb=view&trunk=" + trunks[a].name, "&lt;" + trunks[a].name + "&gt;") + "</div>");
        }
      }
    html.append("</td></tr></table>");

    return html.toString();
  }

  private String getPluginPage(String plugin, String pg, String args[], WebRequest req) {
    //currently plugin is only "core" - the idea was to allow other plugins - not likely
    if (!plugin.equals("core")) return "Plugin does not exist";
    if (pg.equals("blank")) {
      return doBlankPage(args);
    }
    else if (pg.equals("admin")) {
      return doAdminPage(args);
    }
    else if (pg.equals("conf")) {
      return doConfPage(args);
    }
    else if (pg.equals("exts")) {
      return doExtsPage(args);
    }
    else if (pg.equals("inroutes")) {
      return doInRoutesPage(args);
    }
    else if (pg.equals("ivrs")) {
      return doIVRPage(args);
    }
    else if (pg.equals("msgs")) {
      return doMsgsPage(args, req);
    }
    else if (pg.equals("outroutes")) {
      return doOutRoutesPage(args);
    }
    else if (pg.equals("settings")) {
      return doSettingsPage(args, req);
    }
    else if (pg.equals("status")) {
      return doStatusPage(args);
    }
    else if (pg.equals("trunks")) {
      return doTrunksPage(args);
    }
    else if (pg.equals("queues")) {
      return doQueuesPage(args);
    }
    return "Page does not exist";
  }

  private static String pages[][] = {
    // plugin, pg, display, cat
    {"core", "status", "System Status", "General"},
    {"core", "admin", "Administrator", "General"},
    {"core", "exts", "Extensions", "General"},
    {"core", "ivrs", "IVR", "General"},
    {"core", "conf", "Conference", "General"},
    {"core", "msgs", "Messsages", "General"},
    {"core", "trunks", "Trunks", "General"},
    {"core", "settings", "Settings", "General"},
    {"core", "queues", "Queues", "General"},
    {"core", "outroutes", "Outbound Routes", "Routing"},
    {"core", "inroutes", "Inbound Routes", "Routing"},
  };

  public String listPages(String id, String plugin, String pluginpg) {
    StringBuffer list = new StringBuffer();
    String lastCat = "";
    for(int a=0;a<pages.length;a++) {
      if (!pages[a][3].equals(lastCat)) {
        lastCat = pages[a][3];
        list.append("<div class=menucat>" + pages[a][3] + "</div>");
      }
      if (plugin.equalsIgnoreCase(pages[a][0]) && pluginpg.equalsIgnoreCase(pages[a][1])) {
        list.append("<div class=menuitemselected>");
      } else {
        list.append("<div class=menuitem>");
      }
      list.append("<a href='?plugin=" + pages[a][0] + "&pluginpg=" + pages[a][1] + "'>" + pages[a][2] + "</a></div>");
    }
    return list.toString();

  }

  public void doAdmin(WebRequest req, WebResponse res) throws Exception {
    writeHeaders(res.getOutputStream());
    StringBuilder html = new StringBuilder();
    String id = (String)req.session.getAttribute("id");
    if (id == null) {
      res.sendRedirect("/login");
      return;
    }
    String args[] = req.getQueryString().split("&");
    String plugin = "core";
    String pg;
    if (isAdmin(id)) {
      pg = "status";
    } else {
      pg = "blank";  //TODO : voicemail for normal users ???
    }
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = JF.decodeURL(arg.substring(idx + 1));
      if (value == null) value = "";
      switch (key) {
        case "plugin": plugin = value; break;
        case "pluginpg": pg = value; break;
      }
    }
    if (!isAllowed(id, plugin, pg)) { redir(res, "core", "blank"); return; }

    html.append("<div style='overflow: auto;'>");
    html.append("<table border=0 width=100% height=100% cellpadding=0 cellspacing=0>");
    html.append("<tr height=64><td width=100% colspan=2><a href='http://jfpbx.sourceforge.net'><img border=0 src=/static/img/logo.png></a>");
    html.append("<a href='/logout' style='float:right;'>Logout</a></td></tr>");
    html.append("<tr><td style='width: 180px; vertical-align:top;'>");
    html.append(listPages(id, plugin, pg));
    html.append("</td><td style='vertical-align:top; width: 100%;'>");
    html.append("<div style='border-top: 2px solid #000000; border-left: 2px solid #000000; height: 100%;'>");
    html.append(getPluginPage(plugin, pg, args, req));
    html.append("</div>");
    html.append("</td></tr>");
    html.append("</table>");
    html.append("</div>");
    html.append("<div style='position: absolute; display: none; top: 0px; left: 0px; width: 100%; height: 100%; overflow: auto;' id='help'>");
    html.append("<table width=100% height=100%>");
    html.append("<tr height=10%><td colspan=3 class='transparent'> </td></tr>");
    html.append("<tr width=100% height=80%>");
    html.append("  <td width=10% class='transparent'> </td>");
    html.append("  <td width=80% style='background-color: #ffffff; border: 5px; border-color: #000000; border-style: solid;'>");
    html.append("    <div style='float: right;'><a href=\"javascript:hide('help');\">Close</a></div>");
    html.append("    <div style='overflow: auto;' id='helpcontent'></div>");
    html.append("  </td>");
    html.append("  <td width=10% class='transparent'> </td>");
    html.append("</tr>");
    html.append("<tr height=10%><td colspan=3 class='transparent'> </td></tr>");
    html.append("</table>");
    html.append("</div>");

    res.getOutputStream().write(html.toString().getBytes());
  }

  private void noCache(WebResponse res) {
    res.addHeader("Cache-Control: no-cache, no-store, must-revalidate");
    res.addHeader("Pragma: no-cache");
    res.addHeader("Expires: 0");
  }

  public void doRequest(WebRequest req, WebResponse res) throws Exception {
    String url = req.getURL();
    if (url.startsWith("/static/") || url.equals("/favicon.ico")) {
      noCache(res);  //for development
      doStatic(req, res);
    }
    else if (url.equals("/")) {
      doIndex(req, res);
    }
    else if (url.startsWith("/login")) {
      noCache(res);
      doLogin(req, res);
    }
    else if (url.startsWith("/logout")) {
      noCache(res);
      doLogout(req, res);
    }
    else if (url.startsWith("/admin")) {
      noCache(res);
      doAdmin(req, res);
    }
    if (!disableWebRTC) {
      if (url.startsWith("/webrtc1")) {
        noCache(res);
        webrtc.doWebRTC1(req, res);
      }
      else if (url.startsWith("/webrtc2")) {
        noCache(res);
        webrtc.doWebRTC2(req, res);
      }
    }
  }

  //Core API
  public boolean isAdmin(String id) {
    return id.equals("admin");
  }
  public boolean isAllowed(String id, String plugin, String pluginpg) {
    return id.equals("admin");
  }
  public void redir(WebResponse response, String plugin, String pluginpg) {
    try {
      response.sendRedirect("/admin?plugin=" + plugin + "&pg=" + pluginpg);
    } catch(Exception e) {
    }
  }
  public String link(String plugin, String pluginpg, String params, String txt) {
    return "<a href='/admin?plugin=" + plugin + "&pluginpg=" + pluginpg + "&" + params + "'>" + txt + "</a>";
  }
  public String form(String plugin, String pluginpg) {
    //TODO : use method=post
    return "<form action='/admin' method='get'><input type=hidden name=plugin value='" + plugin + "'><input type=hidden name=pluginpg value='" + pluginpg + "'>";
  }
  public String formUpload(String plugin, String pluginpg, String args) {
    return "<form action='/admin?plugin=" + plugin + "&pluginpg=" + pluginpg + "&" + args +"' enctype='multipart/form-data' method='post'>";
  }
  public String select(String name, String value, String list[]) {
    StringBuffer buf = new StringBuffer();
    buf.append("<select name=" + name + "><option></option>");
    if (list != null) {
      for(int a=0;a<list.length;a++) {
        buf.append("<option" + (list[a].equalsIgnoreCase(value) ? " selected" : "") + ">" + list[a] + "</option>");
      }
    }
    buf.append("</select>");
    return buf.toString();
  }
  public String numbersOnly(String in) {
    String out = "";
    char ca[] = in.toCharArray();
    for(int a=0;a<ca.length;a++) {
      if ((ca[a] >= '0') && (ca[a] <= '9')) out += ca[a];
    }
    return out;
  }
  public String patternsOnly(String in) {
    //TODO : make sure 'in' is valid patterns (':' delimited)
    //0-9,X,Z,N,.,|,+,:
    in = in.toUpperCase();
    StringBuffer out = new StringBuffer();
    int sl = in.length();
    char ch;
    for(int a=0;a<sl;a++) {
      ch = in.charAt(a);
      if ((ch >= '0') && (ch <= '9')) {
        out.append(ch);
        continue;
      }
      switch (in.charAt(a)) {
        default:
          continue;
        case 'X':
        case 'Z':
        case 'N':
        case '.':
        case '|':
        case '+':
        case ':':
          out.append(ch);
      }
    }
    return out.toString();
  }
  public boolean isBuiltinSound(String filename) {
    if (filename.equalsIgnoreCase("vm-0")) return true;
    if (filename.equalsIgnoreCase("vm-1")) return true;
    if (filename.equalsIgnoreCase("vm-2")) return true;
    if (filename.equalsIgnoreCase("vm-3")) return true;
    if (filename.equalsIgnoreCase("vm-4")) return true;
    if (filename.equalsIgnoreCase("vm-5")) return true;
    if (filename.equalsIgnoreCase("vm-6")) return true;
    if (filename.equalsIgnoreCase("vm-7")) return true;
    if (filename.equalsIgnoreCase("vm-8")) return true;
    if (filename.equalsIgnoreCase("vm-9")) return true;
    if (filename.equalsIgnoreCase("vm-beep")) return true;
    if (filename.equalsIgnoreCase("vm-deleted")) return true;
    if (filename.equalsIgnoreCase("vm-end-msgs")) return true;
    if (filename.equalsIgnoreCase("vm-enter-password")) return true;
    if (filename.equalsIgnoreCase("vm-goodbye")) return true;
    if (filename.equalsIgnoreCase("vm-greeting")) return true;
    if (filename.equalsIgnoreCase("vm-incorrect")) return true;
    if (filename.equalsIgnoreCase("vm-main-menu")) return true;
    if (filename.equalsIgnoreCase("vm-msg-menu")) return true;
    if (filename.equalsIgnoreCase("vm-msg")) return true;
    if (filename.equalsIgnoreCase("vm-new")) return true;
    if (filename.equalsIgnoreCase("vm-next")) return true;
    if (filename.equalsIgnoreCase("vm-no-msgs")) return true;
    if (filename.equalsIgnoreCase("vm-old")) return true;
    if (filename.equalsIgnoreCase("vm-pause")) return true;
    if (filename.equalsIgnoreCase("vm-rec-greeting")) return true;
    if (filename.equalsIgnoreCase("vm-rec-menu")) return true;
    if (filename.equalsIgnoreCase("vm-too-short")) return true;
    if (filename.equalsIgnoreCase("vm-welcome")) return true;
    if (filename.equalsIgnoreCase("conf-admin-left")) return true;
    if (filename.equalsIgnoreCase("conf-no-admin")) return true;
    if (filename.equalsIgnoreCase("acd-wait-for-agent")) return true;
    return false;
  }
}
