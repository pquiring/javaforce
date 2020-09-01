package jfcontrols.tags;

/**
 *
 * @author User
 */

import java.util.*;

import javaforce.*;
import javaforce.db.*;
import javaforce.io.*;
import javaforce.controls.*;

public abstract class TagBase extends Row {

  public int type;
  public int cid, tid;
  public String name;
  public String comment;
  public boolean unsigned;
  public boolean isArray;

  public boolean dirty;  //value changed
  public boolean nosave;
  public Tag remoteTag;  //if cid > 0
  public TagBase parent;  //if field of UDT (else points to this)

  private ArrayList<TagBaseListener> listeners;
  private Object lock;

  public static interface Creator {
    public TagBase create();
  }

  public void setDirty() {
    if (nosave) return;
    parent.dirty = true;
    if (cid > 0) {
      JFLog.log("SetDirty:" + name);
    }
  }

  public final boolean isArray() {return isArray;}
  public final boolean isUnsigned() {return unsigned;}
  public int getType() {
    return type;
  }
  public int getSize() {
    return getSize(type);
  }

  public static int getSize(int type) {
    switch (type) {
      case TagType.bit:
      case TagType.int8:
        return 1;
      case TagType.int16:
        return 2;
      case TagType.int32:
        return 4;
      case TagType.int64:
        return 8;
      case TagType.float32:
        return 4;
      case TagType.float64:
        return 8;
      case TagType.char8:
        return 1;
      case TagType.char16:
        return 2;
    }
    return -1;
  }
  public String getString8() {
    return toString(0);
  }
  public String getString8(int idx) {
    if (!isArray) return null;
    switch (type) {
      case TagType.string: return ((TagString)this).getString8();
      case TagType.char8: return ((TagChar8)this).getString8();
      case TagType.char16: return ((TagChar16)this).getString16();
    }
    return null;
  }
  public String getString16(int idx) {
    if (type != TagType.char16) return null;
    if (!isArray) return null;
    return ((TagChar16)this).getString16();
  }
  public void setString8(int idx, String value) {
    //see TagString.setString8() only
  }
  public static void encode(int type, boolean unsigned, String value, byte data[], int pos) {
    switch (type) {
      case TagType.bit:
      case TagType.int8:
        LE.setuint8(data, pos, Integer.valueOf(value));
        break;
      case TagType.int16:
        LE.setuint16(data, pos, Integer.valueOf(value));
        break;
      case TagType.int32:
        LE.setuint32(data, pos, Integer.valueOf(value));
        break;
      case TagType.float32:
        LE.setuint32(data, pos, Float.floatToIntBits(Float.valueOf(value)));
        break;
      case TagType.float64:
        LE.setuint64(data, pos, Double.doubleToLongBits(Double.valueOf(value)));
        break;
      case TagType.char8:
        LE.setuint8(data, pos, value.charAt(0));
        break;
      case TagType.char16:
        LE.setuint16(data, pos, value.charAt(0));
        break;
    }
  }

  public static String decode(int type, boolean unsigned, byte data[], int pos) {
    switch (type) {
      case TagType.bit:
      case TagType.int8:
        if (unsigned)
          return Integer.toUnsignedString(LE.getuint8(data, pos));
        else
          return Integer.toString(LE.getuint8(data, pos));
      case TagType.int16:
        if (unsigned)
          return Integer.toUnsignedString(LE.getuint16(data, pos));
        else
          return Integer.toString(LE.getuint16(data, pos));
      case TagType.int32:
        if (unsigned)
          return Integer.toUnsignedString(LE.getuint32(data, pos));
        else
          return Integer.toString(LE.getuint32(data, pos));
      case TagType.float32:
        return Float.toString(Float.intBitsToFloat(LE.getuint32(data, pos)));
      case TagType.float64:
        return Double.toString(Double.longBitsToDouble(LE.getuint64(data, pos)));
      case TagType.char8:
        return "" + (char)LE.getuint8(data, pos);
      case TagType.char16:
        return "" + (char)LE.getuint16(data, pos);
    }
    return null;
  }

  public String getValue() {
    return toString();
  }

  public String getValue(int idx) {
    return toString(idx);
  }

  public void setValue(String newValue) {
    if (cid != 0) {
      JFLog.log("Tag:" + name + ":setValue:" + newValue);
    }
    tagChanged(null, newValue);
    switch (type) {
      case TagType.bit: setBoolean(0, newValue.equals("1"));
      case TagType.int8:
      case TagType.int16:
      case TagType.int32: setInt(0, Integer.valueOf(newValue)); break;
      case TagType.int64: setLong(0, Long.valueOf(newValue)); break;
      case TagType.float32: setInt(0, Float.floatToIntBits(Float.valueOf(newValue)));
      case TagType.float64: setLong(0, Double.doubleToLongBits(Double.valueOf(newValue)));
      case TagType.string: setString8(0, newValue);
    }
    setDirty();
  }

  public void updateValue(String newValue) {
    String oldValue = getValue();
    if (newValue.equals(oldValue)) return;
    switch (type) {
      case TagType.bit: setBoolean(0, newValue.equals("1"));
      case TagType.int8:
      case TagType.int16:
      case TagType.int32: setInt(0, Integer.valueOf(newValue)); break;
      case TagType.int64: setLong(0, Long.valueOf(newValue)); break;
      case TagType.float32: setInt(0, Float.floatToIntBits(Float.valueOf(newValue)));
      case TagType.float64: setLong(0, Double.doubleToLongBits(Double.valueOf(newValue)));
      case TagType.string: setString8(0, newValue);
    }
  }

  public void setValue(String newValue, int idx) {
    switch (type) {
      case TagType.bit: setBoolean(idx, newValue.equals("1"));
      case TagType.int8:
      case TagType.int16:
      case TagType.int32: setInt(idx, Integer.valueOf(newValue)); break;
      case TagType.int64: setLong(idx, Long.valueOf(newValue)); break;
      case TagType.float32: setInt(idx, Float.floatToIntBits(Float.valueOf(newValue)));
      case TagType.float64: setLong(idx, Double.doubleToLongBits(Double.valueOf(newValue)));
      case TagType.string: setString8(idx, newValue);
    }
    setDirty();
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    setDirty();
    this.comment = comment;
  }

  public void init(TagBase parent) {
    this.parent = parent;
    listeners = new ArrayList<>();
    lock = new Object();
    if (cid > 0) {
      RemoteControllers.addTag(this);
    }
    if (cid == 0 && name != null && name.equals("scantime") && parent != null && parent.name.equals("system")) {
      nosave = true;
    }
  }

  public void addListener(TagBaseListener listener) {
    synchronized(lock) {
      listeners.add(listener);
    }
  }

  public void removeListener(TagBaseListener listener) {
    synchronized(lock) {
      listeners.remove(listener);
    }
  }

  public void tagChanged(String oldValue, String newValue) {
    synchronized(lock) {
      int cnt = listeners.size();
      for(int a=0;a<cnt;a++) {
        listeners.get(a).tagChanged(this, oldValue, newValue);
      }
    }
  }

  public String toString() {return toString(0);}

  public abstract String toString(int idx);
  public abstract int getLength();
  public abstract boolean getBoolean(int idx);
  public boolean getBoolean() {
    return getBoolean(0);
  }
  public abstract void setBoolean(int idx, boolean value);
  public void setBoolean(boolean value) {
    setBoolean(0, value);
  }
  public abstract int getInt(int idx);
  public int getInt() {
    return getInt(0);
  }
  public abstract void setInt(int idx, int value);
  public void setInt(int value) {
    setInt(0, value);
  }
  public abstract long getLong(int idx);
  public long getLong() {
    return getLong(0);
  }
  public abstract void setLong(int idx, long value);
  public void setLong(long value) {
    setLong(0, value);
  }
  public abstract float getFloat(int idx);
  public float getFloat() {
    return getFloat(0);
  }
  public abstract void setFloat(int idx, float value);
  public void setFloat(float value) {
    setFloat(0, value);
  }
  public abstract double getDouble(int idx);
  public double getDouble() {
    return getDouble(0);
  }
  public abstract void setDouble(int idx, double value);
  public void setDouble(double value) {
    setDouble(0, value);
  }
  public TagBase[] getFields() {return null;}
  public TagBase[] getFields(int idx) {return null;}
  public TagBase getField(int idx, String name) {return null;}

  public void readObject() throws Exception {
    super.readObject();
    type = readInt();
    cid = readInt();
    tid = readInt();
    name = readString();
    comment = readString();
    unsigned = readBoolean();
    isArray = readBoolean();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(type);
    writeInt(cid);
    writeInt(tid);
    writeString(name);
    writeString(comment);
    writeBoolean(unsigned);
    writeBoolean(isArray);
  }
}
