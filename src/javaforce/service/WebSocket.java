package javaforce.service;

/** WebSocket
 *
 * @author pquiring
 *
 * See : RFC 6455
 */

import java.io.*;
import javaforce.JFLog;

public class WebSocket {
  protected InputStream is;
  protected OutputStream os;
  protected String url;

  public static final int TYPE_CONT = 0x0;  //do not use
  public static final int TYPE_TEXT = 0x1;
  public static final int TYPE_BINARY = 0x2;

  protected static final int TYPE_CLOSE = 0x08;
  protected static final int TYPE_PING = 0x09;
  protected static final int TYPE_PONG = 0x0a;

  /** Free to use data */
  public Object userobj;

  /** Returns URL used during WebSocket connection request. */
  public String getURL() {
    return url;
  }

  /** Writes a WebSocket message to client.
   * Type = TYPE_BINARY
   */
  public void write(byte msg[]) {
    write(msg, TYPE_BINARY);
  }

  /** Writes a WebSocket message to client.
   * @param type = TYPE_...
   */
  public void write(byte msg[], int type) {
    try {
      //encode a packet and write it
      int len = msg.length;
      if (len > 16777216) {
        throw new Exception("WebSocket message > 16MB");
      }
      if (len > 65535) {
        //create 64bit packet length
        byte header[] = new byte[10];
        header[0] = (byte)(0x80 | type);
        header[1] = 127;
        //bytes 2-6 : not supported (64bit length), only support 24bit
        header[7] = (byte)((len & 0xff0000) >> 16);
        header[8] = (byte)((len & 0xff00) >> 8);
        header[9] = (byte)(len & 0xff);
        os.write(header);
      } else if (len >= 126) {
        //create 16bit packet length
        byte header[] = new byte[4];
        header[0] = (byte)(0x80 | type);
        header[1] = 126;
        header[2] = (byte)((len & 0xff00) >> 8);
        header[3] = (byte)(len & 0xff);
        os.write(header);
      } else {
        //create 7bit packet length
        byte header[] = new byte[2];
        header[0] = (byte)(0x80 | type);
        header[1] = (byte)msg.length;
        os.write(header);
      }
      os.write(msg);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
