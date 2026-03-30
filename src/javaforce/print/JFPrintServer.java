package javaforce.print;

/** JFPrintServer
 *
 * HTTP based print server on port 33202.
 *
 * See JFPrint for client side.
 *
 * Commands:
 *   /list - list available printers
 *   /query/{name} - query printer : returns list of properties
 *   /print/{name} - send print job (POST = png-data) : returns properties with 'jobid'
 *   /status/{jobid} - get print job status
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

import javaforce.*;
import javaforce.service.*;
import javaforce.bus.*;

public class JFPrintServer {
  private static final int web_port = 33202;

  public final static String serviceBus = "javaforce.jfprint";

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfprint.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfprint.log";
  }

  private Server server;
  private HashMap<String, DocPrintJob> jobs = new HashMap<>();

  public void start() {
    stop();
    server = new Server();
    server.start();
  }

  public void stop() {
    if (server != null) {
      server.cancel();
      server = null;
    }
    if (busServer != null) {
      busServer.disconnect();
      busServer = null;
    }
  }

  private class Server extends Thread implements WebHandler {
    private WebServer web;
    public void run() {
      JFLog.append(getLogFile(), true);
      JFLog.setRetention(30);
      JFLog.log("JFPrint : Starting service");
      loadConfig();
      busServer = new JBusServer(serviceBus, new JBusMethods());
      busServer.connect();
      web = new WebServer();
      web.start(this, web_port);
    }

    public void cancel() {
      if (web != null) {
        web.stop();
        web = null;
      }
      if (busServer != null) {
        busServer.disconnect();
        busServer = null;
      }
    }

    private String getHeader(WebRequest req, String name, String defaultValue) {
      String value = req.getHeader(name);
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }

    public void doPost(WebRequest req, WebResponse res) {
      try {
        String url = req.getURL();  // /print/name
        String[] ps = url.substring(1).split("/");
        if (ps.length < 2) {
          res.write("error:invalid url".getBytes());
          return;
        }
        String cmd = ps[0];
        if (!cmd.equals("print")) {
          res.write("error:unknown command".getBytes());
          return;
        }
        String name = ps[1];
        String _width = getHeader(req, "width", "8.5");
        float width = Float.valueOf(_width);
        String _height = getHeader(req, "height", "11.0");
        float height = Float.valueOf(_height);
        String _unit = getHeader(req, "unit", JFPrint.unit_inch);
        int unit = MediaPrintableArea.INCH;
        switch (_unit) {
          case JFPrint.unit_inch: unit = MediaPrintableArea.INCH; break;
          case JFPrint.unit_mm: unit = MediaPrintableArea.MM; break;
        }
        OrientationRequested orientation = OrientationRequested.PORTRAIT;
        String _orientation = getHeader(req, "orientation", JFPrint.orientation_portrait);
        switch (_orientation) {
          case JFPrint.orientation_landscape: orientation = OrientationRequested.LANDSCAPE; break;
          case JFPrint.orientation_portrait: orientation = OrientationRequested.PORTRAIT; break;
        }
        byte[] png = req.getData();
        JFLog.log("print:printer=" + name + ",width=" + width + ",height=" + height + ",unit=" + _unit + ",orientation=" + _orientation + ",png=" + png.length);

        PrintRequestAttributeSet attribSet = new HashPrintRequestAttributeSet();
        attribSet.add(new MediaPrintableArea(0,0,width,height,unit));
        attribSet.add(orientation);
        ByteArrayInputStream is = new ByteArrayInputStream(png);
        Doc myDoc = new SimpleDoc(is, DocFlavor.INPUT_STREAM.PNG, null);
        PrintService def = getPrinter(name);
        DocPrintJob job = def.createPrintJob();
        job.print(myDoc, attribSet);
        String jobid = Long.toString(System.currentTimeMillis());
        jobs.put(jobid, job);
        res.write(("jobid:" + jobid).getBytes());
        cleanJobs();
      } catch (Exception e) {
        String ex = e.toString();
        try {res.write(ex.getBytes());} catch (Exception e2) {}
        JFLog.log(ex);
      }
    }

    public void doGet(WebRequest req, WebResponse res) {
      try {
        String url = req.getURL();
        String[] ps = url.substring(1).split("/");
        String cmd = ps[0];
        switch (cmd) {
          case "list":
            res.write(list());
            break;
          case "query":
            res.write(query(ps[1]));
            break;
          case "status":
            res.write(status(ps[1]));
            break;
          default:
            res.write(("JFPrintServer Ready (JF/" + JF.getVersion() + ")").getBytes());
            break;
        }
        cleanJobs();
      } catch (Exception e) {
        try {res.write(e.toString().getBytes());} catch (Exception e2) {}
      }
    }

    private byte[] list() {
      StringBuilder sb = new StringBuilder();
      PrintService[] printers = getPrinters();
      for(PrintService print : printers) {
        String name = print.getName();
        int idx = name.lastIndexOf('\\');
        if (idx != -1) {
          name = name.substring(idx + 1);  //remove server name
        }
        sb.append(name);
        sb.append("\r\n");
      }
      return sb.toString().getBytes();
    }

    private byte[] query(String name) {
      PrintService printer = getPrinter(name);
      if (printer == null) return "error: not found".getBytes();
      return "status: wip".getBytes();
    }

    private byte[] status(String jobid) {
      return "status: wip".getBytes();
    }

    private void cleanJobs() {
      //TODO
    }
  }

  private PrintService getPrinter(String pname) {
    PrintService[] printers = PrintServiceLookup.lookupPrintServices(null, null);
    for(PrintService printer : printers) {
      String name = printer.getName();
      int idx = name.lastIndexOf('\\');
      if (idx != -1) {
        name = name.substring(idx + 1);  //remove server name
      }
      if (name.equals(pname)) {
        return printer;
      }
    }
    return null;
  }

  private PrintService[] getPrinters() {
    return PrintServiceLookup.lookupPrintServices(null, null);
  }

  private final static String defaultConfig = "#JFPrintServer";

  private void loadConfig() {
    try {
      StringBuilder cfg = new StringBuilder();
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\n");
        ln = ln.trim();
        int cmt = ln.indexOf('#');
        if (cmt != -1) ln = ln.substring(0, cmt).trim();
        if (ln.length() == 0) continue;
        int idx = ln.indexOf("=");
        if (idx == -1) continue;
        String key = ln.substring(0, idx).toLowerCase().trim();
        String value = ln.substring(idx+1).trim();
        switch (key) {
          //TODO
        }
      }
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(defaultConfig.getBytes());
        fos.close();
        config = defaultConfig;
      } catch (Exception e2) {
        JFLog.log(e2);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private JBusServer busServer;
  private String config = "#JFPrintServer";

  public static class JBusMethods {
    public String getConfig() {
      return service.config;
    }
    public boolean setConfig(String cfg) {
      //write new file
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(cfg.getBytes());
        fos.close();
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
    public boolean restart() {
      service.stop();
      service = new JFPrintServer();
      service.start();
      return true;
    }
  }

  private static JFPrintServer service;

  public static void serviceStart(String[] args) {
    service = new JFPrintServer();
    service.start();
  }

  public static void serviceStop() {
    JFLog.log("JFPrint : Stopping service");
    service.stop();
  }
}
