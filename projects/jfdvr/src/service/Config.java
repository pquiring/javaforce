package service;

/**
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.io.*;

public class Config extends SerialObject implements Serializable {
  public static final long serialVersionUID = 1;
  public static Config current;

  public Camera cameras[] = new Camera[0];
  public Group groups[] = new Group[0];
  public String user = "dvr";
  public String pass = "password";

  public Object camerasLock = new Object();
  public Object groupsLock = new Object();

  public static void load() {
    String file = Paths.dataPath + "/config.dat";
    try {
      boolean isJavaSerial = isJavaSerialObject(file);
      FileInputStream fis = new FileInputStream(file);
      if (isJavaSerial) {
        JFLog.log("Upgrading config...");
        ObjectInputStream ois = new ObjectInputStream(fis);
        current = (Config)ois.readObject();
        fis.close();
        save();
      } else {
        ObjectReader or = new ObjectReader(fis);
        current = (Config)or.readObject(new Config());
        fis.close();
      }
    } catch (FileNotFoundException e) {
      current = new Config();
      JFLog.log("No config found!");
    } catch (Exception e) {
      current = new Config();
      JFLog.log(e);
    }
  }

  public static void save() {
    try {
      FileOutputStream fos = new FileOutputStream(Paths.dataPath + "/config.dat");
      ObjectWriter ow = new ObjectWriter(fos);
      ow.writeObject(current);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void addCamera(Camera camera) {
    synchronized (camerasLock) {
      cameras = Arrays.copyOf(cameras, cameras.length + 1);
      cameras[cameras.length-1] = camera;
    }
  }

  public void addGroup(Group group) {
    synchronized (groupsLock) {
      groups = Arrays.copyOf(groups, groups.length + 1);
      groups[groups.length-1] = group;
    }
  }

  public void removeCamera(Camera camera) {
    synchronized (camerasLock) {
      int idx = -1;
      for(int a=0;a<cameras.length;a++) {
        if (cameras[a] == camera) {idx = a; break;}
      }
      if (idx == -1) return;
      cameras = (Camera[])JF.copyOfExcluding(cameras, idx);
    }
  }

  public void removeGroup(Group group) {
    synchronized (groupsLock) {
      int idx = -1;
      for(int a=0;a<groups.length;a++) {
        if (groups[a] == group) {idx = a; break;}
      }
      if (idx == -1) return;
      groups = (Group[])JF.copyOfExcluding(groups, idx);
    }
  }

  public Camera getCamera(String name) {
    for(int a=0;a<cameras.length;a++) {
      if (cameras[a].name.equals(name)) {
       return cameras[a];
      }
    }
    return null;
  }

  public Group getGroup(String name) {
    for(int a=0;a<groups.length;a++) {
      if (groups[a].name.equals(name)) {
       return groups[a];
      }
    }
    return null;
  }

  //RTSP ports for service : 5000-5999
  private static int nextPort = 5000;
  public static synchronized int getLocalPort() {
    if (nextPort >= 6000) nextPort = 5000;
    int port = nextPort;
    nextPort += 2;
    return port;
  }

  public static final short id_cameras = id_array + 1;
  public static final short id_groups = id_array + 2;
  public static final short id_user = id_len + 1;
  public static final short id_pass = id_len + 2;

  public void readObject() throws Exception {
    int cnt;
    do {
      short id = readShort();
      switch (id) {
        case id_cameras:
          cnt = readInt();
          cameras = new Camera[cnt];
          for(int a=0;a<cnt;a++) {
            cameras[a] = new Camera();
            cameras[a].readInit(this);
            cameras[a].readObject();
          }
          break;
        case id_groups:
          cnt = readInt();
          groups = new Group[cnt];
          for(int a=0;a<cnt;a++) {
            groups[a] = new Group();
            groups[a].readInit(this);
            groups[a].readObject();
          }
          break;
        case id_user:
          user = readString();
          break;
        case id_pass:
          pass = readString();
          break;
        case id_end: return;
        default: skipChunk(id); break;
      }
    } while (true);
  }

  public void writeObject() throws Exception {
    int cnt;
    writeShort(id_cameras);
    cnt = cameras.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      cameras[a].writeInit(this);
      cameras[a].writeObject();
    }
    writeShort(id_groups);
    cnt = groups.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      groups[a].writeInit(this);
      groups[a].writeObject();
    }
    writeShort(id_user);
    writeString(user);
    writeShort(id_pass);
    writeString(pass);
    writeShort(id_end);
  }
}
