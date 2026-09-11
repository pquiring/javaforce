package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_object : Wayland Base Object.
 *
 * @author pquiring
 */

public class WLObject {
  public static boolean debug = true;

  public WLClient client;
  public int id;
  public Method[] events;
//  public Method[] requests;

  public WLObject(WLClient client) {
    this.client = client;
  }
  private int align32(int offset) {
    int diff = offset & 0x3;
    if (diff == 0) return offset;
    int pad = (0x03 - diff);
    return offset + pad;
  }
  public boolean dispatchEvent(int opcode, byte[] pkt, int offset, int length) {
    if (debug) JFLog.log("WLObject.dispatchEvent:this=" + getClass().getName() + ":opcode=" + opcode);
    if (opcode >= events.length) {
      return false;
    }
    Method method = events[opcode];
    if (method == null) {
      return false;
    }
    if (debug) JFLog.log("WLObject.dispatchEvent:method=" + method.getName());
    Class[] types = method.getParameterTypes();
    Object[] args = new Object[types.length];
    //unmarshal args from byte[]
    for(int a=0;a<types.length;a++) {
      Class cls = types[a];
      String type = cls.getName();
      switch (type) {
        case "java.lang.Integer":
        case "int":
          args[a] = LE.getuint32(pkt, offset);
          offset += 4;
          break;
        case "java.lang.String":
          int strlen = LE.getuint32(pkt, offset);
          offset += 4;
          args[a] = new String(pkt, offset, strlen - 1);
          offset += strlen;
//          offset++;  //null byte
          break;
        default:
          JFLog.log("WLObject:unknown arg type:" + type);
          return false;
      }
      //each arg is aligned to 32bits (padding as needed)
      offset = align32(offset);
    }
    try {
      method.invoke(this, args);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return true;
  }
  private boolean invokeRequest(int id, int opcode, byte[] args) {
    int len = 8 + args.length;
    byte[] pkt = new byte[len];
    LE.setuint32(pkt, 0, id);
    LE.setuint16(pkt, 4, opcode);
    LE.setuint16(pkt, 6, pkt.length);  //full packet length including header
    System.arraycopy(args, 0, pkt, 8, args.length);
    return client.write(pkt, 0, pkt.length);
  }
  public boolean invokeRequest(int id, int opcode, Object... args) {
    //marshall args into byte[]
    int pktlen = 8;  //header
    if (args != null) {
      for(int a=0;a<args.length;a++) {
        Object arg = args[a];
        Class cls = arg.getClass();
        String type = cls.getName();
        switch (type) {
          case "java.lang.Integer":
            pktlen += 4;
            break;
          case "java.lang.String":
            String str = (String)arg;
            pktlen += 4; //length
            pktlen += str.length();
            pktlen++;  //null
            break;
          default:
            JFLog.log("Error:Unknown type:" + type);
            break;
        }
        pktlen = align32(pktlen);
      }
    }
    if (debug) JFLog.log("WL:invokeRequest(" + id + "," + opcode + "):length=" + pktlen);
    byte[] pkt = new byte[pktlen];
    LE.setuint32(pkt, 0, id);
    LE.setuint16(pkt, 4, opcode);
    LE.setuint16(pkt, 6, pktlen);  //full packet length including header
    int offset = 8;
    if (args != null) {
      for(int a=0;a<args.length;a++) {
        Object arg = args[a];
        Class cls = arg.getClass();
        String type = cls.getName();
        switch (type) {
          case "java.lang.Integer":
            int value = (Integer)arg;
            LE.setuint32(pkt, offset, value);
            offset += 4;
            break;
          case "java.lang.String":
            String str = (String)arg;
            int strlen = str.length();
            LE.setuint32(pkt, offset, strlen);
            offset += 4;
            System.arraycopy(str.getBytes(), 0, pkt, offset, strlen);
            offset += strlen;
            offset++;  //null
            break;
          default:
            JFLog.log("Error:Unknown type:" + type);
            break;
        }
        offset = align32(offset);
      }
    }
    if (debug) JFLog.log("write.packet=", pkt, 0, pktlen);
    return client.write(pkt, 0, pktlen);
  }
}
