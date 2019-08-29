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
  /** Type of host (S7, AB, MB, NI, MIC) See ControllerType */
  public int type;
  /** Tag name. */
  public String tag;
  /** Size of tag. See TagType */
  public int size;
  /** Color of tag (for reporting) */
  public int color;
  /** int min/max values (for reporting) */
  public int min, max;
  /** float min/max values (for reporting) */
  public float fmin, fmax;
  /** Speed to poll data (delay = ms delay between polls) (min = 25ms) */
  public int delay;

  private byte pending[];
  private Object pendingLock = new Object();

  /** Get user data. */
  public Object getData(String key) {
    return user.get(key);
  }
  /** Set user data. */
  public void setData(String key, Object value) {
    user.put(key, value);
  }
  /** Set host,type,tag,size,delay(ms). */
  public void setTag(String host, int type, String tag, int size, int delay) {
    this.host = host;
    this.type = type;
    this.tag = tag;
    this.size = size;
    this.delay = delay;
  }

  public boolean isValid() {
    if (size == TagType.unknown) return false;
    if (type == ControllerType.UNKNOWN) return false;
    if (host == null || host.length() == 0) return false;
    return true;
  }

  private Controller c;
  private String socks;
  private Timer timer;
  private Reader reader;
  private TagListener listener;
  private HashMap<String, Object> user = new HashMap<String, Object>();
  private Tag parent;
  private int childIdx;
  private Object lock = new Object();
  private ArrayList<Tag> children = new ArrayList<Tag>();
  private byte[][] childData;
  private ArrayList<Tag> queue = new ArrayList<Tag>();
  private boolean multiRead = true;

  /** Returns true if data type is float32 or float64 */
  public boolean isFloat() {
    return size == TagType.float32 || size == TagType.float64;
  }

  /** Returns true is controller is Big Endian byte order. */
  public boolean isBE() {
    switch (type) {
      case ControllerType.JF: return false;
      case ControllerType.S7: return true;
      case ControllerType.AB: return false;
      case ControllerType.MB: return true;
      case ControllerType.NI: return true;
      case ControllerType.MIC: return false;
      default: return true;
    }
  }

  /** Returns true is controller is Little Endian byte order. */
  public boolean isLE() {
    return !isBE();
  }

  /** Enables reading multiple tags in one request (currently only S7 supported) */
  public void setMultiRead(boolean state) {
    if (type != ControllerType.S7) return;
    if (true) return;  //TODO!!!
    multiRead = state;
  }

  /** Adds a child tag and returns index. */
  public int addChild(Tag child) {
    children.add(child);
    return children.size() - 1;
  }

  private void queue(Tag tag) {
    synchronized(queue) {
      queue.add(tag);
    }
  }

  /** Returns # of bytes tag uses. */
  public int getSize() {
    switch (size) {
      case TagType.bit: return 1;
      case TagType.int8: return 1;
      case TagType.int16: return 2;
      case TagType.int32: return 4;
      case TagType.int64: return 8;
      case TagType.uint8: return 1;
      case TagType.uint16: return 2;
      case TagType.uint32: return 4;
      case TagType.uint64: return 8;
      case TagType.float32: return 4;
      case TagType.float64: return 8;
    }
    return 0;
  }

  public String getURL() {
    switch (type) {
      case ControllerType.JF: return "JF:" + host;
      case ControllerType.S7: return "S7:" + host;
      case ControllerType.AB: return "AB:" + host;
      case ControllerType.MB: return "MB:" + host;
      case ControllerType.NI: return "NI:" + host;
      case ControllerType.MIC: return "MIC:" + host;
    }
    JFLog.log("Tag:Error:type unknown:" + type);
    return null;
  }

  public Controller getController() {
    if (parent != null) {
      return parent.c;
    } else {
      return c;
    }
  }

  public void setListener(TagListener listener) {
    this.listener = listener;
  }

  public String toString() {
    if (!isValid()) return "not set";
    if (type == ControllerType.NI) {
      return host;
    }
    if (type == ControllerType.MIC) {
      return "MIC:" + host;
    }
    return tag;
  }

  public String getmin() {
    if (isFloat()) {
      return Float.toString(fmin);
    } else {
      return Integer.toString(min);
    }
  }

  public String getmax() {
    if (isFloat()) {
      return Float.toString(fmax);
    } else {
      return Integer.toString(max);
    }
  }

  private boolean startTimer() {
    if (parent == null) {
      childData = null;
      c = new Controller();
      if (socks != null) {
        c.setSOCKS(socks);
      }
    } else {
      c = null;
    }
    timer = new Timer();
    reader = new Reader();
    reader.tag = this;
    if (delay < 25) delay = 25;
    timer.scheduleAtFixedRate(reader, delay, delay);
    return true;
  }

  /** Start reading tag at interval (delay). */
  public boolean start() {
    parent = null;
    return startTimer();
  }

  /** Start reading tag at interval (delay) using another Tags connection. */
  public boolean start(Tag parent) {
    this.parent = parent;
    if (parent != null) {
      if (parent.type != type) return false;
      childIdx = parent.addChild(this);
    }
    return startTimer();
  }

  /** Stop monitoring tag value. */
  public void stop() {
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    if (reader != null) {
      reader = null;
    }
    disconnect();
  }

  public void setSOCKS(String socksHost) {
    socks = socksHost;
  }

  public boolean connect() {
    if (parent != null) return false;  //wait for parent to connect
    if (c.connect(getURL())) return true;
    return false;
  }

  public void disconnect() {
    if (parent != null) {
      parent = null;
      return;
    }
    if (c != null) {
      c.disconnect();
      c = null;
    }
    children.clear();
  }

  private String value = "0";

  /** Returns current value (only valid if start() has been called). */
  public String getValue() {
    return value;
  }

  /** Queues pending data to be written on next cycle. (only valid if start() has been called). */
  public void setValue(String value) {
    byte data[] = null;
    if (isBE()) {
      switch (size) {
        case TagType.bit: data = new byte[] {(byte)(value.equals("0") ? 0 : 1)}; break;
        case TagType.int8: data = new byte[] {Byte.valueOf(value)}; break;
        case TagType.int16: data = new byte[2]; BE.setuint16(data, 0, Short.valueOf(value)); break;
        case TagType.int32: data = new byte[4]; BE.setuint32(data, 0, Integer.valueOf(value)); break;
        case TagType.int64: data = new byte[8]; BE.setuint64(data, 0, Long.valueOf(value)); break;
        case TagType.uint8: data = new byte[] {Byte.valueOf(value)}; break;
        case TagType.uint16: data = new byte[2]; BE.setuint16(data, 0, Short.valueOf(value)); break;
        case TagType.uint32: data = new byte[4]; BE.setuint32(data, 0, Integer.valueOf(value)); break;
        case TagType.uint64: data = new byte[8]; BE.setuint64(data, 0, Long.valueOf(value)); break;
        case TagType.float32: data = new byte[4]; BE.setuint32(data, 0, Float.floatToIntBits(Float.valueOf(value))); break;
        case TagType.float64: data = new byte[4]; BE.setuint64(data, 0, Double.doubleToLongBits(Double.valueOf(value))); break;
      }
    } else {
      switch (size) {
        case TagType.bit: data = new byte[] {(byte)(value.equals("0") ? 0 : 1)}; break;
        case TagType.int8: data = new byte[] {Byte.valueOf(value)}; break;
        case TagType.int16: data = new byte[2]; LE.setuint16(data, 0, Short.valueOf(value)); break;
        case TagType.int32: data = new byte[4]; LE.setuint32(data, 0, Integer.valueOf(value)); break;
        case TagType.int64: data = new byte[8]; LE.setuint64(data, 0, Long.valueOf(value)); break;
        case TagType.uint8: data = new byte[] {Byte.valueOf(value)}; break;
        case TagType.uint16: data = new byte[2]; LE.setuint16(data, 0, Short.valueOf(value)); break;
        case TagType.uint32: data = new byte[4]; LE.setuint32(data, 0, Integer.valueOf(value)); break;
        case TagType.uint64: data = new byte[8]; LE.setuint64(data, 0, Long.valueOf(value)); break;
        case TagType.float32: data = new byte[4]; LE.setuint32(data, 0, Float.floatToIntBits(Float.valueOf(value))); break;
        case TagType.float64: data = new byte[4]; LE.setuint64(data, 0, Double.doubleToLongBits(Double.valueOf(value))); break;
      }
    }
    synchronized(pendingLock) {
      pending = data;
    }
  }

  /** Returns current value as int (only valid if start() has been called). */
  public int intValue() {
    return Integer.valueOf(value);
  }

  /** Returns current value as float (only valid if start() has been called). */
  public float floatValue() {
    return Float.valueOf(value);
  }

  /** Returns current value as double (float64) (only valid if start() has been called). */
  public double doubleValue() {
    return Double.valueOf(value);
  }

  /** Reads value directly. */
  public byte[] read() {
    if (parent != null) {
      if (parent.c == null) return null;
      if (multiRead) {
        return parent.read(childIdx);
      } else {
        //queue read with parent to prevent some threads from starving
        synchronized(lock) {
          parent.queue(this);
          try {lock.wait();} catch (Exception e) {}
          return parent.c.read(tag);
        }
      }
    } else {
      if (multiRead && type == ControllerType.S7 && children.size() > 0) {
        int cnt = children.size();
        String tags[] = new String[cnt+1];
        tags[cnt] = tag;
        for(int a=0;a<cnt;a++) {
          tags[a] = children.get(a).tag;
        }
        childData = c.read(tags);
        if (childData == null) return null;
        return childData[cnt];
      } else {
        //allow queued children to proceed
        synchronized(queue) {
          while (queue.size() > 0) {
            Tag child = queue.remove(0);
            synchronized(child.lock) {
              child.lock.notify();
            }
          }
        }
        return c.read(tag);
      }
    }
  }

  /** Writes data to tag. */
  public void write(byte data[]) {
    if (parent != null) {
      if (parent.c == null) return;
      parent.c.write(tag, data);
    } else {
      c.write(tag, data);
    }
  }

  private byte[] read(int idx) {
    if (childData == null || idx >= childData.length) return null;
    return childData[idx];
  }

  private static class Reader extends TimerTask {
    public Tag tag;
    public byte data[];
    public void run() {
      try {
        String lastValue = tag.value;
        if (tag.parent == null) {
          if (!tag.c.isConnected()) {
            if (!tag.connect()) {
              return;
            }
          }
        }
        data = tag.read();
        if (data == null) {
          System.out.println("Error:" + System.currentTimeMillis() + ":data==null:host=" + tag.host + ":tag=" + tag.tag);
          return;
        }
        if (tag.isBE()) {
          switch (tag.size) {
            case TagType.bit: tag.value = data[0] == 0 ? "0" : "1"; break;
            case TagType.int8: tag.value = Byte.toString(data[0]); break;
            case TagType.int16: tag.value = Short.toString((short)BE.getuint16(data, 0)); break;
            case TagType.int32: tag.value = Integer.toString(BE.getuint32(data, 0)); break;
            case TagType.int64: tag.value = Long.toString(BE.getuint64(data, 0)); break;
            case TagType.uint8: tag.value = Integer.toUnsignedString(data[0] & 0xff); break;
            case TagType.uint16: tag.value = Integer.toUnsignedString(BE.getuint16(data, 0) & 0xffff); break;
            case TagType.uint32: tag.value = Integer.toUnsignedString(BE.getuint32(data, 0)); break;
            case TagType.uint64: tag.value = Long.toUnsignedString(BE.getuint64(data, 0)); break;
            case TagType.float32: tag.value = Float.toString(Float.intBitsToFloat(BE.getuint32(data, 0))); break;
            case TagType.float64: tag.value = Double.toString(Double.longBitsToDouble(BE.getuint64(data, 0))); break;
          }
        } else {
          switch (tag.size) {
            case TagType.bit: tag.value = data[0] == 0 ? "0" : "1"; break;
            case TagType.int8: tag.value = Byte.toString(data[0]); break;
            case TagType.int16: tag.value = Short.toString((short)LE.getuint16(data, 0)); break;
            case TagType.int32: tag.value = Integer.toString(LE.getuint32(data, 0)); break;
            case TagType.int64: tag.value = Long.toString(LE.getuint64(data, 0)); break;
            case TagType.uint8: tag.value = Integer.toUnsignedString(data[0] & 0xff); break;
            case TagType.uint16: tag.value = Integer.toUnsignedString(LE.getuint16(data, 0) & 0xffff); break;
            case TagType.uint32: tag.value = Integer.toUnsignedString(LE.getuint32(data, 0)); break;
            case TagType.uint64: tag.value = Long.toUnsignedString(LE.getuint64(data, 0)); break;
            case TagType.float32: tag.value = Float.toString(Float.intBitsToFloat(LE.getuint32(data, 0))); break;
            case TagType.float64: tag.value = Double.toString(Double.longBitsToDouble(LE.getuint64(data, 0))); break;
          }
        }
        synchronized(tag.pendingLock) {
          if (tag.pending != null) {
            tag.write(tag.pending);
            tag.pending = null;
          }
        }
        if (tag.listener == null) return;
        if (lastValue == null || !tag.value.equals(lastValue)) {
          tag.listener.tagChanged(tag, tag.value);
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
}
