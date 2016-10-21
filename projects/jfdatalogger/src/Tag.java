/**
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.controls.*;

public class Tag {
  public static enum types {
    S7, AB, MB, NI
  }

  public static enum sizes {
    bit, int8, int16, int32, float32, float64
  }

  public String host;
  public types type;
  public String tag;
  public sizes size;
  public int color;
  public int min, max;

  public float fmin, fmax;

  public int scaledValue;
  public int lastScaledValue;

  public Controller c;
  public ArrayList<Tag> children = new ArrayList<Tag>();
  public String tags[];
  public boolean child;
  public byte data[];  //data from parent

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
      return host + "|" + type + "|" + tag + "|" + size + "|" + fmin + "|" + fmax + "|" + Integer.toUnsignedString(color, 16);
    else
      return host + "|" + type + "|" + tag + "|" + size + "|" + min + "|" + max + "|" + Integer.toUnsignedString(color, 16);
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
    color = Long.valueOf(f[6], 16).intValue();
  }
}
