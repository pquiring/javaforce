package javaforce;

/** XPM image api
 *
 * Supports XPM3 only.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.ui.*;

public class xpm {
  private int pos = 0;
  private byte data[];
  private String getToken() throws Exception {
    String ret = "", remark = "";
    boolean quote = false, comment = false;
    do {
      if (pos == data.length) throw new Exception("eof");
      char c = (char)data[pos++];
      if (quote) {
        if (c == '\"') {
          return ret;
        }
        ret += c;
      } else if (comment) {
        remark += c;
        if (remark.endsWith("*/")) {
          comment = false;
          remark = "";
        }
      } else {
        if (c == '/') {
          if (ret.length() > 0) {
            pos--;
            return ret;
          }
          comment = true;
          continue;
        }
        if (c == '\"') {
          if (ret.length() > 0) {
            pos--;
            return ret;
          }
          quote = true;
          continue;
        }
        if (c >= 'a' && c <= 'z') {
          ret += c;
        } else if (c >= 'A' && c <= 'Z') {
          ret += c;
        } else if (c >= '0' && c <= '9') {
          ret += c;
        } else if (c == '_') {
          ret += c;
        } else if (c == ' ' || c == '\r' || c == '\n') {
          if (ret.length() > 0) {
            return ret;
          }
        } else {
          //symbol
          if (ret.length() > 0) {
            pos--;
            return ret;
          }
          ret += c;
          return ret;
        }
      }
    } while (true);
  };

  private int px[];
  private int clrs[];

  public int[] load(InputStream is, Dimension d) {
    try {
      data = JF.readAll(is);
      pos = 0;
      //static char * name[] = {
      if (!getToken().equals("static")) throw new Exception("static");
      if (!getToken().equals("char")) throw new Exception("char");
      if (!getToken().equals("*")) throw new Exception("*");
      getToken();  //name  - ignore
      if (!getToken().equals("[")) throw new Exception("[");
      if (!getToken().equals("]")) throw new Exception("]");
      if (!getToken().equals("=")) throw new Exception("=");
      if (!getToken().equals("{")) throw new Exception("{");
      String values = getToken();
      String f[] = values.split(" ");
      int cols = Integer.valueOf(f[0]);
      int rows = Integer.valueOf(f[1]);
      int nclrs = Integer.valueOf(f[2]);
      int nchars = Integer.valueOf(f[3]);
      if (nchars < 1 || nchars > 2) {
        throw new Exception("nchars");
      }
      //hotspot = f[4] f[5] if defined
      px = new int[rows * cols];
      switch (nchars) {
        case 1: clrs = new int[256]; break;
        case 2: clrs = new int[65536]; break;
      }
      if (!getToken().equals(",")) throw new Exception(",");
      for(int a=0;a<nclrs;a++) {
        String clr = getToken();
        String code = clr.substring(0, nchars);
        String c = clr.substring(nchars+1, nchars+2);
        String value = clr.substring(nchars+3).toLowerCase();  //#123456 None grey100
        int idx = 0;
        switch (nchars) {
          case 1: idx = code.charAt(0); break;
          case 2: idx = (code.charAt(0) << 8) + code.charAt(1); break;
        }
        int rgb = 0;
        switch (c.charAt(0)) {
          case 'c': {
            if (value.equals("none")) {
              rgb = 0;
            } else if (value.startsWith("grey")) {
              int g = Integer.valueOf(value.substring(4));
              rgb = (g << 16) + (g << 8) + (g) + 0xff000000;
            } else if (value.charAt(0) == '#') {
              rgb = Integer.valueOf(value.substring(1), 16) + 0xff000000;
            }
            clrs[idx] = rgb;
          }
        }
        if (!getToken().equals(",")) throw new Exception(",");
      }
      int pxpos = 0;
      for(int y=0;y<rows;y++) {
        String col = getToken();
        int p = 0;
        int idx = 0;
        for(int x=0;x<cols;x++) {
          switch (nchars) {
            case 1: idx = col.charAt(p++); break;
            case 2: idx = (col.charAt(p++) << 8) + col.charAt(p++); break;
          }
          px[pxpos++] = clrs[idx];
        }
        if (y < rows-1) {
          if (!getToken().equals(",")) throw new Exception(",");
        }
      }
      if (!getToken().equals("}")) throw new Exception("}");
      if (!getToken().equals(";")) throw new Exception(";");
      d.width = cols;
      d.height = rows;
      return px;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
}
