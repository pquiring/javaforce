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

  public HeatMap(int x, int y, String ssid) {
    this.x = x;
    this.y = y;
    this.ssid = ssid;
    map = new int[y][x];
  }

  public void save(String filename) {
    StringBuilder sb = new StringBuilder();
    sb.append("ssid=" + ssid + JF.eol);
    sb.append("x=" + x + JF.eol);
    sb.append("y=" + y + JF.eol);
    for(int _y=0;_y<y;_y++) {
      sb.append("row=");
      for(int _x=0;_x<x;_x++) {
        if (_x > 0) sb.append(",");
        sb.append(Integer.toString(map[_y][_x]));
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

  private void reset() {
    x = -1;
    y = -1;
    ssid = null;
    map = null;
  }

  public void load(String filename) {
    byte[] data = null;
    try {
      FileInputStream fis = new FileInputStream(filename);
      data = fis.readAllBytes();
      fis.close();
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    reset();
    int xp = 0;
    int yp = 0;
    String[] lns = new String(data).split(JF.eol);
    for(String ln : lns) {
      int idx = ln.indexOf('=');
      if (idx == -1) continue;
      String tag = ln.substring(0, idx);
      String value = ln.substring(idx + 1);
      switch (tag) {
        case "x": x = Integer.valueOf(value); break;
        case "y": y = Integer.valueOf(value); break;
        case "ssid": ssid = value; break;
        case "row":
          if (map == null) {
            map = new int[y][x];
          }
          String[] xs = value.split("[,]");
          xp = 0;
          for(String xv : xs) {
            map[yp][xp++] = Integer.valueOf(xv);
          }
          yp++;
          break;
      }
    }
  }
}
