package javaforce.print;

/** JFPrintServer
 *
 * HTTP based print server on port 33202.
 *
 * Commands:
 *   /list - list available printers
 *   /query/{name} - query printer : returns list of properties
 *   /print/{name} - send print job (POST = png-data) : returns properties with 'job-id'
 *   /status/{jobid} - get print job status
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

import javaforce.service.*;

public class JFPrintServer {
  private Server server;

  private HashMap<String, DocPrintJob> jobs = new HashMap<>();

  public void start() {
    server = new Server();
    server.start();
  }

  public void stop() {
    server.cancel();
  }

  private class Server extends Thread implements WebHandler {
    private WebServer web;
    public void run() {
      web = new WebServer();
      web.start(this, 631);
    }

    public void cancel() {
      web.stop();
      web = null;
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
        String _width = req.getHeader("width");
        float width = Float.valueOf(_width);
        String _height = req.getHeader("height");
        float height = Float.valueOf(_height);
        String _unit = req.getHeader("unit");
        int unit = MediaPrintableArea.INCH;
        switch (_unit) {
          case JFPrint.unit_inch: unit = MediaPrintableArea.INCH; break;
          case JFPrint.unit_mm: unit = MediaPrintableArea.MM; break;
        }
        byte[] png = req.getData();

        PrintRequestAttributeSet attribSet = new HashPrintRequestAttributeSet();
        attribSet.add(new MediaPrintableArea(0,0,width,height,unit));
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
        try {res.write(e.toString().getBytes());} catch (Exception e2) {}
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
}
