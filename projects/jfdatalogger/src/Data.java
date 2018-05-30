/**
 *
 * @author User
 */

import java.io.*;

import javaforce.*;

public class Data implements Serializable {
  public static long serialVersionUID = 1;
  public int tagCount;
  public int rowCount;
  public String[] tags;
  public String[][] data;
  public byte[] save() {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(this);
      return baos.toByteArray();
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
  public static Data load(byte[] buf) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(buf);
      ObjectInputStream ois = new ObjectInputStream(bais);
      return (Data)ois.readObject();
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
}
