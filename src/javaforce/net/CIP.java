package javaforce.net;

import javaforce.controls.ab.*;

/** CIP : Common Industrial Protocol
 *
 * See https://github.com/wireshark/wireshark/blob/master/epan/dissectors/packet-cip.h
 *
 * @author pquiring
 */

public class CIP implements SubPacket {
  //CIP header
  public byte cmd;  //0x80=reply 0x7f=cmd
  public byte count;
  public byte path_1;
  public byte class_1;
  public byte path_2;
  public byte class_2;

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
  public byte ticktime;
  public byte ticktimeout;
  public short sub_cmd_len;  //size of following {} in bytes
  //{
    public byte sub_cmd;
    public byte sub_count;  //size of segments in 16bit words (multiple segments)
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

  public static final byte DEVICE_PLC = 0x0e;

  public static final byte CMD_GET_ADDR_ALL = 0x01;
  public static final byte CMD_GET_ADDR_SINGLE = 0x06;
  public static final byte CMD_UNCONNECTED_SEND = 0x52;

  //sub_cmd (service code)
  public static final byte SUB_CMD_GET_ATTR_ALL = 0x01;
  public static final byte SUB_CMD_SET_ATTR_ALL = 0x02;
  public static final byte SUB_CMD_GET_ATTR_LIST = 0x03;
  public static final byte SUB_CMD_SET_ATTR_LIST = 0x04;
  public static final byte SUB_CMD_MULTI = 0x0a;
  public static final byte SUB_CMD_GET_ATTR_ONE = 0x0e;
  public static final byte SUB_CMD_SET_ATTR_ONE = 0x10;
  public static final byte SUB_CMD_READTAG = 0x4c;
  public static final byte SUB_CMD_WRITETAG = 0x4d;
  public static final byte SUB_CMD_READFRAG = 0x52;
  public static final byte SUB_CMD_WRITEFRAG = 0x53;
  public static final byte SUB_CMD_LIST_TAGS = 0x55;

  public static final byte PATH_SEGMENT_TYPE_MASK = (byte)0xe0;
  public static final byte PATH_PORT = 0x00;
  public static final byte PATH_LOGICAL = 0x20;
  public static final byte PATH_NETWORK = 0x40;
  public static final byte PATH_SYM = 0x60;
  public static final byte PATH_DATA = (byte)0x80;

  public static final byte PATH_LOGICAL_CLASS = 0x20;
  public static final byte PATH_LOGICAL_INSTANCE = 0x24;
  public static final byte PATH_LOGICAL_INSTANCE_16 = 0x25;
  public static final byte PATH_LOGICAL_MEMBER = 0x28;
  public static final byte PATH_LOGICAL_CONNPOINT = 0x2c;
  public static final byte PATH_LOGICAL_ATTRIBUTE = 0x30;

  public static final byte CLS_IDENTITY = 0x01;
  public static final byte CLS_ASSEMBLY = 0x04;
  public static final byte CLS_CONNECTION = 0x05;
  public static final byte CLS_CONNECTION_MANAGER = 0x06;
  public static final byte CLS_TAG_INFO = 0x6b;
  public static final byte CLS_WALLCLOCK = (byte)0x8b;

  public static final byte ATTR_TAG_NAME = 0x01;
  public static final byte ATTR_TAG_TYPE = 0x02;
  public static final byte ATTR_TAG_BASE_SIZE = 0x07;
  public static final byte ATTR_TAG_ARRAY_DIMS = 0x08;  //3x32bit

  public CIP() {
    init();
  }

  public CIP(byte _cmd, byte _sub_cmd) {
    init();
    cmd = _cmd;
    sub_cmd = _sub_cmd;
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        break;
    }
  }

  private void init() {
    count = 2;
    path_1 = PATH_LOGICAL_CLASS;
    class_1 = CLS_CONNECTION_MANAGER;
    path_2 = PATH_LOGICAL_INSTANCE;
    class_2 = 0x01;

    ticktime = 0x07;
    ticktimeout = (byte)0xf9;
  }

  private static abstract class TagSegment {
    public TagSegment(byte type) {
      this.type = type;
    }
    public byte type;
    //...
    public abstract int size();
    public abstract void writeSegment(Packet packet) throws Exception;
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
    public void writeSegment(Packet packet) throws Exception {
      packet.writeByte(type);
      packet.writeByte(len);
      for(int a=0;a<chars.length;a++) {
        packet.writeByte(chars[a]);
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
    public void writeSegment(Packet packet) throws Exception {
      packet.writeByte(type);
      packet.writeByte(idx);
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
    public void writeSegment(Packet packet) throws Exception {
      packet.writeByte(type);
      packet.writeByte(pad);
      packet.writeShort(idx);
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
    public void writeSegment(Packet packet) throws Exception {
      packet.writeByte(type);
      packet.writeByte(pad);
      packet.writeInt(idx);
    }
  }

  public int getSize() {
    if ((cmd & 0x80) == 0x80) {
      //reply
      switch (cmd & 0x7f) {
        case SUB_CMD_READTAG: return data.length + 6;
        case SUB_CMD_WRITETAG: return 4;
      }
      return 0;
    }
    int size = 6;  //header
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        size += 6;  //header
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
          case SUB_CMD_GET_ATTR_ALL:
          case SUB_CMD_GET_ATTR_ONE:
            size += 6;
            break;
          case SUB_CMD_SET_ATTR_ALL:
          case SUB_CMD_SET_ATTR_ONE:
            size += 12;
            break;
        }
        size += 4;  //route path
        return size;
    }
    return 0;
  }

  public int getDataSize() {
    return -1;
  }

  public void write(Packet packet) throws Exception {
    packet.writeByte(cmd);
    packet.writeByte(count);
    packet.writeByte(path_1);
    packet.writeByte(class_1);
    packet.writeByte(path_2);
    packet.writeByte(class_2);
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        packet.writeByte(ticktime);
        packet.writeByte(ticktimeout);
        packet.writeShort(sub_cmd_len);
        //embedded CIP packet follows
        packet.writeByte(sub_cmd);
        packet.writeByte(sub_count);
        switch (sub_cmd) {
          case SUB_CMD_READTAG:
          case SUB_CMD_WRITETAG:
            if (segments != null) {
              for(int a=0;a<segments.length;a++) {
                segments[a].writeSegment(packet);
              }
            }
            if (data != null) {
              packet.write(data);
            }
            break;
          case SUB_CMD_GET_ATTR_ALL:
          case SUB_CMD_GET_ATTR_ONE:
          case SUB_CMD_GET_ATTR_LIST:
            packet.writeByte(PATH_LOGICAL_CLASS);  //path_1
            packet.writeByte(CLS_WALLCLOCK);  //class_1
            packet.writeByte(PATH_LOGICAL_INSTANCE_16);  //path_2
            packet.writeByte((byte)0x00);  //padding
            packet.writeShort((short)0x00);  //instance
//            packet.writeByte((byte)0x01);  //class_2
//            packet.writeByte((byte)0x01);  //class_2
//            packet.writeByte(PATH_ATTRIBUTE);  //path_3
//            packet.writeByte((byte)0x01);  //All Attributes
//            packet.writeShort(attr_count);
//            packet.writeShort(attr_clock);
//            packet.writeByte((byte)0x01);  //ALL attributes
            break;
          case SUB_CMD_SET_ATTR_ALL:
          case SUB_CMD_SET_ATTR_ONE:
          case SUB_CMD_SET_ATTR_LIST:
            packet.writeShort(attr_count);
            packet.writeShort(attr_clock);
            packet.writeLong(clock);
            break;
        }
        packet.writeByte(route_size);
        packet.writeByte(route_res);
        packet.writeByte(route_seg);
        packet.writeByte(route_addr);
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
    set_sub_cmd_len();
  }

  public void setWrite(String tag, byte type, byte[] data) {
    decodeTag(tag);
    this.data = new byte[4 + data.length];
    this.data[0] = type;
    this.data[1] = 0;
    this.data[2] = 1;
    this.data[3] = 0;
    System.arraycopy(data, 0, this.data, 4, data.length);
    set_sub_cmd_len();
  }

  public void setReadClock() {
    set_sub_cmd_len();
  }

  public void setWriteClock() {
    set_sub_cmd_len();
  }

  private void set_sub_cmd_len() {
    short size = 2;  //sub_cmd, sub_count
    switch (sub_cmd) {
      case SUB_CMD_READTAG:
      case SUB_CMD_WRITETAG:
        int sub_size = 0;
        if (segments != null) {
          for(int a=0;a<segments.length;a++) {
            sub_size += segments[a].size();
          }
        }
        sub_count = (byte)((sub_size) >> 1);
        size += sub_size;
        if (data != null) {
          size += data.length;
        }
        break;
      case SUB_CMD_GET_ATTR_ALL:
        size += 6;
        sub_count = 3;
        break;
      case SUB_CMD_GET_ATTR_ONE:
        size += 6;
        sub_count = 3;
        break;
      case SUB_CMD_SET_ATTR_ALL:
        size += 12;
        sub_count = 2;
        break;
      case SUB_CMD_SET_ATTR_ONE:
        size += 12;
        sub_count = 3;
        break;
    }
    sub_cmd_len = size;
  }

  public void read(Packet packet) throws Exception {
    cmd = packet.readByte();
    switch (cmd & 0x7f) {
      case CIP.SUB_CMD_READTAG: {
        readReplyReadTag(packet);
        break;
      }
      case CIP.SUB_CMD_WRITETAG: {
        readReplyWriteTag(packet);
        break;
      }
      case CIP.SUB_CMD_GET_ATTR_ALL: {
        readReplyGetAttrs(packet);
        break;
      }
      case CIP.SUB_CMD_SET_ATTR_ALL: {
        readReplySetAttrs(packet);
        break;
      }
      default:
        throw new Exception("CIP:Unknown cmd:0x" + Integer.toHexString(cmd & 0xff));
    }
  }

  private void readReplyReadTag(Packet packet) throws Exception {
    reserved1 = packet.readByte();
    reserved2 = packet.readByte();
    reserved3 = packet.readByte();
    type = packet.readByte();
    reserved = packet.readByte();
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
    packet.read(data);
  }

  private void readReplyWriteTag(Packet packet) throws Exception {
    reserved1 = packet.readByte();
    reserved2 = packet.readByte();
    reserved3 = packet.readByte();
  }

  private void readReplyGetAttrs(Packet packet) throws Exception {
    count = packet.readByte();
    path_1 = packet.readByte();
    class_1 = packet.readByte();
    path_2 = packet.readByte();
    class_2 = packet.readByte();
    short attr_count = (short)packet.readShort();
    attrs = new byte[attr_count][];
    for(int i=0;i<attr_count;i++) {
      attrs[i] = new byte[8];
      packet.read(attrs[i]);
    }
  }

  private void readReplySetAttrs(Packet packet) throws Exception {
    count = packet.readByte();
    short attr_count = (short)packet.readShort();
    attrs = new byte[attr_count][];
    for(int i=0;i<attr_count;i++) {
      attrs[i] = new byte[1];
      attrs[i][0] = packet.readByte();  //0 = success
      packet.readByte();  //padding ???
    }
  }
}
