/**
 *
 * @author pquiring
 */

import java.util.*;
import javaforce.LE;

import javaforce.controls.*;

public class Tag {
  public static enum types {
    S7, AB, MB, NI
  }

  public static enum sizes {
    bit, int8, int16, int32, float32, float64
  }

  public boolean isFloat() {
    return size == sizes.float32 || size == sizes.float64;
  }

  public int id;
  public String host;
  public types type;
  public String tag;
  public sizes size;
  public int color;
  public int min, max;
  public int delay;

  public float fmin, fmax;

  public int scaledValue;
  public int lastScaledValue;

  public Controller c;
  public Timer timer;
  public Reader reader;

  public String getURL() {
    switch (type) {
      case S7: return "S7:" + host;
      case AB: return "AB:" + host;
      case MB: return "MODBUS:" + host;
      case NI: return "NI:" + host;
    }
    return null;
  }

  public String toString() {
    if (type == types.NI) {
      return host;
    }
    return tag;
  }

  public String save() {
    if (size == sizes.float32 || size == sizes.float64)
      return host + "|" + type + "|" + tag + "|" + size + "|" + fmin + "|" + fmax + "|" + Integer.toUnsignedString(color, 16) + "|" + delay;
    else
      return host + "|" + type + "|" + tag + "|" + size + "|" + min + "|" + max + "|" + Integer.toUnsignedString(color, 16) + "|" + delay;
  }

  public void load(String data) {
    String f[] = data.split("[|]");
    host = f[0];
    switch (f[1]) {
      case "S7": type = types.S7; break;
      case "AB": type = types.AB; break;
      case "MB": type = types.MB; break;
      case "NI": type = types.NI; break;
    }
    tag = f[2];
    switch (f[3]) {
      case "bit": size = sizes.bit; break;
      case "int8": size = sizes.int8; break;
      case "int16": size = sizes.int16; break;
      case "int32": size = sizes.int32; break;
      case "float32": size = sizes.float32; break;
      case "float64": size = sizes.float64; break;
    }
    if (size == sizes.float32 || size == sizes.float64) {
      fmin = Float.valueOf(f[4]);
      fmax = Float.valueOf(f[5]);
    } else {
      min = Integer.valueOf(f[4]);
      max = Integer.valueOf(f[5]);
    }
    color = Integer.valueOf(f[6], 16);
    delay = Integer.valueOf(f[7]);
  }

  public String getmin() {
    if (size == sizes.float32 || size == sizes.float64) {
      return Float.toString(fmin);
    } else {
      return Integer.toString(min);
    }
  }

  public String getmax() {
    if (size == sizes.float32 || size == sizes.float64) {
      return Float.toString(fmax);
    } else {
      return Integer.toString(max);
    }
  }

  public void start() {
    if (!connect()) return;
    timer = new Timer();
    reader = new Reader();
    reader.tag = this;
    timer.scheduleAtFixedRate(reader, delay, delay);
  }

  public void stop() {
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    if (reader != null) {
      reader = null;
    }
  }

  public boolean connect() {
    c = new Controller();
    if (c.connect(getURL())) return true;
    Service.logMsg("Unable to connect tag:" + getURL());
    return false;
  }

  public static class Reader extends TimerTask {
    public Tag tag;
    public byte last[];
    public byte data[];
    public void run() {
      if (!tag.c.isConnected()) {
        if (!tag.connect()) {
          tag.stop();
          return;
        }
      }
      data = tag.c.read(tag.tag);
      if (data != null && last != null) {
        //compare data/last
        if (data.length == last.length) {
          boolean diff = false;
          for(int a=0;a<data.length;a++) {
            if (data[a] != last[a]) {
              diff = true;
              break;
            }
          }
          if (diff) {
            //change detected, log it
            String value = null;
            switch (tag.size) {
              case bit: value = data[0] == 0 ? "false" : "true"; break;
              case int8: value = Byte.toString(data[0]); break;
              case int16: value = Short.toString((short)LE.getuint16(data, 0)); break;
              case int32: value = Integer.toString(LE.getuint32(data, 0)); break;
              case float32: value = Float.toString(Float.intBitsToFloat(LE.getuint32(data, 0))); break;
              case float64: value = Double.toString(Double.longBitsToDouble(LE.getuint64(data, 0))); break;
            }
            Service.logChange(tag, value);
          }
        }
      }
      last = data;
    }
  }
}
