package javaforce.controls.ab;

import javaforce.LE;
import javax.swing.text.Segment;

/** CIP : Connection Manager : Request
 *
 * @author pquiring
 */

public class CIP_Request {
  //CIP header
  public byte cmd = (byte)0x52;
  public byte count = 2;
  public byte path_1 = 0x20;  //8bit class segment
  public byte class_1 = 0x06;  //connection manager
  public byte path_2 = 0x24;  //8bit instance segment
  public byte class_2 = 0x01;  //instance

  //connection manager header
  public byte ticktime = 0x07;
  public byte ticktimeout = (byte)0xf9;
  public short len;  //size of following {} in bytes
  //{
    public byte service;
    public byte tag_size;  //size of segments in 16bit words (multiple segments)
    public TagSegment[] segments;
    public byte[] tagdata;
  //}
  public byte route_size = 0x01;  //size of following {} in 16bit words
  public byte route_res = 0x00;  //reserved
  //{
    public byte route_seg = 0x01;
    public byte route_addr = 0x00;
  //}

  private static abstract class TagSegment {
    public TagSegment(byte type) {
      this.type = type;
    }
    public byte type;
    //...
    public abstract int size();
    public abstract void write(byte[] data, int offset);
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
    public void write(byte[] data, int offset) {
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
    public void write(byte[] data, int offset) {
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
    public void write(byte[] data, int offset) {
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
    public void write(byte[] data, int offset) {
      data[offset++] = type;
      data[offset++] = pad;
      LE.setuint32(data, offset, idx);
    }
  }

  public static final byte SERVICE_READTAG = 0x4c;
  public static final byte SERVICE_WRITETAG = 0x4d;

  public int size() {
    int segs_len = 0;
    for(int a=0;a<segments.length;a++) {
      segs_len += segments[a].size();
    }
    int size = 12 + segs_len + tagdata.length + 4;
    return size;
  }

  public void write(byte[] data, int offset) throws Exception {
    data[offset++] = cmd;
    data[offset++] = count;
    data[offset++] = path_1;
    data[offset++] = class_1;
    data[offset++] = path_2;
    data[offset++] = class_2;
    data[offset++] = ticktime;
    data[offset++] = ticktimeout;
    LE.setuint16(data, offset, len); offset += 2;
    data[offset++] = service;
    data[offset++] = tag_size;
    for(int a=0;a<segments.length;a++) {
      segments[a].write(data, offset);
      offset += segments[a].size();
    }
    System.arraycopy(tagdata, 0, data, offset, tagdata.length); offset += tagdata.length;
    data[offset++] = route_size;
    data[offset++] = route_res;
    data[offset++] = route_seg;
    data[offset++] = route_addr;
  }

  private void decodeTag(String tag) {
    String[] segs = tag.split("[.]");
    int len = segs.length;
    for(int a=0;a<segs.length;a++) {
      String seg = segs[a];
      if (seg.endsWith("]")) {
        len++;  //array index
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
        int idx = Integer.valueOf(seg.substring(i1+1, i2));
        segments[pos++] = new TagName(name);
        if (idx < 256) {
          segments[pos++] = new TagElement8((byte)idx);
        } else if (idx < 65536) {
          segments[pos++] = new TagElement16((short)idx);
        } else {
          segments[pos++] = new TagElement32(idx);
        }
      } else {
        segments[pos++] = new TagName(seg);
      }
    }
  }

  public void setRead(String tag) {
    service = SERVICE_READTAG;
    decodeTag(tag);
    tagdata = new byte[] {0x01, 0x00};  //count
    setLengths();
  }

  public void setWrite(String tag, byte type, byte[] data) {
    service = SERVICE_WRITETAG;
    decodeTag(tag);
    tagdata = new byte[4 + data.length];
    tagdata[0] = type;
    tagdata[1] = 0;
    tagdata[2] = 1;
    tagdata[3] = 0;
    System.arraycopy(data, 0, tagdata, 4, data.length);
    setLengths();
  }

  private void setLengths() {
    int segs_len = 0;
    for(int a=0;a<segments.length;a++) {
      segs_len += segments[a].size();
    }
    len = (short)(2 + segs_len + tagdata.length);
    tag_size = (byte)((segs_len) >> 1);
  }
}
