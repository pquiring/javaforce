package javaforce.ipc;

/** DBus IPC/RPC implementation
 *
 * Supported:
 *  - Linux:Unix Sockets
 *  - Windows:Pipes
 *  - invoking remote method and returning value
 *
 * Not Supported:
 *  - signal message (broadcasting methods)
 *
 * Supported Data Types:
 *  - java.lang.Integer
 *  - java.lang.Double
 *  - java.lang.Boolean
 *  - java.lang.String
 *  - javaforce.UInteger (not recommended) (primitive type is 'int')
 *
 * Notes:
 *  - sender field required to send back RPC reply
 *    - although technically not required for DBus on Linux
 *      it is required for the Windows Pipes implementation
 *  - methods must return a value (void is not supported)
 *    - return boolean at the least and always return true
 *      so caller knows call was successful
 *  - on Linux a dbus conf is installed to allow end points to
 *      use names that begin with "javaforce." (root only)
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.ipc.transport.*;

public class DBus implements IPC {

  private static final boolean debug = true;

  private static class Field {
    public byte type;
    public String sign;
    public Object value;  //variant
    public String toString() {
      return "DBus.Field:" + field_names[type] + "=" + value;
    }
  }

  //Message Types
  private static final byte MSG_CALL = 0x01;
  private static final byte MSG_RETURN = 0x02;
  private static final byte MSG_ERROR = 0x03;
  private static final byte MSG_SIGNAL = 0x04;

  private static final String[] msg_names = {
    "null",
    "call",
    "return",
    "error",
    "signal"
  };

  // Field Types                                         CALL RETURN ERROR SIGNAL Example (R=required O=optional)
  private static final byte FIELD_PATH         = 0x01;  // R                 R     /path/to/object
  private static final byte FIELD_INTERFACE    = 0x02;  // O                 R     com.example.InterfaceName
  private static final byte FIELD_MEMBER       = 0x03;  // R                 R     MethodName
  private static final byte FIELD_ERROR_MSG    = 0x04;  //             R           Error msg
  private static final byte FIELD_REPLY_SERIAL = 0x05;  //      R      R           Serial # from method_call
  //6-9 = optional
  private static final byte FIELD_DEST         = 0x06;  //                         Unique dest ID (message bus)
  private static final byte FIELD_SENDER       = 0x07;  //                         Unique sender ID (message bus)
  private static final byte FIELD_SIGNATURE    = 0x08;  // O    O            O     If omitted assumes body.length = 0 (no args)
  private static final byte FIELD_FD           = 0x09;  //                         # of fd (not used)

  private static final String[] field_names = {
    "null",
    "path",
    "interface",
    "member",
    "error",
    "reply_serial",
    "dest",
    "sender",
    "signature",
    "fd"
  };

  //Data Types (only small subset supported)
  private static final char TYPE_UINT8 = 'y';
  private static final char TYPE_INT16 = 'n';
  private static final char TYPE_UINT16 = 'q';
  private static final char TYPE_INT32 = 'i';
  private static final char TYPE_UINT32 = 'u';
  private static final char TYPE_INT64 = 'x';
  private static final char TYPE_UINT64 = 't';
  private static final char TYPE_DOUBLE = 'd';
  private static final char TYPE_BOOLEAN = 'b';
  private static final char TYPE_STRING = 's';
  private static final char TYPE_ARRAY = 'a';
  private static final char TYPE_STRUCT = 'r';
  private static final char TYPE_STRUCT_OPEN = '(';
  private static final char TYPE_STRUCT_CLOSE = ')';
  private static final char TYPE_DICT = 'e';
  private static final char TYPE_DICT_OPEN = '{';
  private static final char TYPE_DICT_CLOSE = '}';
  private static final char TYPE_VARIANT = 'v';
  private static final char TYPE_OBJECT_PATH = 'o';
  private static final char TYPE_SIGNATURE = 'g';
  private static final char TYPE_FD = 'h';

  private static final String DBusMessageBus = "org.freedesktop.DBus";

  private EndPoint ep;
  private Reader reader;
  private DBusTransport transport;
  private int timeout = 30 * 1000;
  private int serial = 1;
  private Object serial_lock = new Object();

  /** Create DBus with specified EndPoint. */
  public DBus(EndPoint ep) {
    this.ep = ep;
    if (JF.isUnix()) {
      transport = new UnixSocketTransport();
    } else {
      transport = new WinPipeTransport();
    }
  }

  /** Create DBus with specified EndPoint and transport. */
  public DBus(EndPoint ep, DBusTransport transport) {
    this.ep = ep;
    this.transport = transport;
  }

  /** Create a client end point with system provided name.
   * @param obj = where RPC methods reside
   */
  public static EndPoint createEndPoint(Object obj) {
    Dispatcher dispatcher = new Dispatcher(obj);
    EndPoint ep = new EndPoint() {
      public String getEndPointName() {
        return null;
      }
      public Object dispatch(String method, Object[] args) throws Exception {
        return dispatcher.dispatch(method, args);
      }
    };
    return ep;
  }

  /** Create a EndPoint with specified name (servers).
   * @param name = name of end point
   * @param obj = where RPC methods reside
   */
  public static EndPoint createEndPoint(String name, Object obj) {
    Dispatcher dispatcher = new Dispatcher(obj);
    EndPoint ep = new EndPoint() {
      public String getEndPointName() {
        return name;
      }
      public Object dispatch(String method, Object[] args) throws Exception {
        return dispatcher.dispatch(method, args);
      }
    };
    return ep;
  }

  /** Convert a message bus name to object path.
   * org.freedesktop.DBus becomes /org/freedesktop/DBus
   */
  public static String nameToPath(String name) {
    return "/" + name.replaceAll("[.]", "/");
  }

  /** Connects to message bus. */
  public boolean connect() {
    return transport.connect(ep.getEndPointName(), this, new Runnable() {
      public void run() {
        reader = new Reader();
        reader.start();
      }
    });
  }

  /** Disconnects from message bus. */
  public boolean disconnect() {
    boolean result = false;
    try {
      result = transport.disconnect();  //this should cause reader to abort
      if (Thread.currentThread() != reader) {
        reader.join();
      }
      reader = null;
      return result;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Set timeout for reply.  Default = 30 seconds.
   * @param ms = timeout (min = 3000, max = 60000)
   */
  public void setTimeout(int ms) {
    if (ms < 3000) ms = 3000;
    if (ms > 60 * 1000) ms = 60 * 1000;
    timeout = ms;
  }

  /** Return next serial # for unique msg. */
  private int nextSerial() {
    int value;
    synchronized(serial_lock) {
      value = serial++;
      if (serial == Integer.MAX_VALUE) {
        serial = 1;
      }
    }
    return value;
  }

  private static class Invoke {
    public int serial;  //standard message identifier
    public boolean dbus;  //org.freedesktop.DBus message
    public Object value;  //return value
    public Object error;  //return error
    public Object lock = new Object();  //timeout/notify lock
  }

  private Object invokes_lock = new Object();
  private ArrayList<Invoke> invokes = new ArrayList<>();

  /** Invokes method in remote end point.
   * @param dest = destination end point
   * @param method = method to invoke
   * @param args = arguments
   * @return return value from remote method
   * @exception thrown if method returned at error message or no reply within timeout duration
   *   Errors could be method not found, mismatch arguments, etc.
   */
  public Object invoke(String dest, String method, Object[] args) throws Exception {
    if (debug) JFLog.log("DBus.invoke:" + dest + "." + method);
    Invoke invoke = new Invoke();
    invoke.serial = nextSerial();
    if (dest.equals(DBusMessageBus)) {
      invoke.dbus = true;
    }
    synchronized (invoke.lock) {
      synchronized (invokes_lock) {
        write_msg(MSG_CALL, dest, invoke.serial, -1, method, args);
        invokes.add(invoke);
      }
      try {
        invoke.lock.wait(timeout);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    synchronized (invokes_lock) {
      invokes.remove(invoke);
    }
    if (invoke.error != null) {
      throw new Exception((String)invoke.error);
    } else {
      if (invoke.value == null) {
        throw new Exception("DBus.timeout");
      }
      return invoke.value;
    }
  }

  private Object write_lock = new Object();

  private boolean write(String path, byte[] data, int offset, int length) {
    if (debug) {
      JFLog.log("DBus.write.length=" + length);
      JFLog.log("packet=", data, offset, length);
    }
    synchronized (write_lock) {
      return transport.write(path, data, offset, length);
    }
  }

  private int read(byte[] data) {
    return transport.read(data);
  }

  int bodyLength;
  /** Align buffer position to data type size. */
  private void balign(int size) {
    int align = bodyLength & (size-1);
    if (align == 0) return;
    int pad = size - align;
    bodyLength += pad;
  }

  /** Calculates size of body.
   *
   * Note : this method is already synced with write_msg()
   */
  private int args_length(Object[] args) {
    int argsLength = args.length;
    bodyLength = 0;
    for (int a = 0; a < argsLength; a++) {
      if (args[a] instanceof Integer) {
        balign(4);
        bodyLength += 4;
      } else if (args[a] instanceof UInteger) {
        balign(4);
        bodyLength += 4;
      } else if (args[a] instanceof Double) {
        balign(8);
        bodyLength += 8;
      } else if (args[a] instanceof Boolean) {
        balign(4);
        bodyLength += 4;  //only LSB is used
      } else if (args[a] instanceof String) {
        String value = (String)args[a];
        balign(4);
        bodyLength += 4;  //length
        bodyLength += value.length();  //UTF-8 bytes
        bodyLength++;  //null
      } else {
        JFLog.log("DBus:Error:Unknown type:" + args[a].getClass());
        return 0;
      }
    }
    return bodyLength;
  }

  /** Generates method signature. */
  private String args_sign(Object[] args) {
    int argsLength = args.length;
    StringBuilder sign = new StringBuilder();
    for (int a = 0; a < argsLength; a++) {
      if (args[a] instanceof Integer) {
        sign.append(TYPE_INT32);
      } else if (args[a] instanceof UInteger) {
        sign.append(TYPE_UINT32);
      } else if (args[a] instanceof Double) {
        sign.append(TYPE_DOUBLE);
      } else if (args[a] instanceof Boolean) {
        sign.append(TYPE_BOOLEAN);
      } else if (args[a] instanceof String) {
        sign.append(TYPE_STRING);
      } else {
        JFLog.log("DBus:Error:Unknown type:" + args[a].getClass());
        return null;
      }
    }
    return sign.toString();
  }

  //write buffer
  private byte[] wpkt = new byte[1024];
  private int wpos;

  /** Align write buffer position to data type size. */
  private void walign(int size) {
    int align = wpos & (size-1);
    if (align == 0) return;
    int pad = size - align;
    for(int i=0;i<pad;i++) {
      wpkt[wpos++] = 0;
    }
  }
  /** Expand write buffer as needed. */
  private void wcheck(int size) throws Exception {
    while (wpos + size > wpkt.length) {
      int cur_len = wpkt.length;
      byte[] new_wpkt = new byte[cur_len << 1];
      System.arraycopy(wpkt, 0, new_wpkt, 0, cur_len);
      wpkt = new_wpkt;
    }
  }
  private void write_byte(byte value) throws Exception {
    //no alignment check
    wcheck(1);
    wpkt[wpos++] = value;
  }
  private void write_byte(char value) throws Exception {
    write_byte((byte)value);
  }
  private void write_int(int value) throws Exception {
    walign(4);
    wcheck(4);
    LE.setuint32(wpkt, wpos, value);
    wpos += 4;
  }
  private void write_double(double value) throws Exception {
    walign(8);
    wcheck(8);
    LE.setdouble(wpkt, wpos, value);
    wpos += 8;
  }
  private void write_boolean(boolean value) throws Exception {
    walign(4);
    wcheck(4);
    LE.setuint32(wpkt, wpos, value ? 1 : 0);
    wpos += 4;
  }
  private void write_String(String value) throws Exception {
    int strlen = value.length();
    write_int(strlen);
    wcheck(strlen + 1);
    System.arraycopy(value.getBytes(), 0, wpkt, wpos, strlen);
    wpos += strlen;
    wpkt[wpos++] = 0;  //null
  }
  private void write_sign(char value) throws Exception {
    write_byte((byte)1);
    wcheck(1 + 1);
    wpkt[wpos++] = (byte)value;
    wpkt[wpos++] = 0;  //null
  }
  private void write_sign(String value) throws Exception {
    int strlen = value.length();
    if (strlen > 255) {
      throw new Exception("DBus.Error:signature length > 255");
    }
    write_byte((byte)strlen);
    wcheck(strlen + 1);
    System.arraycopy(value.getBytes(), 0, wpkt, wpos, strlen);
    wpos += strlen;
    wpkt[wpos++] = 0;  //null
  }

  private Object write_msg_lock = new Object();

  private void write_msg(byte type, String dest, int serial, int serial_reply, String member, Object[] args) {
    synchronized (write_msg_lock) {
      boolean write_to_dbus = dest.equals(DBusMessageBus);
      boolean dest_generic = dest.startsWith(":");
      int body_size = args_length(args);
      String sign = args_sign(args);
      wpos = 0;
      if (args == null) args = new Object[0];

      try {
        write_byte((byte)'l');  //little endian
        write_byte(type);
        write_byte((byte)0);  //flags
        write_byte((byte)1);  //major version

        write_int(body_size);
        if (debug) JFLog.log("write.serial:" + serial);
        write_int(serial);

        //write fields (DEST, METHOD, SIGNATURE, SENDER, [REPLY_SERIAL])
        //each field is a struct so it must be 8 byte aligned
        write_int(-1);  //array size in bytes (excluding init padding)
        int array_offset = wpos - 4;
        int array_start = wpos;
        if (!dest_generic) {
          //field:dest
          walign(8);
          if (debug) JFLog.log("write.field:OBJ_PATH:" + nameToPath(dest));
          write_byte(FIELD_PATH);
          write_sign(TYPE_OBJECT_PATH);
          write_String(nameToPath(dest));
        }
        if (!dest_generic) {
          //field:interface
          walign(8);
          if (debug) JFLog.log("write.field:INTERFACE:" + dest);
          write_byte(FIELD_INTERFACE);
          write_sign(TYPE_STRING);
          write_String(dest);
        }
        //field:member
        walign(8);
        if (debug) JFLog.log("write.field:MEMBER:" + member);
        write_byte(FIELD_MEMBER);
        write_sign(TYPE_STRING);
        write_String(member);
        if (args.length > 0) {
          //field:signature
          walign(8);
          if (debug) JFLog.log("write.field:SIGNATURE:" + sign);
          write_byte(FIELD_SIGNATURE);
          write_sign(TYPE_SIGNATURE);
          write_sign(sign);
        }
        if (!write_to_dbus) {
          //field:sender
          walign(8);
          if (debug) JFLog.log("write.field:SENDER:" + transport.getBusName());
          write_byte(FIELD_SENDER);
          write_sign(TYPE_STRING);
          write_String(transport.getBusName());
        }
        //field:dest
        walign(8);
        if (debug) JFLog.log("write.field:DEST:" + dest);
        write_byte(FIELD_DEST);
        write_sign(TYPE_STRING);
        write_String(dest);
        if (type == MSG_RETURN) {
          //field:reply_serial
          walign(8);
          if (debug) JFLog.log("write.field:REPLY_SERIAL:" + serial_reply);
          write_byte(FIELD_REPLY_SERIAL);
          write_sign(TYPE_UINT32);
          write_int(serial_reply);
        }
        //patch array fields size (excluding end of header padding to 8 bytes)
        int array_size = wpos - array_start;
        if (debug) {
          JFLog.log("array_size=" + array_size);
        }
        LE.setuint32(wpkt, array_offset, array_size);
        walign(8);  //end of header padding to 8 bytes

        //write args (body)
        for(Object obj : args) {
          if (obj instanceof Integer) {
            write_int((int)obj);
          } else if (obj instanceof UInteger) {
            UInteger uint = (UInteger)obj;
            write_int(uint.getValue());
          } else if (obj instanceof Double) {
            write_double((double)obj);
          } else if (obj instanceof Boolean) {
            write_boolean((boolean)obj);
          } else if (obj instanceof String) {
            write_String((String)obj);
          } else {
            throw new Exception("DBus:Error:Unknown type:" + obj.getClass());
          }
        }
        //write packet
        write(dest, wpkt, 0, wpos);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  private class Reader extends Thread {
    byte[] rpkt = new byte[1024];
    int rpos = 0;
    int rpkt_len;
    boolean le;
    byte msg_type;
    byte msg_flags;
    byte msg_ver;
    int body_len;
    int serial;
    int field_size;
    ArrayList<Field> fields = new ArrayList<>();
    public void run() {
      while (transport.isAlive()) {
        if (debug) {
          JFLog.log("DBus.reading...");
        }
        rpkt_len = read(rpkt);
        if (debug) {
          JFLog.log("DBus.read.length=" + rpkt_len);
        }
        if (rpkt_len < 0) {
          disconnect();
          break;
        }
        if (rpkt_len == 0) {
          JF.sleep(100);
          continue;
        }
        rpos = 0;
        fields.clear();
        //header = yyyyuua(yv) : y=8bit u=32bit a=array v=variant (64bit)
        // y = endian : l or B
        // y = MSG_...
        // y = FLAG_...
        // y = version == 1
        // u = body length (32bits)
        // u = serial # of msg
        // a(yv) = fields
        // {body}
        try {
          le = read_byte() == 'l';  //'l' or 'B'
          msg_type = read_byte();
          msg_flags = read_byte();
          msg_ver = read_byte();
          body_len = read_int();
          serial = read_int();
          field_size = read_int();
          if (debug) {
            JFLog.log("msg:" + msg_names[msg_type]);
            JFLog.log("field_size=" + field_size);
          }
          int field_end = rpos + field_size;
          while (rpos < field_end) {
            Field field = new Field();
            fields.add(field);
            ralign(8);  //each field is a struct that is aligned to 8 bytes
//            read_struct_open();
            //read field type
            field.type = read_byte();
            //read variant {sign + value}
            field.sign = read_sign();
            switch (field.type) {
              case FIELD_PATH:
                if (!field.sign.equals("o")) {
                  throw new Exception("field:mismatch type:o != " + field.sign);
                }
                field.value = read_String();
                break;
              case FIELD_INTERFACE:
              case FIELD_MEMBER:
              case FIELD_ERROR_MSG:
              case FIELD_DEST:
              case FIELD_SENDER:
                if (!field.sign.equals("s")) {
                  throw new Exception("field:mismatch type:s != " + field.sign);
                }
                field.value = read_String();
                break;
              case FIELD_SIGNATURE:
                if (!field.sign.equals("g")) {
                  throw new Exception("field:mismatch type:g != " + field.sign);
                }
                field.value = read_sign();
                break;
              case FIELD_REPLY_SERIAL:
              case FIELD_FD:
                if (!field.sign.equals("u")) {
                  throw new Exception("field:mismatch type:u != " + field.sign);
                }
                field.value = read_int();
                break;
              default:
                throw new Exception("bad field:" + field.type);
            }
//            read_struct_close();
            if (debug) {
              JFLog.log("read.field:" + field);
            }
          }
          ralign(8);  //end of header pads to 8 byte alignment
          switch (msg_type) {
            case MSG_CALL:
              method_call();
              break;
            case MSG_RETURN:
              method_return();
              break;
            case MSG_ERROR:
              method_error();
              break;
            case MSG_SIGNAL:
              method_return();
              break;
          }
        } catch (Exception e) {
          JFLog.log(e);
          disconnect();
          return;
        }
      }
    }
    private void method_call() throws Exception {
      String path = null;
      String sender = null;
      String member = null;
      String sign = null;
      int cnt = fields.size();
      for(int a=0;a<cnt;a++) {
        Field field = fields.get(a);
        switch (field.type) {
          case FIELD_MEMBER:
            member = (String)field.value;
            break;
          case FIELD_PATH:
            path = (String)field.value;
            break;
          case FIELD_SIGNATURE:
            sign = (String)field.value;
            break;
          case FIELD_SENDER:
            sender = (String)field.value;
            break;
        }
      }
      if (member == null) {
        JFLog.log("DBus:Error:No method name found");
        return;
      }
      //get args using signature and body
      Object[] args = read_args(sign);
      try {
        Object ret = ep.dispatch(member, args);
        write_msg(MSG_RETURN, sender, nextSerial(), serial, member, new Object[] {ret});
      } catch (Exception e) {
        write_msg(MSG_ERROR, sender, nextSerial(), serial, member, new Object[] {e.toString()});
      }
    }
    private void method_return() throws Exception {
      String path = null;
      String sender = null;
      String member = null;
      String sign = null;
      boolean dbus = false;
      int reply_serial = -1;
      int cnt = fields.size();
      for(int a=0;a<cnt;a++) {
        Field field = fields.get(a);
        switch (field.type) {
          case FIELD_MEMBER:
            member = (String)field.value;
            break;
          case FIELD_PATH:
            path = (String)field.value;
            break;
          case FIELD_SIGNATURE:
            sign = (String)field.value;
            break;
          case FIELD_SENDER:
            sender = (String)field.value;
            if (sender.equals(DBusMessageBus)) {
              dbus = true;
            }
            break;
          case FIELD_REPLY_SERIAL:
            reply_serial = (Integer)field.value;
            break;
        }
      }
      Object[] args = read_args(sign);
      synchronized (invokes_lock) {
        for(Invoke invoke : invokes) {
          if (invoke.serial == reply_serial || invoke.dbus && dbus) {
            invoke.value = args[0];
            synchronized (invoke.lock) {
              invoke.lock.notify();
            }
            return;
          }
        }
      }
      JFLog.log("DBus:Warning:msg_return():no pending invoke found");
    }
    private void method_error() throws Exception {
      String path = null;
      String sender = null;
      String member = null;
      String sign = null;
      int reply_serial = -1;
      int cnt = fields.size();
      for(int a=0;a<cnt;a++) {
        Field field = fields.get(a);
        switch (field.type) {
          case FIELD_MEMBER:
            member = (String)field.value;
            break;
          case FIELD_PATH:
            path = (String)field.value;
            break;
          case FIELD_SIGNATURE:
            sign = (String)field.value;
            break;
          case FIELD_SENDER:
            sender = (String)field.value;
            break;
          case FIELD_REPLY_SERIAL:
            reply_serial = (Integer)field.value;
            break;
        }
      }
      Object[] args = read_args(sign);
      synchronized (invokes_lock) {
        for(Invoke invoke : invokes) {
          if (invoke.serial == reply_serial) {
            invoke.error = args[0];
            synchronized (invoke.lock) {
              invoke.lock.notify();
            }
            return;
          }
        }
      }
    }
    private Object[] read_args(String sign) throws Exception {
      //get args using signature and body
      char[] types = sign.toCharArray();
      Object[] args = new Object[types.length];
      int idx = 0;
      for(char type : types) {
        switch (type) {
          case TYPE_UINT8: {
            args[idx] = read_byte();
            break;
          }
          case TYPE_UINT16:
          case TYPE_INT16: {
            args[idx] = read_short();
            break;
          }
          case TYPE_UINT32:
          case TYPE_INT32: {
            args[idx] = read_int();
            break;
          }
          case TYPE_DOUBLE: {
            args[idx] = read_double();
            break;
          }
          case TYPE_BOOLEAN: {
            args[idx] = (read_int() == 1);
            break;
          }
          case TYPE_STRING: {
            args[idx] = read_String();
            if (debug) {
              JFLog.log("read.string=" + args[idx]);
            }
            break;
          }
          default: {
            JFLog.log("DBus:Error:Unsupported type:" + type);
            break;
          }
        }
        idx++;
      }
      return args;
    }
    /** Align read buffer position to data type size. */
    private void ralign(int size) {
      int align = rpos & (size-1);
      if (align == 0) return;
      int pad = size - align;
      rpos += pad;
    }
    /** Check for read buffer underflow. */
    private void rcheck(int size) throws Exception {
      if (rpos + size > rpkt_len) throw new Exception("buffer underflow");
    }
    private byte read_byte() throws Exception {
      rcheck(1);
      return rpkt[rpos++];
    }
    private int read_short() throws Exception {
      ralign(2);
      rcheck(2);
      int value;
      if (le) {
        value = LE.getuint16(rpkt, rpos);
      } else {
        value = BE.getuint16(rpkt, rpos);
      }
      rpos += 2;
      return value;
    }
    private int read_int() throws Exception {
      ralign(4);
      rcheck(4);
      int value;
      if (le) {
        value = LE.getuint32(rpkt, rpos);
      } else {
        value = BE.getuint32(rpkt, rpos);
      }
      rpos += 4;
      return value;
    }
    private long read_long() throws Exception {
      ralign(8);
      rcheck(8);
      long value;
      if (le) {
        value = LE.getuint64(rpkt, rpos);
      } else {
        value = BE.getuint64(rpkt, rpos);
      }
      rpos += 8;
      return value;
    }
    private double read_double() throws Exception {
      ralign(8);
      rcheck(8);
      double value;
      if (le) {
        value = LE.getdouble(rpkt, rpos);
      } else {
        value = BE.getdouble(rpkt, rpos);
      }
      rpos += 8;
      return value;
    }
    private String read_String() throws Exception {
      int strlen = read_int();
      rcheck(strlen + 1);  //+1 for null
      String str = new String(rpkt, rpos, strlen);
      rpos += strlen;
      rpos++;  //null
      return str;
    }
    private String read_sign() throws Exception {
      int strlen = read_byte();
      rcheck(strlen + 1);  //+1 for null
      String str = new String(rpkt, rpos, strlen);
      rpos += strlen;
      rpos++;  //null
      return str;
    }
    private void read_struct_open() throws Exception {
      if (rpkt[rpos++] != TYPE_STRUCT_OPEN) {
        throw new Exception("DBus:Error:expecting struct open '('");
      }
    }
    private void read_struct_close() throws Exception {
      if (rpkt[rpos++] != TYPE_STRUCT_CLOSE) {
        throw new Exception("DBus:Error:expecting struct close ')'");
      }
    }
  }
}
