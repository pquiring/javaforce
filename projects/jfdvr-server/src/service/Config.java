package service;

/** Config
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.io.*;

public class Config implements Serializable {
  public static final long serialVersionUID = 1;
  public static Config current;

  public Camera cameras[] = new Camera[0];
  public Group groups[] = new Group[0];
  public String user = "dvr";
  public String pass = "password";

  public transient Object camerasLock;
  public transient Object groupsLock;

  public Config() {
    init();
  }

  /** init transient fields. */
  private void init() {
    camerasLock = new Object();
    groupsLock = new Object();
  }

  public static void load() {
    String file = Paths.dataPath + "/config.dat";
    try {
      FileInputStream fis = new FileInputStream(file);
      ObjectInputStream ois = new ObjectInputStream(fis);
      current = (Config)ois.readObject();
      fis.close();
      current.init();
      for(Camera camera : current.cameras) {
        camera.init();
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
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(current);
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
    //remove from groups
    for(Group group : groups) {
      if (group.contains(camera.name)) {
        group.remove(camera.name);
      }
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

  public void renamedCamera(String oldName, String newName) {
    for(int a=0;a<groups.length;a++) {
      Group group = groups[a];
      for(int b=0;b<group.cameras.length;b++) {
        if (group.cameras[b].equals(oldName)) {
          group.cameras[b] = newName;
        }
      }
    }
  }

  private static int next_log = 1;
  /* Returns next log id for JFLog */
  public static synchronized int nextLog() {
    return next_log++;
  }

  //RTSP ports for service : 5000-5999
  private static int nextPort = 5000;
  public static synchronized int getLocalPort() {
    if (nextPort >= 6000) nextPort = 5000;
    int port = nextPort;
    nextPort += 2;
    return port;
  }
}
