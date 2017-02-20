package javaforce.controls;

/** Monitors a PLC Tag.
 *
 * Auto-reconnects when disconnects.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class Tag {
  /** Host (usually IP Address) */
  public String host;
  /** Type of host (S7, AB, MB, NI) */
  public Controller.types type;
  /** Tag name. */
  public String tag;
  /** Size of tag. */
  public Controller.sizes size;
  /** Color of tag (for reporting) */
  public int color;
  /** int min/max values (for reporting) */
  public int min, max;
  /** float min/max values (for reporting) */
  public float fmin, fmax;
  /** Speed to poll data (delay = ms delay between polls) (min = 25ms) */
  public int delay;
  /** Get user data. */
  public Object getData(String key) {
    return user.get(key);
  }
  /** Set user data. */
  public void setData(String key, Object value) {
    user.put(key, value);
  }

  private Controller c;
  private Timer timer;
  private Reader reader;
  private TagListener listener;
  private HashMap<String, Object> user = new HashMap<String, Object>();

  /** Returns true if data type is float32 or float64 */
  public boolean isFloat() {
    return size == Controller.sizes.float32 || size == Controller.sizes.float64;
  }

  /** Returns # of bytes tag uses. */
  public int getSize() {
    switch (size) {
      case bit: return 1;
      case int8: return 1;
      case int16: return 2;
      case int32: return 4;
      case float32: return 4;
      case float64: return 8;
    }
    return 0;
  }

  public String getURL() {
    switch (type) {
      case S7: return "S7:" + host;
      case AB: return "AB:" + host;
      case MB: return "MB:" + host;
      case NI: return "NI:" + host;
    }
    return null;
  }

  public void setListener(TagListener listener) {
    this.listener = listener;
  }

  public String toString() {
    if (type == Controller.types.NI) {
      return host;
    }
    return tag;
  }

  public String getmin() {
    if (size == Controller.sizes.float32 || size == Controller.sizes.float64) {
      return Float.toString(fmin);
    } else {
      return Integer.toString(min);
    }
  }

  public String getmax() {
    if (size == Controller.sizes.float32 || size == Controller.sizes.float64) {
      return Float.toString(fmax);
    } else {
      return Integer.toString(max);
    }
  }

  public void start() {
    timer = new Timer();
    reader = new Reader();
    reader.tag = this;
    if (delay < 25) delay = 25;
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
    return false;
  }

  private String value;

  /** Returns current value (only valid if start() has been called). */
  public String getValue() {
    return value;
  }

  /** Reads value directly (do NOT use if start() has been called). */
  public byte[] read() {
    return c.read(tag);
  }

  /** Writes data to tag. */
  public void write(byte data[]) {
    c.write(tag, data);
  }

  private static class Reader extends TimerTask {
    public Tag tag;
    public byte data[];
    public void run() {
      if (tag.c == null) {
        if (!tag.connect()) return;
      }
      String lastValue = tag.value;
      if (!tag.c.isConnected()) {
        if (!tag.connect()) {
          return;
        }
      }
      data = tag.c.read(tag.tag);
      if (data == null) {
        tag.value = "error";
      } else {
        switch (tag.size) {
          case bit: tag.value = data[0] == 0 ? "0" : "1"; break;
          case int8: tag.value = Byte.toString(data[0]); break;
          case int16: tag.value = Short.toString((short)BE.getuint16(data, 0)); break;
          case int32: tag.value = Integer.toString(BE.getuint32(data, 0)); break;
          case float32: tag.value = Float.toString(Float.intBitsToFloat(BE.getuint32(data, 0))); break;
          case float64: tag.value = Double.toString(Double.longBitsToDouble(BE.getuint64(data, 0))); break;
        }
      }
      if (tag.listener == null) return;
      if (lastValue == null || !tag.value.equals(lastValue)) {
        tag.listener.tagChanged(tag);
      }
    }
  }
}
