package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_object : Wayland Base Object.
 *
 * @author pquiring
 */

public abstract class WLObject {
  public static boolean debug = true;
  public static boolean debug_packet = false;

  public WLClient client;
  public int id;
  public int ver = 1;
  public Method[] events;
//  public Method[] requests;
  protected WLNotify notify;

  public WLObject(WLClient client, int id) {
    this.client = client;
    this.id = id;
    client.setObject(id, this);
  }

  public abstract String getName();

  public void setVersion(int ver) {
    this.ver = ver;
  }

  public int getVersion() {
    return ver;
  }

  private int align32(int offset) {
    int diff = offset & 0x3;
    if (diff == 0) return offset;
    int pad = (0x03 - diff);
    return offset + pad;
  }
  public boolean dispatchEvent(int opcode, byte[] pkt, int offset, int length) {
    if (opcode >= events.length) {
      if (debug) JFLog.log("ERROR:WLObject.dispatchEvent:opcode >= events:this=" + getClass().getName() + ":opcode=" + opcode);
      return false;
    }
    Method method = events[opcode];
    if (method == null) {
      if (debug) JFLog.log("ERROR:WLObject.dispatchEvent:method==null:this=" + getClass().getName() + ":opcode=" + opcode);
      return false;
    }
    if (debug) JFLog.log("WLObject.dispatchEvent:this=" + getClass().getName() + ":opcode=" + opcode + ":method=" + method.getName());
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
          int strlen = LE.getuint32(pkt, offset);  //includes null
          offset += 4;
          args[a] = new String(pkt, offset, strlen - 1);
          offset += strlen;
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
    if (notify != null) {
      notify.onEvent(getName(), method.getName(), args);
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
            LE.setuint32(pkt, offset, strlen + 1);  //includes null
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
    if (debug_packet) JFLog.log("write.packet=", pkt, 0, pktlen);
    return client.write(pkt, 0, pktlen);
  }
  public void setNotify(WLNotify notify) {
    this.notify = notify;
  }
}
