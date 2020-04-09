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

  public static final short id_cameras = id_array + 1;

  public void readObject() throws Exception {
    do {
      short id = readShort();
      switch (id) {
        case id_cameras:
          int cnt = readInt();
          cameras = new Camera[cnt];
          for(int a=0;a<cnt;a++) {
            cameras[a] = new Camera();
            cameras[a].readInit(this);
            cameras[a].readObject();
          }
          break;
        case id_end: return;
        default: skipChunk(id); break;
      }
    } while (true);
  }

  public void writeObject() throws Exception {
    JFLog.log("writeObject:Config");
    writeShort(id_cameras);
    int cnt = cameras.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      JFLog.log("writeObject:Camera:" + cameras[a].name);
      cameras[a].writeInit(this);
      cameras[a].writeObject();
    }
    writeShort(id_end);
  }
}
