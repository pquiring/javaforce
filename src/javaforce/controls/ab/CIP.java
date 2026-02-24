package javaforce.controls.ab;

import javaforce.JFLog;
import javaforce.LE;

/** CIP : Connection Manager : Request
 *
 * @author pquiring
 */

public class CIP {
  //CIP header
  public byte cmd = 0;
  public byte count = 2;
  public byte path_1 = 0x20;  //8bit class segment
  public byte class_1 = 0x06;  //connection manager
  public byte path_2 = 0x24;  //8bit instance segment
  public byte class_2 = 0x01;  //instance

  //reply data
  public byte reserved1;
  public byte reserved2;
  public byte reserved3;
  public byte type = 0;  //ABTypes
  public byte reserved;
  public byte[] data;  //tag data
  public byte[][] attrs;

  //CMD_TAGS {
  //connection manager header
  public byte ticktime = 0x07;
  public byte ticktimeout = (byte)0xf9;
  public short sub_cmd_len;  //size of following {} in bytes
  //{
    public byte sub_cmd;
    public byte data_words;  //size of segments in 16bit words (multiple segments)
    public TagSegment[] segments;
  //}
  public byte route_size = 0x01;  //size of following {} in 16bit words
  public byte route_res = 0x00;  //reserved
  //  {
    public byte route_seg = 0x01;
    public byte route_addr = 0x00;
  //  }
  //}

  //CMD_*_ATTRS {
  public short attr_count = 1;
  public short attr_clock = 6;  //clock (timestamp)
  public long clock;  //unix epoch (ms) * 1000 (=us)
  //}

  public static final byte CMD_UNCONNECTED_SEND = 0x52;

  public CIP(byte _cmd, byte _sub_cmd) {
    cmd = _cmd;
    sub_cmd = _sub_cmd;
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        break;
    }
  }

  private static abstract class TagSegment {
    public TagSegment(byte type) {
      this.type = type;
    }
    public byte type;
    //...
    public abstract int size();
    public abstract void writeSegment(byte[] data, int offset);
  }

  private static class TagName extends TagSegment {
    public TagName(String name) {
      super((byte)0x91);
      len = (byte)name.length();
      chars = name.getBytes();
    }
    public byte len = 0;
    public byte[] chars;
    public int size() {
      int len = 2 + chars.length;
      if (len % 2 != 0) len++;
      return len;
    }
    public void writeSegment(byte[] data, int offset) {
      data[offset++] = type;
      data[offset++] = len;
      for(int a=0;a<chars.length;a++) {
        data[offset++] = chars[a];
      }
    }
  }

  private static class TagElement8 extends TagSegment {
    public TagElement8(byte idx) {
      super((byte)0x28);
      this.idx = idx;
    }
    public byte idx;
    public int size() {
      return 2;
    }
    public void writeSegment(byte[] data, int offset) {
      data[offset++] = type;
      data[offset++] = idx;
    }
  }

  private static class TagElement16 extends TagSegment {
    public TagElement16(short idx) {
      super((byte)0x29);
      this.idx = idx;
    }
    public byte pad;
    public short idx;
    public int size() {
      return 4;
    }
    public void writeSegment(byte[] data, int offset) {
      data[offset++] = type;
      data[offset++] = pad;
      LE.setuint16(data, offset, idx);
    }
  }

  private static class TagElement32 extends TagSegment {
    public TagElement32(int idx) {
      super((byte)0x29);
      this.idx = idx;
    }
    public byte pad;
    public int idx;
    public int size() {
      return 6;
    }
    public void writeSegment(byte[] data, int offset) {
      data[offset++] = type;
      data[offset++] = pad;
      LE.setuint32(data, offset, idx);
    }
  }

  //CMD_TAGS : sub_cmd
  public static final byte SUB_CMD_READTAG = 0x4c;
  public static final byte SUB_CMD_WRITETAG = 0x4d;
  public static final byte SUB_CMD_GET_ATTR = 0x03;
  public static final byte SUB_CMD_SET_ATTR = 0x04;

  public int getSize() {
    if ((cmd & 0x80) == 0x80) {
      //reply
      switch (cmd & 0x7f) {
        case SUB_CMD_READTAG: return data.length + 6;
        case SUB_CMD_WRITETAG: return 4;
      }
      return 0;
    }
    int size = 12;
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        switch (sub_cmd) {
          case SUB_CMD_READTAG:
          case SUB_CMD_WRITETAG:
            if (segments != null) {
              for(int a=0;a<segments.length;a++) {
                size += segments[a].size();
              }
            }
            if (data != null) {
              size += data.length;
            }
            break;
          case SUB_CMD_GET_ATTR:
            size += 8;
            break;
          case SUB_CMD_SET_ATTR:
            size += 16;
            break;
        }
        size += 4;
        return size;
    }
    return 0;
  }

  public void write(byte[] packet, int offset) throws Exception {
    JFLog.log("offset=" + offset);
    packet[offset++] = cmd;
    packet[offset++] = count;
    packet[offset++] = path_1;
    packet[offset++] = class_1;
    packet[offset++] = path_2;
    packet[offset++] = class_2;
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        packet[offset++] = ticktime;
        packet[offset++] = ticktimeout;
        LE.setuint16(packet, offset, sub_cmd_len); offset += 2;
        packet[offset++] = sub_cmd;
        packet[offset++] = data_words;
        switch (sub_cmd) {
          case SUB_CMD_READTAG:
          case SUB_CMD_WRITETAG:
            if (segments != null) {
              for(int a=0;a<segments.length;a++) {
                segments[a].writeSegment(packet, offset);
                offset += segments[a].size();
              }
            }
            if (data != null) {
              System.arraycopy(data, 0, packet, offset, data.length); offset += data.length;
            }
            break;
          case SUB_CMD_GET_ATTR:
            LE.setuint16(packet, offset, attr_count); offset+= 2;
            LE.setuint16(packet, offset, attr_clock); offset+= 2;
            break;
          case SUB_CMD_SET_ATTR:
            LE.setuint16(packet, offset, attr_count); offset+= 2;
            LE.setuint16(packet, offset, attr_clock); offset+= 2;
            LE.setuint64(packet, offset, clock); offset+= 8;
            break;
        }
        packet[offset++] = route_size;
        packet[offset++] = route_res;
        packet[offset++] = route_seg;
        packet[offset++] = route_addr;
        break;
    }
  }

  private void decodeTag(String tag) {
    String[] segs = tag.split("[.]");
    int len = segs.length;
    for(int a=0;a<segs.length;a++) {
      String seg = segs[a];
      if (seg.endsWith("]")) {
        int i1 = seg.indexOf('[');
        int i2 = seg.indexOf(']');
        String[] idxes = seg.substring(i1+1, i2).split("[,]");
        len += idxes.length;
      }
    }
    segments = new TagSegment[len];
    int pos = 0;
    for(int a=0;a<segs.length;a++) {
      String seg = segs[a];
      if (seg.endsWith("]")) {
        int i1 = seg.indexOf('[');
        int i2 = seg.indexOf(']');
        String name = seg.substring(0, i1);
        segments[pos++] = new TagName(name);
        String[] idxes = seg.substring(i1+1, i2).split("[,]");
        for(int b=0;b<idxes.length;b++) {
          int idx = Integer.valueOf(idxes[b]);
          if (idx < 256) {
            segments[pos++] = new TagElement8((byte)idx);
          } else if (idx < 65536) {
            segments[pos++] = new TagElement16((short)idx);
          } else {
            segments[pos++] = new TagElement32(idx);
          }
        }
      } else {
        segments[pos++] = new TagName(seg);
      }
    }
  }

  public void setRead(String tag) {
    decodeTag(tag);
    data = new byte[] {0x01, 0x00};  //count
    setLengths();
  }

  public void setWrite(String tag, byte type, byte[] data) {
    decodeTag(tag);
    this.data = new byte[4 + data.length];
    this.data[0] = type;
    this.data[1] = 0;
    this.data[2] = 1;
    this.data[3] = 0;
    System.arraycopy(data, 0, this.data, 4, data.length);
    setLengths();
  }

  public void setReadClock() {
//    class_1 = (byte)0x8b;
    setLengths();
  }

  public void setWriteClock() {
//    class_1 = (byte)0x8b;
    setLengths();
  }

  private void setLengths() {
    int size = 0;
    switch (sub_cmd) {
      case SUB_CMD_READTAG:
      case SUB_CMD_WRITETAG:
        if (segments != null) {
          for(int a=0;a<segments.length;a++) {
            size += segments[a].size();
          }
        }
        data_words = (byte)((size) >> 1);
        if (data != null) {
          size += data.length;
        }
        break;
      case SUB_CMD_GET_ATTR:
        size = 4;
        data_words = 0;
        break;
      case SUB_CMD_SET_ATTR:
        size = 4 + 8;
        data_words = 0;
        break;
    }
    sub_cmd_len = (short)(2 + size);
  }

  public void readReplyReadTag(byte[] data, int offset) throws Exception {
    cmd = data[offset++];
    reserved1 = data[offset++];
    reserved2 = data[offset++];
    reserved3 = data[offset++];
    type = data[offset++];
    reserved = data[offset++];
    int size = 0;
    switch (type) {
      case ABTypes.INT:
        size = 2;
        break;
      case ABTypes.DINT:
      case ABTypes.REAL:
        size = 4;
        break;
      case ABTypes.BOOL:
        size = 1;
        break;
    }
    this.data = new byte[size];
    System.arraycopy(data, offset, this.data, 0, size);
  }

  public void readReplyWriteTag(byte[] data, int offset) throws Exception {
    cmd = data[offset++];
    reserved1 = data[offset++];
    reserved2 = data[offset++];
    reserved3 = data[offset++];
  }

  public void readReplyGetAttrs(byte[] data, int offset) throws Exception {
    cmd = data[offset++];
    count = data[offset++];
    path_1 = data[offset++];
    class_1 = data[offset++];
    path_2 = data[offset++];
    class_2 = data[offset++];
    short attr_count = (short)LE.getuint16(data, offset); offset += 2;
    attrs = new byte[attr_count][];
    for(int i=0;i<attr_count;i++) {
      attrs[i] = new byte[8];
      System.arraycopy(data, offset, attrs[i], 0, 8);
      offset += 8;
    }
  }

  public void readReplySetAttrs(byte[] data, int offset) throws Exception {
    cmd = data[offset++];
    count = data[offset++];
    short attr_count = (short)LE.getuint16(data, offset); offset += 2;
    attrs = new byte[attr_count][];
    for(int i=0;i<attr_count;i++) {
      attrs[i] = new byte[1];
      attrs[i][0] = data[offset++];  //0 = success
      offset++;  //padding ???
    }
  }
}
