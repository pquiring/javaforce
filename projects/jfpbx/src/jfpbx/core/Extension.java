package jfpbx.core;

public class Extension {
  public Extension(String ext, long expires, String remoteip, int remoteport) {
    this.ext = ext;
    this.expires = expires;
    this.remoteip = remoteip;
    this.remoteport = remoteport;
  }
  public String ext;
  public long expires;
  public String remoteip;
  public int remoteport;
}
