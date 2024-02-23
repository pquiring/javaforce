package javaforce.voip;

/** PacketBuffer
 *
 * Combines H264/H265 packets into full frames.
 *
 * For H265 sometimes a frame is split into slices.
 *
 */

import javaforce.*;

public class PacketBuffer {
  private static final int maxPacketsSize = 16 * 1024 * 1024;
  private static final int maxPackets = 256;

  public boolean debug = false;

  /** PacketBuffer
   *
   * @param codecType = H264 or H265
   */
  public PacketBuffer(int codecType) {
    this.codecType = codecType;
    data = new byte[maxPacketsSize];
    nextFrame.data = new byte[maxPacketsSize];
    switch(codecType) {
      case CodecType.H264: h264 = new RTPH264(); break;
      case CodecType.H265: h265 = new RTPH265(); break;
    }
  }
  public byte[] data;
  private Packet nextFrame = new Packet();
  private RTPH264 h264;
  private RTPH265 h265;
  private int codecType;
  private boolean started;
  public int[] offset = new int[maxPackets];
  public int[] length = new int[maxPackets];
  public byte[] type = new byte[maxPackets];
  public int nextOffset;
  public int head, tail;
  public int log;
  public void setLog(int id) {
    this.log = id;
  }
  public void reset() {
    //TODO : need to lock this from consumer
    head = 0;
    tail = 0;
    nextOffset = 0;
    started = false;
  }
  private boolean calcOffset(int nextLength) {
    if (nextOffset + nextLength >= maxPacketsSize) {
      nextOffset = 0;
    }
    int next_head = head + 1;
    if (next_head == maxPackets) {
      next_head = 0;
    }
    if (next_head == tail) {
      JFLog.log(log, "PacketBuffer : Error : Buffer Overflow (# of packets exceeded)");
      reset();
      return false;
    }
    int _tail = tail;
    if (head == _tail) return true;  //empty
    int total_length = 0;
    while (_tail != head) {
      total_length += length[_tail];
      _tail++;
      if (_tail == maxPackets) _tail = 0;
    }
    if (total_length + nextLength > maxPacketsSize) {
      JFLog.log(log, "PacketBuffer : Error : Buffer Overflow (# of bytes exceeded)");
      reset();
      return false;
    }
    return true;
  }
  public void add(Packet packet) {
    byte nal_type = 0;
    if (h264 != null) {
      nal_type = h264.get_nal_type(packet.data, 4);
      if (!h264.canDecodePacket(nal_type)) return;
      if (!started) {
        if (!h264.isStart(nal_type)) return;
        started = true;
      }
    }
    else if (h265 != null) {
      nal_type = h265.get_nal_type(packet.data, 4);
      if (!h265.canDecodePacket(nal_type)) return;
      if (!started) {
        if (!h265.isStart(nal_type)) return;
        started = true;
      }
    }
    if (debug) JFLog.log(log, "packet=" + nal_type);
    if (!calcOffset(packet.length)) return;
    try {
      System.arraycopy(packet.data, packet.offset, data, nextOffset, packet.length);
    } catch (Exception e) {
      JFLog.log(log, "PacketBuffer : Error : arraycopy(src," + packet.offset + ",dst," + nextOffset + "," + packet.length + ")");
      JFLog.log(log, e);
      return;
    }
    offset[head] = nextOffset;
    length[head] = packet.length;
    switch (codecType) {
      case CodecType.H264: type[head] = RTPH264.get_nal_type(packet.data, packet.offset + 4); break;
      case CodecType.H265: type[head] = RTPH265.get_nal_type_slice_flag(packet.data, packet.offset + 4); break;
    }
    nextOffset += packet.length;
    int new_head = head + 1;
    if (new_head == maxPackets) new_head = 0;
    head = new_head;
  }
  public void removePacket() {
    if (tail == head) {
      JFLog.log(log, "PacketBuffer : Error : Packets Buffer underflow");
      return;
    }
    int new_tail = tail + 1;
    if (new_tail == maxPackets) new_tail = 0;
    tail = new_tail;
  }
  private byte get_this_type(int offset) {
    if (offset == head) return 0;
    return type[offset];
  }
  private byte get_next_type(int offset) {
    offset++;
    if (offset == maxPackets) offset = 0;
    if (offset == head) return 0;
    return type[offset];
  }

  public boolean haveCompleteFrame() {
    for(int pos=tail;pos!=head;) {
      byte this_type = get_this_type(pos);
      byte next_type = get_next_type(pos);
      switch (codecType) {
        case CodecType.H264: if (h264.isFrame(this_type)) return true; break;
        case CodecType.H265: if (h265.isFrame(this_type, next_type)) return true; break;
      }
      pos++;
      if (pos == maxPackets) {
        pos = 0;
      }
    }
    return false;
  }

  public boolean isNextFrame_KeyFrame() {
    for(int pos=tail;pos!=head;) {
      byte this_type = get_this_type(pos);
      byte next_type = get_next_type(pos);
      switch (codecType) {
        case CodecType.H264:
          if (h264.isKeyFrame(this_type)) return true;
          if (h264.isIFrame(this_type)) return false;
          break;
        case CodecType.H265:
          if (h265.isKeyFrame(this_type, next_type)) return true;
          if (h265.isIFrame(this_type, next_type)) return false;
          break;
      }
      pos++;
      if (pos == maxPackets) pos = 0;
    }
    return false;
  }

  private int nal_size;
  private byte[] nal_list = new byte[64];

  public Packet getNextFrame() {
    if (!haveCompleteFrame()) {
      JFLog.log(log, "PacketBuffer : Error : getNextFrame() called but don't have one ???");
      return null;
    }
    nal_size = 0;
    nextFrame.length = 0;
    for(;tail!=head;) {
      System.arraycopy(data, offset[tail], nextFrame.data, nextFrame.length, length[tail]);
      nextFrame.length += length[tail];
      byte this_type = get_this_type(tail);
      byte next_type = get_next_type(tail);
      boolean done = false;
      switch (codecType) {
        case CodecType.H264: if (h264.isFrame(this_type)) done = true; break;
        case CodecType.H265: if (h265.isFrame(this_type, next_type)) done = true; break;
      }
      if (nal_size < nal_list.length) {
        nal_list[nal_size++] = this_type;
      }
      int new_tail = tail + 1;
      if (new_tail == maxPackets) new_tail = 0;
      tail = new_tail;
      if (done) break;
    }
    return nextFrame;
  }

  public String get_nal_list() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    for(int a=0;a<nal_size;a++) {
      if (a > 0) sb.append(',');
      sb.append(Integer.toString(nal_list[a] & 0xff, 16));
    }
    sb.append('}');
    return sb.toString();
  }

  public String toString() {
    return "PacketBuffer:tail=" + tail + ":head=" + head;
  }
}
