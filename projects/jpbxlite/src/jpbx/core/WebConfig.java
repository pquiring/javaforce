package jpbx.core;

/**
 * WebConfig service for jPBXlite
 *
 * @author pquiring
 *
 * Created : Sept 14, 2013
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.service.*;

public class WebConfig implements WebHandler {
  public static int http_port, https_port, sip_port;
  public static boolean hideAdmin, disableWebRTC;
  private static String dbVersion = "0.92";
  private WebRTC webrtc;
  private Web http, https;

  public boolean start() {
    JFLog.log("Starting Web Server on port " + http_port);
    http = new Web();
    http.start(this, http_port, false);
    if (new File(Paths.etc + "jpbx.key").exists()) {
      System.setProperty("javax.net.ssl.keyStore", Paths.etc + "jpbx.key");
      System.setProperty("javax.net.ssl.keyStorePassword", "password");
      JFLog.log("Starting Web Server on port " + https_port + " (secure)");
      https = new Web();
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

  private String decode(String in) {
    try {return URLDecoder.decode(in, "UTF-8");} catch (Exception e) {}
    return "";
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
      url = "/static/jpbxlite.ico";
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
    html.append("  <title>jPBXlite</title>");
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
    SQL sql = new SQL();
    if (!sql.connect(Service.jdbc)) return "Unable to connect to database";
    String res = sql.select1value("SELECT passmd5 FROM users WHERE userid='" + user + "'");
    sql.close();
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
      if (args[a].startsWith("user=")) user = args[a].substring(5);
      if (args[a].startsWith("pass=")) pass = args[a].substring(5);
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

  private String doAdminPage(SQL sql, String args[]) {
    String verb = "", current = "", p1 = "", p2 = "";
    for(int a=0;a<args.length;a++) {
      if (args[a].startsWith("verb=")) verb = args[a].substring(5);
      if (args[a].startsWith("current=")) current = args[a].substring(8);
      if (args[a].startsWith("p1=")) p1 = args[a].substring(3);
      if (args[a].startsWith("p2=")) p2 = args[a].substring(3);
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
        String storedmd5 = sql.select1value("SELECT passmd5 FROM users WHERE userid='admin'");
        if ((storedmd5 == null) || (!storedmd5.equals(currentmd5))) {
          msg = "Incorrect current password";
        } else {
          md5.init();
          md5.add(p1.getBytes(),0,p1.length());
          String passmd5 = new String(md5.byte2char(md5.done()));
          if (!sql.execute("UPDATE users SET passmd5=" + sql.quote(passmd5) + " WHERE userid='admin'")) {
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

  private String doBlankPage(SQL sql, String args[]) {
    return "Please select an option on the left.";
  }

  private String doConfPage(SQL sql, String args[]) {
    return "Conferences are special IVRs.  To create a conference, create a new IVR and load the preset \"Conference\".<br>\n" +
           "To create a video conference select the \"Video Conference\" preset.<br>\n" +
           "Then change the $adminpass and $userpass variables for the admin and user pass codes.<br>\n";
  }

  private String doExtsPage(SQL sql, String args[]) {
    String verb = "", ext = "", editext = "", cloneStart = "", cloneCount = "", display = "", cid = "", pass = "", msg = "", sure = "", vm = "", vmpass = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
      if (args[a].startsWith("ext=")) ext = args[a].substring(x);
      if (args[a].startsWith("editext=")) editext = args[a].substring(x);
      if (args[a].startsWith("cloneStart=")) cloneStart = args[a].substring(x);
      if (args[a].startsWith("cloneCount=")) cloneCount = args[a].substring(x);
      if (args[a].startsWith("display=")) display = args[a].substring(x);
      if (args[a].startsWith("cid=")) cid = args[a].substring(x);
      if (args[a].startsWith("pass=")) pass = args[a].substring(x);
      if (args[a].startsWith("sure=")) sure = args[a].substring(x);
      if (args[a].startsWith("vm=")) vm = args[a].substring(x);
      if (args[a].startsWith("vmpass=")) vmpass = args[a].substring(x);
    }
    ext = numbersOnly(ext);
    cid = numbersOnly(cid);
    display = convertString(display);
    vmpass = numbersOnly(vmpass);
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        if (sql.execute("DELETE FROM exts WHERE ext=" + sql.quote(ext))) {
          if (!sql.execute("DELETE FROM extopts WHERE ext=" + sql.quote(ext))) {
            msg = "Failed to delete extension options";
          }
        } else {
          msg = "Failed to delete extension";
        }
        if (msg.length() == 0) {
          msg = "Extension deleted";
        }
      } else {
        msg = "Please confirm delete action";
      }
      ext = "";
    }
    if (verb.equals("view")) {
      editext = ext;
      String exts[] = sql.select1row("SELECT display,cid,pass FROM exts WHERE ext=" + sql.quote(ext));
      if (exts != null) {
        display = exts[0];
        cid = exts[1];
        pass = exts[2];
        vm = sql.select1value("SELECT value FROM extopts WHERE ext=" + sql.quote(ext) + " AND id='vm'");
        if (vm == null) vm = "false";
        vmpass = sql.select1value("SELECT value FROM extopts WHERE ext=" + sql.quote(ext) + " AND id='vmpass'");
        if (vmpass == null) vmpass = "";
      }
    }
    if (verb.equals("add") || verb.equals("edit")) {
      if (ext.length() == 0)  {
        msg = "Invalid extension number";
      } else {
        String isivr = sql.select1value("SELECT ext FROM ivrs WHERE ext=" + sql.quote(ext));
        if ((isivr != null) && (isivr.equals(ext))) {
          msg = "IVR already exists with that number";
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
      if (!sql.execute("INSERT INTO exts (ext, display, cid, pass) VALUES (" + sql.quote(ext) + "," + sql.quote(display) + "," + sql.quote(cid) + "," + sql.quote(pass) + ")")) {
        msg = "Failed to add : " + sql.getLastException();
      } else if (!sql.execute("INSERT INTO extopts (ext, id, value) VALUES (" + sql.quote(ext) + ",'vm'," + sql.quote( (vm.equalsIgnoreCase("on") ? "true" : "false") ) + ")")) {
        msg = "Unable to add voicemail option";
      } else if (!sql.execute("INSERT INTO extopts (ext, id, value) VALUES (" + sql.quote(ext) + ",'vmpass'," + sql.quote(vmpass) + ")")) {
        msg = "Unable to add voicemail password";
      }
      if (msg.length() == 0) {
        msg = "Extension added";
        ext = "";
        display = "";
        cid = "";
        pass = "";
        vm = "";
        vmpass = "";
      }
    }
    if (verb.equals("edit")) {
      if (!sql.execute("UPDATE exts SET ext=" + sql.quote(ext) + ",display=" + sql.quote(display) + ",cid=" + sql.quote(cid) + ",pass=" + sql.quote(pass) + " WHERE ext=" + sql.quote(editext))) {
        msg = "Failed to edit : " + sql.getLastException();
      } else if (!sql.execute("UPDATE extopts SET ext=" + sql.quote(ext) + ",value=" + sql.quote( (vm.equalsIgnoreCase("on") ? "true" : "false") ) + " WHERE id='vm' AND ext=" + sql.quote(editext))) {
        msg = "Unable to edit voicemail option";
      } else if (!sql.execute("UPDATE extopts SET ext=" + sql.quote(ext) + ",value=" + sql.quote(vmpass) + " WHERE id='vmpass' AND ext=" + sql.quote(editext))) {
        msg = "Unable to edit voicemail password";
      }
      if (msg.length() == 0) {
        msg = "Extension edited";
        ext = "";
        display = "";
        cid = "";
        pass = "";
        vm = "";
        vmpass = "";
      } else {
        verb = "view";
      }
    }
    if (verb.equals("clone")) {
      verb = "";
      String exts[] = sql.select1row("SELECT cid,pass FROM exts WHERE ext=" + sql.quote(ext));
      cid = exts[0];
      pass = exts[1];
      vm = sql.select1value("SELECT value FROM extopts WHERE ext=" + sql.quote(ext) + " AND id='vm'");
      if (vm == null) vm = "false";
      vmpass = sql.select1value("SELECT value FROM extopts WHERE ext=" + sql.quote(ext) + " AND id='vmpass'");
      if (vmpass == null) vmpass = "";
      try {
        int start = Integer.valueOf(cloneStart);
        int count = Integer.valueOf(cloneCount);
        if (count <=0 || count > 1000) throw new Exception("max 1000 clones");
        int added = 0;
        for(int idx=start;count > 0;count--,idx++) {
          ext = "" + idx;
          display = "" + idx;
          msg = "";
          if (!sql.execute("INSERT INTO exts (ext, display, cid, pass) VALUES (" + sql.quote(ext) + "," + sql.quote(display) + "," + sql.quote(cid) + "," + sql.quote(pass) + ")")) {
            msg = "Failed to add";
          } else if (!sql.execute("INSERT INTO extopts (ext, id, value) VALUES (" + sql.quote(ext) + ",'vm'," + sql.quote(vm) + ")")) {
            msg = "Unable to add voicemail option";
          } else if (!sql.execute("INSERT INTO extopts (ext, id, value) VALUES (" + sql.quote(ext) + ",'vmpass'," + sql.quote(vmpass) + ")")) {
            msg = "Unable to add voicemail password";
          }
          if (msg.length() == 0) added++;
        }
        msg = "Cloning complete (" + added + " extensions added)";
      } catch (Exception e) {
        msg = "Cloning failed:" + e.toString();
      }
      ext = "";
      display = "";
      cid = "";
      pass = "";
      vm = "";
      vmpass = "";
    }
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font><br>");
    if (verb.equals("view")) {
      html.append(form("core", "exts"));
      html.append("<input type=hidden name=verb value=del><input type=hidden name=ext value=" + sql.quote(ext) + ">");
      html.append("<input type=submit value='Delete Extension'> <input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "exts"));
    if (verb.equals("cloneForm")) {
      html.append("<input type=hidden name=verb value=clone>");
      html.append("<table>");
      html.append("<tr><td>Extension #:</td><td><input name=ext value=" + sql.quote(ext) + " readonly></td></tr>");
      html.append("<tr><td>Start:</td><td><input name=cloneStart></td></tr>");
      html.append("<tr><td>Count:</td><td><input name=cloneCount></td></tr>");
      html.append("<tr><td colspan=2>NOTE : Existing extension in clone range may be altered!</td></tr>");
      html.append("</table>");
      html.append("<input type=submit value=Clone>");
    } else {
      html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
      if (verb.equals("view")) html.append("<input type=hidden name=editext value=" + sql.quote(editext) + ">");
      html.append("<table>");
      html.append("<tr><td>Extension #:</td><td><input name=ext value=" + sql.quote(ext) + "></td><td>");
      if (verb.equals("view")) {
        html.append(link("core", "exts", "verb=cloneForm&ext=" + editext, "Clone"));
      }
      html.append("</td></tr>");
      html.append("<tr><td>Display Name:</td><td><input name=display value=" + sql.quote(display) + "></td><td>(usually same as extension #)</td></tr>");
      html.append("<tr><td>Outbound Caller ID #:</td><td><input name=cid value=" + sql.quote(cid) + "></td><td>(optional) (used if call sent to trunk)</td></tr>");
      html.append("<tr><td>SIP Password:</td><td><input name=pass value=" + sql.quote(pass) + "></td></tr>");
      html.append("<tr><td>VoiceMail:</td><td><input type=checkbox name=vm " + (vm.equals("true") ? "checked" : "") + "></td></tr>");
      html.append("<tr><td>VM Password:</td><td><input name=vmpass value=" + sql.quote(vmpass) + "></td></tr>");
      html.append("</table>");
      html.append("<input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + ">");
    }
    html.append("</form>");
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    html.append("<div class=menuitem>" + link("core", "exts", "", "Add New") + "</div>");
    String exts[][] = sql.select("SELECT ext,display FROM exts ORDER BY ext");
    if (exts != null) {
      for(int a=0;a<exts.length;a++) {
        html.append("<div class=menuitem>" + link("core", "exts", "verb=view&ext=" + exts[a][0], exts[a][1] + "&lt;" + exts[a][0] + "&gt;") + "</div>");
      }
    }
    html.append("</td></tr>");
    html.append("</table>");

    return html.toString();
  }

  private String doInRoutesPage(SQL sql, String args[]) {
    StringBuilder html = new StringBuilder();

    String verb = "", route = "", cid = "", did = "", dest = "", msg = "", editroute= "", sure = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
      if (args[a].startsWith("route=")) route = args[a].substring(x);
      if (args[a].startsWith("editroute=")) editroute = args[a].substring(x);
      if (args[a].startsWith("cid=")) cid = args[a].substring(x);
      if (args[a].startsWith("did=")) did = args[a].substring(x);
      if (args[a].startsWith("dest=")) dest = args[a].substring(x);
      if (args[a].startsWith("sure=")) sure = args[a].substring(x);
    }
    cid = numbersOnly(cid);
    did = numbersOnly(did);
    dest = numbersOnly(dest);

    if (route.length() == 0) verb = "";
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        if (sql.execute("DELETE FROM inroutes WHERE route=" + sql.quote(route))) {
          msg = "Route deleted";
        } else {
          msg = "Failed to delete";
        }
      } else {
        msg = "Please confirm delete action";
      }
      route = "";
    }
    if (verb.equals("view")) {
      editroute = route;
      String routes[] = sql.select1row("SELECT cid,did,dest FROM inroutes WHERE route=" + sql.quote(route));
      if (routes != null) {
        cid = routes[0];
        did = routes[1];
        dest = routes[2];
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
      if (sql.execute("INSERT INTO inroutes (route, cid, did, dest) VALUES (" + sql.quote(route) + ","
      + sql.quote(cid) + "," + sql.quote(did) + "," + sql.quote(dest) + ")")) {
        msg = "Route added";
        route = "";
        cid = "";
        did = "";
        dest = "";
      } else {
        msg = "Failed to add:" + sql.getLastException();
      }
    }
    if (verb.equals("edit")) {
      if (sql.execute("UPDATE inroutes SET route=" + sql.quote(route) + ",cid=" + sql.quote(cid) + ",did=" + sql.quote(did)
      + ",dest=" + sql.quote(dest) + " WHERE route=" + sql.quote(editroute))) {
        msg = "Route edited";
        route = "";
        cid = "";
        did = "";
        dest = "";
      } else {
        msg = "Failed to edit:" + sql.getLastException();
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
      html.append("<input type=hidden name=verb value=del><input type=hidden name=route value=" + sql.quote(route) + "><input type=submit value='Delete route'>");
      html.append("<input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "inroutes"));
    html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
    if (verb.equals("view")) {
      html.append("<input type=hidden name=editroute value=" + sql.quote(editroute) + ">");
    }
    html.append("<table>");
    html.append("<tr><td> Route Name: </td><td> <input name=route value=" + sql.quote(route) + "</td></tr>");
    html.append("<tr><td> Dialed # (DID): </td><td> <input name=did value=" + sql.quote(did) + "></td><td>(optional)</td></tr>");
    html.append("<tr><td> Caller # (CID): </td><td> <input name=cid value=" + sql.quote(cid) + "></td><td>(optional)</td></tr>");
    html.append("<tr><td> Destination #: </td><td> <input name=dest value=" + sql.quote(dest) + "></td><td>(Extension, IVR, etc.)</tr>");
    html.append("<tr><td> <input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + "></td></tr>");
    html.append("</table>");
    html.append("</form>");
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    html.append("<div class=menuitem>" + link("core", "inroutes", "", "Add New") + "</div>");
    String routes[][] = sql.select("SELECT route, did, cid FROM inroutes ORDER BY route");
    if (routes != null) {
      for(int a=0;a<routes.length;a++) {
        if (routes[a][1].length() == 0) routes[a][1] = "any DID";
        if (routes[a][2].length() == 0) routes[a][2] = "any CID";
        html.append("<div class=menuitem>" + link("core", "inroutes", "verb=view&route=" + routes[a][0],
          "&lt;" + routes[a][0] + "&gt;" + routes[a][1] + "/" + routes[a][2]) + "</div>");
      }
    }
    html.append("</td></tr></table>");

    return html.toString();
  }

  private String doIVRPage(SQL sql, String args[]) {
    String verb = "", ext = "", editext = "", display = "", script = "", sure = "", msg = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
      if (args[a].startsWith("ext=")) ext = args[a].substring(x);
      if (args[a].startsWith("display=")) display = args[a].substring(x);
      if (args[a].startsWith("script=")) script = args[a].substring(x);
      if (args[a].startsWith("editext=")) editext = args[a].substring(x);
      if (args[a].startsWith("sure=")) sure = args[a].substring(x);
    }
    ext = numbersOnly(ext);
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        if (!sql.execute("DELETE FROM ivrs WHERE ext=" + sql.quote(ext))) {
          msg = "Failed to delete IVR";
        }
        if (msg.length() == 0) {
          msg = "IVR deleted";
        }
      } else {
        msg = "Please confirm delete action";
      }
      ext = "";
    }
    if (verb.equals("view")) {
      editext = ext;
      display = sql.select1value("SELECT display FROM ivrs WHERE ext=" + sql.quote(ext));
      script = sql.select1value("SELECT script FROM ivrs WHERE ext=" + sql.quote(ext));
    }
    if (verb.equals("add") || verb.equals("edit")) {
      if (ext.length() == 0)  {
        msg = "Invalid IVR number";
      } else {
        String isext = sql.select1value("SELECT ext FROM exts WHERE ext=" + sql.quote(ext));
        if ((isext != null) && (isext.equals(ext))) {
          msg = "Extension already exists with that number";
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
      if (!sql.execute("INSERT INTO ivrs (ext, display, script) VALUES (" + sql.quote(ext) + "," + sql.quote(display) + "," + sql.quote(script) + ")")) {
        msg = "Failed to add";
      }
      if (msg.length() == 0) {
        msg = "IVR added";
        ext = "";
        display = "";
        script = "";
      }
    }
    if (verb.equals("edit")) {
      if (!sql.execute("UPDATE ivrs SET ext=" + sql.quote(ext) + ",display=" + sql.quote(display) + ",script=" + sql.quote(script) + " WHERE ext=" + sql.quote(editext))) {
        msg = "Failed to edit : " + sql.getLastException();
        verb = "view";
      }
      if (msg.length() == 0) {
        msg = "IVR edited";
        ext = "";
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
      html.append("<input type=hidden name=verb value=del><input type=hidden name=ext value=" + sql.quote(ext));
      html.append("><input type=submit value='Delete IVR'>" + "<input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "ivrs"));
    html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
    if (verb.equals("view")) {
      html.append("<input type=hidden name=editext value=" + sql.quote(editext) + ">");
    }
    html.append("<table>");
    html.append("<tr><td>IVR #:</td><td><input name=ext value=" + sql.quote(ext) + ">");
    html.append("</td></tr>");
    html.append("<tr><td>Name:</td><td><input name=display value=" + sql.quote(display) + "></td></tr>");
    html.append("<tr><td>Script:</td><td><textarea id=script name=script cols=40 rows=20>" + convertString(script) + "</textarea></td><td><a href=\"javascript:showHelp('ivr');\">Help</a><br>");
    html.append("Presets:<select id=preset><option>-none-</option><option>Conference</option><option>Video Conference</option></select> <a href=\"javascript:load_preset();\">Load</a></td></tr>");
    html.append("</table>");
    html.append("<input type=submit value=" + (verb.equals("view") ? "Edit" : "Add") + ">");
    html.append("</form>");
    html.append("</td><td width=2 bgcolor=#000000></td><td width=180>");
    html.append("<div class=menuitem>" + link("core", "ivrs", "", "Add New") + "</div>");
    String ivrs[][] = sql.select("SELECT ext,display FROM ivrs ORDER BY ext");
    if (ivrs != null) {
      for(int a=0;a<ivrs.length;a++) {
        html.append("<div class=menuitem>" + link("core", "ivrs", "verb=view&ext=" + ivrs[a][0], ivrs[a][1] + "&lt;" + ivrs[a][0] + "&gt;") + "</div>");
      }
    }
    html.append("</td></tr>");
    html.append("</table>");

    return html.toString();
  }

  private String doQueuesPage(SQL sql, String args[]) {
    String verb = "", ext = "", editext = "", display = "", agents = "", sure = "", msg = "", message = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
      if (args[a].startsWith("ext=")) ext = args[a].substring(x);
      if (args[a].startsWith("display=")) display = args[a].substring(x);
      if (args[a].startsWith("agents=")) agents = convertString(args[a].substring(x)).replaceAll("\r\n", ",");
      if (args[a].startsWith("message=")) message = args[a].substring(x);
      if (args[a].startsWith("editext=")) editext = args[a].substring(x);
      if (args[a].startsWith("sure=")) sure = args[a].substring(x);
    }
    if (message.length() == 0) message = "acd-wait-for-agent";
    ext = numbersOnly(ext);
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        if (!sql.execute("DELETE FROM queues WHERE ext=" + sql.quote(ext))) {
          msg = "Failed to delete queue";
        }
        if (msg.length() == 0) {
          msg = "Queue deleted";
        }
      } else {
        msg = "Please confirm delete action";
      }
      ext = "";
    }
    if (verb.equals("view")) {
      editext = ext;
      display = sql.select1value("SELECT display FROM queues WHERE ext=" + sql.quote(ext));
      agents = sql.select1value("SELECT agents FROM queues WHERE ext=" + sql.quote(ext));
      message = sql.select1value("SELECT message FROM queues WHERE ext=" + sql.quote(ext));
    }
    if (verb.equals("add") || verb.equals("edit")) {
      if (ext.length() == 0)  {
        msg = "Invalid Queue number";
      } else {
        String isext = sql.select1value("SELECT ext FROM exts WHERE ext=" + sql.quote(ext));
        if ((isext != null) && (isext.equals(ext))) {
          msg = "Extension already exists with that number";
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
      if (!sql.execute("INSERT INTO queues (ext, display, agents, message) VALUES (" + sql.quote(ext) + "," + sql.quote(display) + "," + sql.quote(agents) + "," + sql.quote(message) + ")")) {
        msg = "Failed to add";
      }
      if (msg.length() == 0) {
        msg = "Queue added";
        ext = "";
        display = "";
        agents = "";
      }
    }
    if (verb.equals("edit")) {
      if (!sql.execute("UPDATE queues SET ext=" + sql.quote(ext) + ",display=" + sql.quote(display) + ",agents=" + sql.quote(agents) + ",message=" + sql.quote(message) + " WHERE ext=" + sql.quote(editext))) {
        msg = "Failed to edit : " + sql.getLastException();
        verb = "view";
      }
      if (msg.length() == 0) {
        msg = "Queue edited";
        ext = "";
        display = "";
        agents = "";
      }
    }
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) {
      html.append("<font color=#ff0000>" + msg + "</font><br>");
    }
    if (verb.equals("view")) {
      html.append(form("core", "queues"));
      html.append("<input type=hidden name=verb value=del><input type=hidden name=ext value=" + sql.quote(ext));
      html.append("><input type=submit value='Delete Queue'>" + "<input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "queues"));
    html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
    if (verb.equals("view")) {
      html.append("<input type=hidden name=editext value=" + sql.quote(editext) + ">");
    }
    html.append("<table>");
    html.append("<tr><td>Queue #:</td><td><input name=ext value=" + sql.quote(ext) + ">");
    html.append("</td></tr>");
    html.append("<tr><td>Name:</td><td><input name=display value=" + sql.quote(display) + "></td></tr>");
    html.append("<tr><td>Agents:</td><td><textarea id=agents name=agents cols=40 rows=20>" + convertString(agents).replaceAll(",", "\r\n") + "</textarea> (list agents one per line)</td></tr>");
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
    String queues[][] = sql.select("SELECT ext,display FROM queues ORDER BY ext");
    if (queues != null) {
      for(int a=0;a<queues.length;a++) {
        html.append("<div class=menuitem>" + link("core", "queues", "verb=view&ext=" + queues[a][0], queues[a][1] + "&lt;" + queues[a][0] + "&gt;") + "</div>");
      }
    }
    html.append("</td></tr>");
    html.append("</table>");

    return html.toString();
  }

  private String doMsgsPage(SQL sql, String args[], WebRequest req) {
    String verb = "", file = "", oldfile = "", newfile = "", sure = "", msg= "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
      if (args[a].startsWith("file=")) file = decode(args[a].substring(x));
      if (args[a].startsWith("oldfile=")) oldfile = decode(args[a].substring(x));
      if (args[a].startsWith("newfile=")) newfile = decode(args[a].substring(x));
      if (args[a].startsWith("sure=")) sure = args[a].substring(x);
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
      html.append("<input type=hidden name=verb value=del><input type=hidden name=file value=" + sql.quote(file));
      html.append("><input type=submit value='Delete Message'>" + "<input type=checkbox name=sure>I'm Sure</form>");
    }
    if (verb.equals("renameForm")) {
      html.append(form("core", "msgs"));
      html.append("Not implemented Yet!<br>");
      html.append("<input type=hidden name=verb value=rename>");
      html.append("<table>");
      html.append("<tr><td>Old Filename:</td><td><input name=oldfile value=" + sql.quote(oldfile) + " readonly></td></tr>");
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

  private String doOutRoutesPage(SQL sql, String args[]) {
    String verb = "", routetable = "", route = "", priority = "", cid = "", patterns = "", trunks = "", msg = "", editroute= "", t1 = "", t2 = "", t3 = "", t4 = "", sure = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
      if (args[a].startsWith("routetable=")) routetable = args[a].substring(x);
      if (args[a].startsWith("route=")) route = args[a].substring(x);
      if (args[a].startsWith("priority=")) priority = args[a].substring(x);
      if (args[a].startsWith("editroute=")) editroute = args[a].substring(x);
      if (args[a].startsWith("cid=")) cid = args[a].substring(x);
      if (args[a].startsWith("patterns=")) patterns = args[a].substring(x);
      if (args[a].startsWith("t1=")) t1 = args[a].substring(x);
      if (args[a].startsWith("t2=")) t2 = args[a].substring(x);
      if (args[a].startsWith("t3=")) t3 = args[a].substring(x);
      if (args[a].startsWith("t4=")) t4 = args[a].substring(x);
      if (args[a].startsWith("sure=")) sure = args[a].substring(x);
    }
    cid = numbersOnly(cid);
    trunks = t1;
    if (t2.length() > 0) {if (trunks.length() > 0) trunks += ":"; trunks += t2;}
    if (t3.length() > 0) {if (trunks.length() > 0) trunks += ":"; trunks += t3;}
    if (t4.length() > 0) {if (trunks.length() > 0) trunks += ":"; trunks += t4;}
    priority = numbersOnly(priority);
    if (priority.length() == 0) priority = "0";

    if (routetable.length() == 0) verb = "";
    if (verb.equals("deltable")) {
      if (sql.execute("DELETE FROM outroutetables WHERE routetable=" + sql.quote(routetable))) {
        sql.execute("DELETE FROM outroutes WHERE routetable=" + sql.quote(routetable));
        msg = "Table deleted";
      } else {
        msg = "Failed to delete table";
      }
      routetable = "";
    }
    if (verb.equals("addtable")) {
      if (sql.execute("INSERT INTO outroutetables (routetable) VALUES (" + sql.quote(routetable) + ")")) {
        msg = "Table added";
      } else {
        msg = "Failed to add table";
      }
    }

    if (route.length() == 0) verb = "";
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        if (sql.execute("DELETE FROM outroutes WHERE routetable=" + sql.quote(routetable) + " AND route=" + sql.quote(route))) {
          msg = "Route deleted";
        } else {
          msg = "Failed to delete";
        }
      } else {
        msg = "Please confirm delete action";
      }
      route = "";
    }
    if (verb.equals("view")) {
      editroute = route;
      String routes[] = sql.select1row("SELECT priority,cid,patterns,trunks FROM outroutes WHERE routetable=" + sql.quote(routetable) + " AND route=" + sql.quote(route));
      if (routes != null) {
        priority = routes[0];
        cid = routes[1];
        patterns = routes[2];
        trunks = routes[3];
        String lns[] = trunks.split(":");
        if (lns.length > 0) t1 = lns[0];
        if (lns.length > 1) t2 = lns[1];
        if (lns.length > 2) t3 = lns[2];
        if (lns.length > 3) t4 = lns[3];
      }
    }
    if (verb.equals("add")) {
      patterns = convertString(patterns).replaceAll("\r\n", ":");
      patterns = patternsOnly(patterns);
      if (sql.execute("INSERT INTO outroutes (routetable, route, priority, cid, patterns, trunks) VALUES (" + sql.quote(routetable) + "," + sql.quote(route) + ","
      + priority + "," + sql.quote(cid) + "," + sql.quote(patterns) + "," + sql.quote(trunks) + ")")) {
        msg = "Route added";
        route = "";
        priority = "";
        cid = "";
        patterns = "";
        trunks = "";
        t1 = "";
        t2 = "";
        t3 = "";
        t4 = "";
      } else {
        msg = "Failed to add";
      }
    }
    if (verb.equals("edit")) {
      patterns = convertString(patterns).replaceAll("\r\n", ":");
      patterns = patternsOnly(patterns);
      if (sql.execute("UPDATE outroutes SET route=" + sql.quote(route) + ",priority=" + sql.quote(priority) + ",cid=" + sql.quote(cid) + ",patterns=" + sql.quote(patterns)
      + ",trunks=" + sql.quote(trunks) + " WHERE routetable=" + sql.quote(routetable) + " AND route=" + sql.quote(editroute))) {
        msg = "Route edited";
        route = "";
        priority = "";
        cid = "";
        patterns = "";
        t1 = "";
        t2 = "";
        t3 = "";
        t4 = "";
      } else {
        msg = "Failed to edit";
        verb = "view";
      }
    }
    String list[] = sql.select1col("SELECT trunk FROM trunks");
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    html.append("<div class=table>");
    html.append("<div class=menucat>Routing Tables</div>");
    String tables[] = sql.select1col("SELECT routetable FROM outroutetables");
    if (tables != null) {
      for(int a=0;a<tables.length;a++) {
        if (routetable.equals(tables[a])) {
          html.append("<div class=menuitemselected>");
        } else {
          html.append("<div class=menuitem>");
        }
        html.append(link("core", "outroutes", "routetable=" + tables[a], tables[a]) + "</div>");
      }
    }
    html.append(form("core", "outroutes") + "<input name=routetable><input type=hidden name=verb value=addtable><input type=submit value='Add Table'></form><br>");
    html.append(form("core", "outroutes") + "<input name=routetable><input type=hidden name=verb value=deltable><input type=submit value='Delete Table'></form>");
    html.append("</div>");
    html.append("<hr>");
    if (routetable.length() > 0) {
      if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font><br>");
      if (verb.equals("view")) {
        html.append(form("core", "outroutes") + "<input type=hidden name=routetable value=" + routetable);
        html.append("><input type=hidden name=verb value=del><input type=hidden name=route value=" + sql.quote(route) + "><input type=submit value='Delete route'>");
        html.append("<input type=checkbox name=sure>I'm Sure</form>");
      }
      html.append(form("core", "outroutes"));
      html.append("<input type=hidden name=routetable value=" + sql.quote(routetable) + ">");
      html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
      if (verb.equals("view")) {
        html.append("<input type=hidden name=editroute value=" + sql.quote(editroute) + ">");
      }
      html.append("<table>");
      html.append("<tr><td> Route Name: </td><td> <input name=route value=" + sql.quote(route) + "></td></tr>");
      html.append("<tr><td> Priority: </td><td> <input name=priority value=" + sql.quote(priority) + "></td></tr>");
      html.append("<tr><td> Default Caller ID: </td><td> <input name=cid value=" + sql.quote(cid) + "></td><td>(optional)</td></tr>");
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
    if (routetable.length() == 0) {
      html.append("<div class=menuitem>Select a Routing Table</div>");
    } else {
      html.append("<div class=menuitem>" + link("core", "outroutes", "routetable=" + routetable, "Add New") + "</div>");
      String routes[] = sql.select1col("SELECT route FROM outroutes WHERE routetable=" + sql.quote(routetable) + " ORDER BY priority");
      if (routes != null) {
        for(int a=0;a<routes.length;a++) {
          html.append("<div class=menuitem>" + link("core", "outroutes", "routetable=" + routetable + "&verb=view&route=" + routes[a], "&lt;" + routes[a] + "&gt;") + "</div>");
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

  private String doSettingsPage(SQL sql, String args[], WebRequest req) {
    String verb = "", port = "", msg = "", anon = "", route = "", rtpmin = "", rtpmax = "", videoCodecs = "";
    String relayAudio = "", relayVideo = "", moh = "";
    String http = "", https = "", hideAdmin = "", disableWebRTC = "";
    String valid = "", dname = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
      if (args[a].startsWith("port=")) port = args[a].substring(x);
      if (args[a].startsWith("anon=")) anon = args[a].substring(x);
      if (args[a].startsWith("route=")) route = args[a].substring(x);
      if (args[a].startsWith("rtpmin=")) rtpmin = args[a].substring(x);
      if (args[a].startsWith("rtpmax=")) rtpmax = args[a].substring(x);
      if (args[a].startsWith("videoCodecs=")) videoCodecs = decode(args[a].substring(x).toUpperCase());
      if (args[a].startsWith("relayAudio=")) relayAudio = args[a].substring(x);
      if (args[a].startsWith("relayVideo=")) relayVideo = args[a].substring(x);
      if (args[a].startsWith("moh=")) moh = decode(args[a].substring(x));
      if (args[a].startsWith("http=")) http = args[a].substring(x);
      if (args[a].startsWith("https=")) https = args[a].substring(x);
      if (args[a].startsWith("hideAdmin=")) hideAdmin = args[a].substring(x);
      if (args[a].startsWith("disableWebRTC=")) disableWebRTC = args[a].substring(x);
      if (args[a].startsWith("valid=")) valid = args[a].substring(x);
      if (args[a].startsWith("dname=")) dname = decode(args[a].substring(x));
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
      sql.execute("UPDATE config SET value=" + sql.quote(port) + " WHERE id='port'");
      sql.execute("UPDATE config SET value=" + sql.quote(anon) + " WHERE id='anon'");
      sql.execute("UPDATE config SET value=" + sql.quote(route) + " WHERE id='route'");
      sql.execute("UPDATE config SET value=" + sql.quote("" + irtpmin) + " WHERE id='rtpmin'");
      sql.execute("UPDATE config SET value=" + sql.quote("" + irtpmax) + " WHERE id='rtpmax'");
      sql.execute("UPDATE config SET value=" + sql.quote(videoCodecs) + " WHERE id='videoCodecs'");
      sql.execute("UPDATE config SET value=" + sql.quote(relayAudio) + " WHERE id='relayAudio'");
      sql.execute("UPDATE config SET value=" + sql.quote(relayVideo) + " WHERE id='relayVideo'");
      sql.execute("UPDATE config SET value=" + sql.quote(moh) + " WHERE id='moh'");
      sql.execute("UPDATE config SET value=" + sql.quote(http) + " WHERE id='http'");
      sql.execute("UPDATE config SET value=" + sql.quote(https) + " WHERE id='https'");
      sql.execute("UPDATE config SET value=" + sql.quote(hideAdmin) + " WHERE id='hideAdmin'");
      sql.execute("UPDATE config SET value=" + sql.quote(disableWebRTC) + " WHERE id='disableWebRTC'");
      msg = "Settings saved";
    }
    //NOTE : The -debug option is important to prevent KeyTool from executing System.exit()
    if (verb.equals("sslSelf")) {
      if (KeyMgmt.keytool(new String[] {
        "-genkey", "-debug", "-alias", "jpbxlite", "-keypass", "password", "-storepass", "password",
        "-keystore", Paths.etc + "jpbx.key", "-validity", valid, "-dname", dname,
        "-keyalg" , "RSA", "-keysize", "2048"
      })) {
        msg = "Generated self-signed SSL Certificate";
      } else {
        msg = "KeyTool Error";
      }
    }
    String cfg[][] = sql.select("SELECT id,value FROM config");
    if (cfg == null) {
      return "Database error";
    }
    html.append("<font color=#ff0000>" + msg + "</font><br>");
    html.append(form("core", "settings"));

    html.append("<input type=hidden name='verb' value='save'>");
    html.append("SIP Port : <input name=port value=" + getCfg(cfg, "port") + "><br>");
    html.append("RTP Port Min : <input name=rtpmin value=" + getCfg(cfg, "rtpmin") + "> (1024-64534) (default:32768)<br>");
    html.append("RTP Port Max : <input name=rtpmax value=" + getCfg(cfg, "rtpmax") + "> (2023-65535) (default:65535)<br>");
    html.append("RTP Port Range must include at least 1000 ports and start on an even port number.<br>");
    html.append("<input type=checkbox name=anon " + checked(getCfg(cfg, "anon")) + "> Anonymous Inbound Calls (allows calls from any source to extensions, voicemail, IVRs, etc.)<br>");
    html.append("<input type=checkbox name=route " + checked(getCfg(cfg, "route")) + "> Route Calls (route calls from one trunk to another) [not implemented yet]<br>");
    html.append("Video Conference Codecs : <input name=videoCodecs value=" + getCfg(cfg, "videoCodecs") + "> [comma list : H263,H263-1998,H263-2000,H264,VP8]<br>");
    html.append("<input type=checkbox name=relayAudio " + checked(getCfg(cfg, "relayAudio")) + "> Relay Audio Media (recommended)<br>");
    html.append("<input type=checkbox name=relayVideo " + checked(getCfg(cfg, "relayVideo")) + "> Relay Video Media (optional)<br>");
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
    html.append(select("moh", getCfg(cfg, "moh"), wavFiles.toArray(new String[0])));
    html.append(" (use " + link("core", "msgs", "", "Messages") + " page to upload new files)<br>");
    html.append("Web HTTP Port : <input name=http value=" + getCfg(cfg, "http") + "><br>");
    html.append("Web HTTPS Port : <input name=https value=" + getCfg(cfg, "https") + "><br>");
    html.append("<input type=checkbox name=hideAdmin " + checked(getCfg(cfg, "hideAdmin")) + "> Hide Admin Link on Home Page<br>");
    html.append("<input type=checkbox name=disableWebRTC " + checked(getCfg(cfg, "disableWebRTC")) + "> Disable WebRTC support<br>");
    html.append("<br>");
    html.append("<input type=submit value='Save'>");
    html.append("</form>");
    html.append("<br><br><br>");
    html.append("Secure Web SSL Cerificate:<br><br>");
    html.append("Generate self-signed Key/Certificate Pair:<br>");
    html.append(form("core", "settings"));
    html.append("dname:<input name=dname value='CN=" + req.getHost() + ", OU=jpbxlite, O=JavaForce, C=CA' size=50><br>");
    html.append("valid(days):<input name=valid value='3650' size=10><br>");
    html.append("<input type=hidden name='verb' value='sslSelf'>");
    html.append("<input type=submit value='Generate'>");
    html.append("</form><br><br>");
    html.append("All options on this page will not take effect until the server is restarted.<br>");
    html.append("</div>");

    return html.toString();
  }

  private String doStatusPage(SQL sql, String args[]) {
    String verb = "", msg = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
    }
    if (verb.equals("initdb") || verb.equals("upgradedb")) {
      String version;
      version = sql.select1value("SELECT value FROM config WHERE id='version'");
      if (version == null) version = "0.0";
      Float fversion = Float.valueOf(version);
      //setup init DB (ignore errors -- it is repeatable)
      if (fversion == 0.91) {
        sql.execute("DROP TABLE queues");  //test
      }
      sql.execute("CREATE TABLE svcplugins (jar VARCHAR(32), cls VARCHAR(32), UNIQUE (jar, cls))");
      sql.execute("INSERT INTO svcplugins (jar, cls) VALUES ('extensions.jar', 'core.Extensions')");
      sql.execute("INSERT INTO svcplugins (jar, cls) VALUES ('trunks.jar', 'core.Trunks')");
      sql.execute("INSERT INTO svcplugins (jar, cls) VALUES ('voicemail.jar', 'core.VoiceMail')");
      sql.execute("INSERT INTO svcplugins (jar, cls) VALUES ('ivrs.jar', 'core.IVR')");
      sql.execute("INSERT INTO svcplugins (jar, cls) VALUES ('queues.jar', 'core.Queues')");
      sql.execute("CREATE TABLE webplugins (plugin VARCHAR(16), pg VARCHAR(16), cat VARCHAR(32), display VARCHAR(32), UNIQUE (plugin, pg))");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'status', 'General', 'System Status')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'admin', 'General', 'Administrator')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'exts', 'General', 'Extensions')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'ivrs', 'General', 'IVR')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'conf', 'General', 'Conference')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'msgs', 'General', 'Messsages')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'trunks', 'General', 'Trunks')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'settings', 'General', 'Settings')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'outroutes', 'Routing', 'Outbound Routes')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'inroutes', 'Routing', 'Inbound Routes')");
      sql.execute("INSERT INTO webplugins (plugin, pg, cat, display) VALUES ('core', 'queues', 'General', 'Queues')");
      sql.execute("CREATE TABLE webpluginusers (plugin VARCHAR(16), pg VARCHAR(16), userid VARCHAR(16), UNIQUE (plugin, pg, userid))");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'status', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'admin', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'exts', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'ivrs', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'conf', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'msgs', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'trunks', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'settings', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'outroutes', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'inroutes', 'admin')");
      sql.execute("INSERT INTO webpluginusers (plugin, pg, userid) VALUES ('core', 'queues', 'admin')");
      sql.execute("CREATE TABLE config (id VARCHAR(32) NOT NULL UNIQUE, value VARCHAR(256))");
      sql.execute("DELETE FROM config WHERE id='version'");
      sql.execute("INSERT INTO config (id, value) VALUES ('version', '" + dbVersion + "')");
      sql.execute("INSERT INTO config (id, value) VALUES ('port', '5060')");
      sql.execute("INSERT INTO config (id, value) VALUES ('rtpmin', '32768')");
      sql.execute("INSERT INTO config (id, value) VALUES ('rtpmax', '65535')");
      sql.execute("INSERT INTO config (id, value) VALUES ('anon', 'false')");
      sql.execute("INSERT INTO config (id, value) VALUES ('route', 'false')");
      sql.execute("INSERT INTO config (id, value) VALUES ('defaultoutroutetable', 'default')");
      sql.execute("INSERT INTO config (id, value) VALUES ('binpath', 'invalid')");  //each time the service starts it will set this value
      sql.execute("INSERT INTO config (id, value) VALUES ('videoCodecs', 'H264,VP8')");
      sql.execute("INSERT INTO config (id, value) VALUES ('relayAudio', 'true')");
      sql.execute("INSERT INTO config (id, value) VALUES ('relayVideo', 'true')");
      sql.execute("INSERT INTO config (id, value) VALUES ('moh', '')");
      sql.execute("INSERT INTO config (id, value) VALUES ('http', '80')");
      sql.execute("INSERT INTO config (id, value) VALUES ('https', '443')");
      sql.execute("INSERT INTO config (id, value) VALUES ('hideAdmin', 'false')");
      sql.execute("INSERT INTO config (id, value) VALUES ('disableWebRTC', 'false')");
      sql.execute("CREATE TABLE notices (msg VARCHAR(64) NOT NULL, plugin VARCHAR(16), pg VARCHAR(16))");
      sql.execute("CREATE TABLE exts (ext VARCHAR(16) NOT NULL UNIQUE, display VARCHAR(64) NOT NULL, cid VARCHAR(16), pass VARCHAR(16) NOT NULL)");
      sql.execute("CREATE TABLE extopts (ext VARCHAR(16) NOT NULL, id VARCHAR(16) NOT NULL, value VARCHAR(32), UNIQUE (ext, id))");  //voicemail, routetable, block1, block011, etc.
      sql.execute("CREATE TABLE trunks (trunk VARCHAR(16) NOT NULL UNIQUE, host VARCHAR(255) NOT NULL, cid VARCHAR(16) NOT NULL DEFAULT '0000000000', register VARCHAR(512), outrules CLOB, inrules CLOB, routetable VARCHAR(16) NOT NULL DEFAULT 'default')");
      sql.execute("CREATE TABLE outroutetables (routetable VARCHAR(16) NOT NULL UNIQUE)");
      sql.execute("INSERT INTO outroutetables (routetable) VALUES ('default')");  //can not be deleted/edited
      sql.execute("CREATE TABLE outroutes (routetable VARCHAR(16) NOT NULL, route VARCHAR(16) NOT NULL, cid VARCHAR(16), patterns CLOB NOT NULL, trunks CLOB NOT NULL, priority INT NOT NULL, UNIQUE (routetable, route))");
      sql.execute("CREATE TABLE inroutes (route VARCHAR(16) NOT NULL UNIQUE, did VARCHAR(16), cid VARCHAR(16), dest VARCHAR(16))");
      sql.execute("CREATE TABLE ivrs (ext VARCHAR(16) NOT NULL UNIQUE, display VARCHAR(64) NOT NULL, script CLOB NOT NULL)");
      sql.execute("CREATE TABLE queues (ext VARCHAR(16) NOT NULL UNIQUE, display VARCHAR(64) NOT NULL, agents CLOB NOT NULL, message VARCHAR(256) NOT NULL)");
      if (verb.equals("initdb")) msg = "Database created"; else msg = "Database upgraded";
    }
    StringBuilder html = new StringBuilder();

    if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font>");
    html.append("<div class=table>");
    html.append("<div class=notice>Notices</div>");
    String res = sql.select1value("SELECT value FROM config WHERE id='version'");
    if (res == null) {
      html.append("<div class=noticeitem>");
      html.append(link("core", "status", "verb=initdb", "Database not initialized"));
      html.append("</div>");
    } else if (!res.equals(dbVersion)) {
      html.append("<div class=noticeitem>");
      html.append(link("core", "status", "verb=upgradedb", "Database upgrade required"));
      html.append("</div>");
    }
    res = sql.select1value("SELECT passmd5 FROM users WHERE userid='admin'");
    if ((res == null) || (res.equals("21232f297a57a5a743894a0e4a801fc3"))) {
      html.append("<div class=noticeitem>");
      html.append(link("core", "admin", "", "Default Admin Password in use"));
      html.append("</div>");
    }
    String notices[][] = sql.select("SELECT msg, plugin, pg FROM notices");
    if (notices != null) {
      for(int a=0;a<notices.length;a++) {
        html.append("<div class=noticeitem>");
        if ((notices[a][1] == null) || (notices[a][2] == null)) {
          html.append(notices[a][0]);
        } else {
          html.append(link(notices[a][1], notices[a][2], "", notices[a][0]));
        }
        html.append("</div>");
      }
    }
    html.append("</div>");

    return html.toString();
  }

  private String doTrunksPage(SQL sql, String args[]) {
    String verb = "", trunk = "", cid = "", host = "", register = "", msg = "", outrules = "", inrules = "", edittrunk= "", sure = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("verb=")) verb = args[a].substring(x);
      if (args[a].startsWith("trunk=")) trunk = args[a].substring(x);
      if (args[a].startsWith("edittrunk=")) edittrunk = args[a].substring(x);
      if (args[a].startsWith("cid=")) cid = args[a].substring(x);
      if (args[a].startsWith("host=")) host = args[a].substring(x);
      if (args[a].startsWith("register=")) register = args[a].substring(x);
      if (args[a].startsWith("outrules=")) outrules = args[a].substring(x);
      if (args[a].startsWith("inrules=")) inrules = args[a].substring(x);
      if (args[a].startsWith("sure=")) sure = args[a].substring(x);
    }
    cid = numbersOnly(cid);
    if (trunk.length() == 0) verb = "";
    if (verb.equals("del")) {
      if (sure.equalsIgnoreCase("on")) {
        if (sql.execute("DELETE FROM trunks WHERE trunk=" + sql.quote(trunk))) {
          msg = "Trunk deleted";
        } else {
          msg = "Failed to delete";
        }
      } else {
        msg = "Please confirm delete action";
      }
      trunk = "";
    }
    if (verb.equals("view")) {
      edittrunk = trunk;
      String trunks[] = sql.select1row("SELECT host,cid,outrules,inrules,register FROM trunks WHERE trunk=" + sql.quote(trunk));
      if (trunks != null) {
        host = trunks[0];
        cid = trunks[1];
        outrules = trunks[2];
        inrules = trunks[3];
        register = trunks[4];
      }
    }
    if (verb.equals("add")) {
      outrules = convertString(outrules).replaceAll("\r\n", ":");
      outrules = patternsOnly(outrules);
      inrules = convertString(inrules).replaceAll("\r\n", ":");
      inrules = patternsOnly(inrules);
      register = convertString(register);
      if (sql.execute("INSERT INTO trunks (trunk, host, cid, outrules, inrules, register) VALUES (" + sql.quote(trunk) + "," + sql.quote(host) + "," + sql.quote(cid) + ","
      + sql.quote(outrules) + "," + sql.quote(inrules) + "," + sql.quote(register) + ")")) {
        msg = "Trunk added";
        trunk = "";
        host = "";
        cid = "";
        register = "";
        outrules = "";
        inrules = "";
      } else {
        msg = "Failed to add:" + sql.getLastException();
      }
    }
    if (verb.equals("edit")) {
      outrules = convertString(outrules).replaceAll("\r\n", ":");
      outrules = patternsOnly(outrules);
      inrules = convertString(inrules).replaceAll("\r\n", ":");
      inrules = patternsOnly(inrules);
      register = convertString(register);
      if (sql.execute("UPDATE trunks SET trunk=" + sql.quote(trunk) + ",host=" + sql.quote(host) + ",cid=" + sql.quote(cid) + ",outrules=" + sql.quote(outrules)
      + ",inrules=" + sql.quote(inrules) + ",register=" + sql.quote(register) + " WHERE trunk=" + sql.quote(edittrunk))) {
        msg = "Trunk edited";
        trunk = "";
        host = "";
        cid = "";
        register = "";
        outrules = "";
        inrules = "";
      } else {
        msg = "Failed to edit:" + sql.getLastException();
        verb = "view";
      }
    }
    StringBuilder html = new StringBuilder();

    html.append("<table height=100%>");
    html.append("<tr><td width=100%>");
    if (msg.length() > 0) html.append("<font color=#ff0000>" + msg + "</font><br>");
    if (verb.equals("view")) {
      html.append(form("core", "trunks") + "<input type=hidden name=verb value=del><input type=hidden name=trunk value=" + sql.quote(trunk));
      html.append("><input type=submit value='Delete Trunk'>" + "<input type=checkbox name=sure>I'm Sure</form>");
    }
    html.append(form("core", "trunks"));
    html.append("<input type=hidden name=verb value=" + (verb.equals("view") ? "edit" : "add") + ">");
    if (verb.equals("view")) html.append("<input type=hidden name=edittrunk value=" + sql.quote(edittrunk) + ">");
    html.append("<table>");
    html.append("<tr><td> Trunk: </td><td> <input name=trunk value=" + sql.quote(trunk) + "</td></tr>");
    html.append("<tr><td> Host: </td><td> <input name=host value=" + sql.quote(host) + "></td><td>domain_or_ip[:port] (default port = 5060)</td></tr>");
    html.append("<tr><td nowrap> Override Caller ID: </td><td> <input name=cid value=" + sql.quote(cid) + "></td><td>(optional)</td></tr>");
    html.append("<tr><td> Register String: </td><td> <input name=register value=" + sql.quote(register) + "></td><td>(optional) [user:pass@host/did]</td></tr>");
    html.append("<tr><td> Dial Out Rules: </td><td> <textarea name=outrules cols=20 rows=10>");
      String lns[] = outrules.split(":");
      for(int a=0;a<lns.length;a++) html.append(lns[a]);
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
    lns = inrules.split(":");
    for(int a=0;a<lns.length;a++) html.append(lns[a]);
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
      String trunks[] = sql.select1col("SELECT trunk FROM trunks");
      if (trunks != null) {
        for(int a=0;a<trunks.length;a++) {
          html.append("<div class=menuitem>" + link("core", "trunks", "verb=view&trunk=" + trunks[a], "&lt;" + trunks[a] + "&gt;") + "</div>");
        }
      }
    html.append("</td></tr></table>");

    return html.toString();
  }

  private String getPluginPage(String plugin, String pg, SQL sql, String args[], WebRequest req) {
    //currently plugin is only "core" - the idea was to allow other plugins - not likely
    if (!plugin.equals("core")) return "Plugin does not exist";
    if (pg.equals("blank")) {
      return doBlankPage(sql, args);
    }
    else if (pg.equals("admin")) {
      return doAdminPage(sql, args);
    }
    else if (pg.equals("conf")) {
      return doConfPage(sql, args);
    }
    else if (pg.equals("exts")) {
      return doExtsPage(sql, args);
    }
    else if (pg.equals("inroutes")) {
      return doInRoutesPage(sql, args);
    }
    else if (pg.equals("ivrs")) {
      return doIVRPage(sql, args);
    }
    else if (pg.equals("msgs")) {
      return doMsgsPage(sql, args, req);
    }
    else if (pg.equals("outroutes")) {
      return doOutRoutesPage(sql, args);
    }
    else if (pg.equals("settings")) {
      return doSettingsPage(sql, args, req);
    }
    else if (pg.equals("status")) {
      return doStatusPage(sql, args);
    }
    else if (pg.equals("trunks")) {
      return doTrunksPage(sql, args);
    }
    else if (pg.equals("queues")) {
      return doQueuesPage(sql, args);
    }
    return "Page does not exist";
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
    SQL sql = new SQL();
    if (!sql.connect(Service.jdbc)) {
      res.write("SQL connection failed".getBytes());
      return;
    }
    String plugin = "core";
    String pg;
    if (isAdmin(id)) {
      pg = "status";
    } else {
      pg = "blank";  //TODO : voicemail for normal users ???
    }
    for(int a=0;a<args.length;a++) {
      if (args[a].startsWith("plugin=")) plugin = args[a].substring(7);
      if (args[a].startsWith("pluginpg=")) pg = args[a].substring(9);
    }
    if (!isAllowed(sql, id, plugin, pg)) { redir(res, "core", "blank"); return; }

    html.append("<div style='overflow: auto;'>");
    html.append("<table border=0 width=100% height=100% cellpadding=0 cellspacing=0>");
    html.append("<tr height=64><td width=100% colspan=2><a href='http://jpbxlite.sourceforge.net'><img border=0 src=/static/img/logo.png></a>");
    html.append("<a href='/logout' style='float:right;'>Logout</a></td></tr>");
    html.append("<tr><td style='width: 180px; vertical-align:top;'>");
    html.append(listPlugins(sql, id, plugin, pg));
    html.append("</td><td style='vertical-align:top; width: 100%;'>");
    html.append("<div style='border-top: 2px solid #000000; border-left: 2px solid #000000; height: 100%;'>");
    html.append(getPluginPage(plugin, pg, sql, args, req));
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

    sql.close();
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
  public String listPlugins(SQL sql, String id, String plugin, String pluginpg) {
    String res[][] = sql.select("SELECT webplugins.plugin,webplugins.pg,webplugins.display,webplugins.cat FROM webplugins JOIN webpluginusers" +
      " ON webplugins.plugin = webpluginusers.plugin AND webplugins.pg = webpluginusers.pg" +
      " WHERE webpluginusers.userid=" + sql.quote(id) + " OR webpluginusers.userid='*'" +
      " ORDER BY webplugins.cat");
    if (res == null) {
      if (isAdmin(id)) {
        //allow init setup
        res = new String[1][4];
        res[0][0] = "core";
        res[0][1] = "status";
        res[0][2] = "System Status (init)";
        res[0][3] = "General";
      } else {
        return "";
      }
    }
    StringBuffer list = new StringBuffer();
    String lastCat = "";
//    list.append("length=" + res.length);
    for(int a=0;a<res.length;a++) {
      if (!res[a][3].equals(lastCat)) {
        lastCat = res[a][3];
        list.append("<div class=menucat>" + res[a][3] + "</div>");
      }
      if (plugin.equalsIgnoreCase(res[a][0]) && pluginpg.equalsIgnoreCase(res[a][1])) {
        list.append("<div class=menuitemselected>");
      } else {
        list.append("<div class=menuitem>");
      }
      list.append("<a href='?plugin=" + res[a][0] + "&pluginpg=" + res[a][1] + "'>" + res[a][2] + "</a></div>");
    }
    return list.toString();
  }
  public boolean isAdmin(String id) {
    return id.equals("admin");
  }
  public boolean isAllowed(SQL sql, String id, String plugin, String pluginpg) {
    if (isAdmin(id)) return true;
    String res[][] = sql.select("SELECT plugin,pg FROM webpluginusers WHERE (userid=" + sql.quote(id) + " OR userid='*') AND plugin=" + sql.quote(plugin) +
      " AND pg=" + sql.quote(pluginpg));
    return (res.length > 0);
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
  public String convertString(String instr) {
    //convert WebString to normal strings (expand %## codes, '+'->' ')
    String outstr = "";
    char ca[] = instr.toCharArray();
    int h1, h2;
    char ch;
    for(int a=0;a<ca.length;a++) {
      switch (ca[a]) {
        case '%':
          if (a+2>=ca.length) return outstr;
          if ((ca[a+1] >= '0') && (ca[a+1] <= '9')) h1 = ca[a+1] - '0';
          else if ((ca[a+1] >= 'a') && (ca[a+1] <= 'f')) h1 = ca[a+1] - ('a' - 10);
          else if ((ca[a+1] >= 'A') && (ca[a+1] <= 'F')) h1 = ca[a+1] - ('A' - 10);
          else h1 = 0;
          if ((ca[a+2] >= '0') && (ca[a+2] <= '9')) h2 = ca[a+2] - '0';
          else if ((ca[a+2] >= 'a') && (ca[a+2] <= 'f')) h2 = ca[a+2] - ('a' - 10);
          else if ((ca[a+2] >= 'A') && (ca[a+2] <= 'F')) h2 = ca[a+2] - ('A' - 10);
          else h2 = 0;
          ch = (char)(h1 * 0x10 + h2);
          if (ch == '\"') ch = '\'';
          outstr += ch;
          a+=2;
          break;
        case '+': outstr += " "; break;
        default: outstr += ca[a]; break;
      }
    }
    return outstr;
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
