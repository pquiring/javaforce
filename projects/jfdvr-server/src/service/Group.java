package service;

/** Group.
 *
 * A collection of cameras for viewing.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.io.*;

public class Group implements Serializable {
  public static final long serialVersionUID = 1;

  public String name;
  public String[] cameras = new String[0];

  public void add(String camera) {
    cameras = Arrays.copyOf(cameras, cameras.length + 1);
    cameras[cameras.length-1] = camera;
  }

  public void remove(String camera) {
    int idx = -1;
    for(int a=0;a<cameras.length;a++) {
      if (cameras[a] == camera) {idx = a; break;}
    }
    if (idx == -1) return;
    cameras = (String[])JF.copyOfExcluding(cameras, idx);
  }

  public void moveUp(int idx) {
    String c1 = cameras[idx];
    String c2 = cameras[idx - 1];
    cameras[idx] = c2;
    cameras[idx - 1] = c1;
  }

  public void moveDown(int idx) {
    String c1 = cameras[idx];
    String c2 = cameras[idx + 1];
    cameras[idx] = c2;
    cameras[idx + 1] = c1;
  }

  public boolean contains(String camera) {
    for(int a=0;a<cameras.length;a++) {
      if (cameras[a].equals(camera)) {
        return true;
      }
    }
    return false;
  }

  public String getCameraList() {
    StringBuilder sb = new StringBuilder();
    sb.append("cameras: ");
    int cnt = cameras.length;
    for(int a=0;a<cnt;a++) {
      if (a > 0) sb.append(",");
      sb.append(cameras[a]);
    }
    return sb.toString();
  }
}
