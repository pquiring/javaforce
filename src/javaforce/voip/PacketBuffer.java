package javaforce.voip;

/** PacketBuffer */

import javaforce.*;

public class PacketBuffer {
  private static final int maxPacketsSize = 16 * 1024 * 1024;
  private static final int maxPackets = 256;
  public PacketBuffer() {
    log = 0;
    init();
  }
  public PacketBuffer(int log) {
    this.log = log;
    init();
  }
  private void init() {
    data = new byte[maxPacketsSize];
    nextFrame.data = new byte[maxPacketsSize];
  }
  public byte[] data;
  private Packet nextFrame = new Packet();
  public int[] offset = new int[maxPackets];
  public int[] length = new int[maxPackets];
  public int[] type = new int[maxPackets];
  public int nextOffset;
  public int head, tail;
  public int log;
  public void reset() {
    //TODO : need to lock this from consumer
    head = 0;
    tail = 0;
    nextOffset = 0;
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
      JFLog.log(log, "Error : Buffer Overflow (# of packets exceeded)");
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
      JFLog.log(log, "Error : Buffer Overflow (# of bytes exceeded)");
      reset();
      return false;
    }
    return true;
  }
  public void add(Packet packet) {
    if (!calcOffset(packet.length)) return;
    try {
      System.arraycopy(packet.data, packet.offset, data, nextOffset, packet.length);
    } catch (Exception e) {
      JFLog.log(log, "Error:arraycopy(src," + packet.offset + ",dst," + nextOffset + "," + packet.length + ")");
      JFLog.log(log, e);
      return;
    }
    offset[head] = nextOffset;
    length[head] = packet.length;
    type[head] = packet.data[packet.offset + 4] & 0x1f;
    nextOffset += packet.length;
    int new_head = head + 1;
    if (new_head == maxPackets) new_head = 0;
    head = new_head;
  }
  public void removePacket() {
    if (tail == head) {
      JFLog.log(log, "Error:Packets Buffer underflow");
      return;
    }
    int new_tail = tail + 1;
    if (new_tail == maxPackets) new_tail = 0;
    tail = new_tail;
  }
  public void cleanPackets(boolean mark) {
    //only keep back to the last keyFrame (type 5)
    int key_frames = 0;
    for(int pos=tail;pos!=head;) {
      switch (type[pos]) {
        case 5: key_frames++; break;
      }
      pos++;
      if (pos == maxPackets) pos = 0;
    }
    if (key_frames <= 1) return;
    if (mark) {
      boolean i_frame = false;
      for(;tail!=head;) {
        switch (type[tail]) {
          case 1: i_frame = true; break;
          default: if (i_frame) return;
        }
        int new_tail = tail + 1;
        if (new_tail == maxPackets) new_tail = 0;
        tail = new_tail;
      }
    }
  }
  public boolean haveCompleteFrame() {
    for(int pos=tail;pos!=head;) {
      switch (type[pos]) {
        case 1: return true;
        case 5: return true;
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
      switch (type[pos]) {
        case 1: return false;
        case 5: return true;
      }
      pos++;
      if (pos == maxPackets) pos = 0;
    }
    return false;
  }
  public Packet getNextFrame() {
    next_frame_packets = 0;
    if (!haveCompleteFrame()) {
      JFLog.log(log, "Error : getNextFrame() called but don't have one ???");
      return null;
    }
    nextFrame.length = 0;
    for(int pos=tail;pos!=head;) {
      System.arraycopy(data, offset[pos], nextFrame.data, nextFrame.length, length[pos]);
      nextFrame.length += length[pos];
      next_frame_packets++;
      int this_type = type[pos];
      if (this_type == 1 || this_type == 5) {
        break;
      }
      pos++;
      if (pos == maxPackets) pos = 0;
    }
    return nextFrame;
  }
  public void removeNextFrame() {
    while (next_frame_packets > 0) {
      int new_tail = tail + 1;
      if (new_tail == maxPackets) new_tail = 0;
      tail = new_tail;
      next_frame_packets--;
    }
  }
  public int next_frame_packets;
  public String toString() {
    return "Packets:tail=" + tail + ":head=" + head;
  }
}
