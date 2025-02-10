/** HeatMap
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class HeatMap {
  public int x, y;  //size
  public String ssid;
  public int[][] map;  //dBm values [y][x]

  public void save(String filename) {
    StringBuilder sb = new StringBuilder();
    sb.append("ssid=" + ssid + JF.eol);
    sb.append("x=" + x + JF.eol);
    sb.append("y=" + y + JF.eol);
    for(int _y=0;_y<y;_y++) {
      sb.append("row=");
      for(int _x=0;_x<x;_x++) {
        if (_x > 0) sb.append(",");
        sb.append(Integer.toString(map[y][x]));
      }
      sb.append(JF.eol);
    }
    byte[] data = sb.toString().getBytes();
    try {
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(data);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void load(String filename) {
    //TODO
  }
}
