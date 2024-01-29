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

public class Group extends SerialObject {
  public String name;
  public String[] cameras = new String[0];

  public static final short id_name = id_len + 1;
  public static final short id_list = id_len + 2;

  public void readObject() throws Exception {
    do {
      short id = readShort();
      switch (id) {
        case id_name: name = readString(); break;
        case id_list:
          int cnt = readInt();
          cameras = new String[cnt];
          for(int a=0;a<cnt;a++) {
            cameras[a] = readString();
          }
          break;
        case id_end: return;
        default: skipChunk(id); break;
      }
    } while (true);
  }

  public void writeObject() throws Exception {
    writeShort(id_name);
    writeString(name);
    writeShort(id_list);
    writeInt(cameras.length);
    for(int a=0;a<cameras.length;a++) {
      writeString(cameras[a]);
    }
    writeShort(id_end);
  }

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
