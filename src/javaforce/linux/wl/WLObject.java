package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_object : Wayland Base Object.
 *
 * @author pquiring
 */

public abstract class WLObject {
  public static boolean debug = true;

  public WLClient client;
  public int id;
  public int ver = 1;
  public Method[] requests;
  public Method[] events;
  protected WLNotify notify;

  public WLObject(WLClient client, int id) {
    this.client = client;
    this.id = id;
    client.setObject(id, this);
  }

  public abstract String get_wl_name();

  public void setVersion(int ver) {
    this.ver = ver;
  }

  public int getVersion() {
    return ver;
  }

  private int align32(int offset) {
    int diff = offset & 0x3;
    if (diff == 0) return offset;
    int pad = (0x04 - diff);
    return offset + pad;
  }
  public boolean dispatchEvent(int opcode, byte[] pkt, int offset, int length) {
    if (events == null || opcode >= events.length) {
      if (debug) client.log("ERROR:WLObject.dispatchEvent:opcode >= events:this=" + getClass().getName() + ":opcode=" + opcode);
      return false;
    }
    Method method = events[opcode];
    if (method == null) {
      if (debug) client.log("ERROR:WLObject.dispatchEvent:method==null:this=" + getClass().getName() + ":opcode=" + opcode);
      return false;
    }
    if (debug) client.log("WLObject.dispatchEvent:this=" + getClass().getName() + ":opcode=" + opcode + ":method=" + method.getName());
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
          client.log("WLObject:unknown arg type:" + type);
          return false;
      }
      //each arg is aligned to 32bits (padding as needed)
      offset = align32(offset);
    }
    try {
      method.invoke(this, args);
    } catch (Exception e) {
      client.log(e);
    }
    if (notify != null) {
      notify.onEvent(get_wl_name(), method.getName(), args);
    }
    return true;
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
            client.log("Error:Unknown type:" + type);
            break;
        }
        pktlen = align32(pktlen);
      }
    }
    if (debug) client.log("WL:invokeRequest(" + id + "," + opcode + "):length=" + pktlen);
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
            client.log("Error:Unknown type:" + type);
            break;
        }
        offset = align32(offset);
      }
    }
    return client.write(pkt, 0, pktlen);
  }
  public void setNotify(WLNotify notify) {
    this.notify = notify;
  }
  public boolean dispatchRequest(int id, int opcode, int size, byte[] pkt, int offset, int length) {
    if (requests == null || opcode >= requests.length) {
      if (debug) client.log("ERROR:WLObject.dispatchRequest:opcode >= requests:this=" + getClass().getName() + ":opcode=" + opcode);
      return false;
    }
    Method method = requests[opcode];
    if (method == null) {
      if (debug) client.log("ERROR:WLObject.dispatchRequest:method==null:this=" + getClass().getName() + ":opcode=" + opcode);
      return false;
    }
    if (debug) client.log("WLObject.dispatchRequest:this=" + getClass().getName() + ":opcode=" + opcode + ":method=" + method.getName());
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
          client.log("WLObject:unknown arg type:" + type);
          return false;
      }
      //each arg is aligned to 32bits (padding as needed)
      offset = align32(offset);
    }
    try {
      method.invoke(this, args);
    } catch (Exception e) {
      client.log(e);
    }
    if (notify != null) {
      notify.onRequest(get_wl_name(), method.getName(), args);
    }
    return true;
  }
}
