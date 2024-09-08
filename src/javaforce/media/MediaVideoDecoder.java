package javaforce.media;

/** Media "raw" video decoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;

public class MediaVideoDecoder extends MediaCoder {
  public native boolean start(int codec_id, int new_width, int new_height);
  public native void stop();
  public native int[] decode(byte[] data, int offset, int length);
  public int[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  public native short[] decode16(byte[] data, int offset, int length);
  public short[] decode16(Packet packet) {
    return decode16(packet.data, packet.offset, packet.length);
  }
  public native int getWidth();
  public native int getHeight();
  public native float getFrameRate();
}
