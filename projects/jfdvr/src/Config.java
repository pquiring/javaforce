/**
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Config implements Serializable {
  public static final long serialVersionUID = 1;
  public static Config current;

  public Camera cameras[] = new Camera[0];

  public static void load() {
    try {
      FileInputStream fis = new FileInputStream(Paths.dataPath + "/config.dat");
      ObjectInputStream ois = new ObjectInputStream(fis);
      current = (Config)ois.readObject();
      fis.close();
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
    cameras = Arrays.copyOf(cameras, cameras.length + 1);
    cameras[cameras.length-1] = camera;
  }

  public void removeCamera(Camera camera) {
    int idx = -1;
    for(int a=0;a<cameras.length;a++) {
      if (cameras[a] == camera) {idx = a; break;}
    }
    if (idx == -1) return;
    cameras = (Camera[])JF.copyOfExcluding(cameras, idx);
  }
}
