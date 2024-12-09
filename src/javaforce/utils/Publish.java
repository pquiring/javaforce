package javaforce.utils;

/** Publish to maven central (sonatype.org)
 *
 * Auth Tokens : ${user.home}/.m2/settings.xml
 *
 * https://central.sonatype.org/publish/publish-portal-api/
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Publish {
  public static void main(String[] args) {
    new Publish().publish(args);
  }
  public static boolean debug = true;
  private void usage() {
    System.out.println("Usage : javaforce.utils.Publish central-bundle.zip");
  }
  private void error(String msg) {
    System.out.println("Error : javaforce.utils.Publish : " + msg);
    System.exit(1);
  }
  private BuildTools settings = new BuildTools();
  private String user = "";
  private String pass = "";
  private boolean readXML() {
    settings.loadXML(System.getProperty("user.home") + "/.m2/settings.xml");
    user = settings.getTag(new String[] {"settings", "servers", "server", "username"});
    if (user == null) error("settings.xml:username not found");
    if (debug) JFLog.log("user=" + user);
    pass = settings.getTag(new String[] {"settings", "servers", "server", "password"});
    if (pass == null) error("settings.xml:password not found");
    if (debug) JFLog.log("pass=" + pass);
    return true;
  }
  private String getBearer() {
    String in = user + ":" + pass;
    String out = new String(Base64.encode(in.getBytes()));
    if (debug) JFLog.log("bearer=" + out);
    return out;
  }
  public void publish(String[] args) {
    if (args.length == 0) {
      usage();
      return;
    }
    if (!readXML()) {
      return;
    }
    try {
      File file = new File(args[0]);
      if (!file.exists()) {
        error(args[0] + " not found");
      }
      FileInputStream fis = new FileInputStream(file);
      byte[] bundle = fis.readAllBytes();
      fis.close();
      HTTP.Part part = new HTTP.Part();
      part.name = "bundle";
      part.filename = "central-bundle.zip";
      part.mimeType = HTTP.partTypeStream;
      part.data = bundle;
      HTTP http = new HTTPS();
      http.setHeader("Authorization", "Bearer " + getBearer());
      http.open("central.sonatype.com");
      byte[] deploymentID = http.post("/api/v1/publisher/upload?publishingType=USER_MANAGED", new HTTP.Part[] {part});
      System.out.println("DeploymentID=" + new String(deploymentID));
      http.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
