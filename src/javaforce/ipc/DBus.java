package javaforce.ipc;

import java.util.*;

import javaforce.*;
import javaforce.ipc.transport.*;

/** DBus IPC/RPC implementation.
 *
 * Supported:
 *  - Linux:Unix Sockets
 *  - Windows:Pipes
 *  - invoking remote method and returning value
 *  - signals (broadcasting method to all subscribed clients)
 *
 * Supported Data Types:
 *  - byte
 *  - short
 *  - int
 *  - long
 *  - double
 *  - boolean
 *  - String
 *  - javaforce.UShort
 *  - javaforce.UInteger
 *  - javaforce.ULong
 *  - dictionary entry (JFTuple&lt;String,Object&gt;)
 *  - struct (JFArray)
 *  - variant (JFVariant)
 *  - byte[]
 *  - short[]
 *  - int[]
 *  - long[]
 *  - double[]
 *  - boolean[]
 *  - String[]
 *  - javaforce.UShort[]
 *  - javaforce.UInteger[]
 *  - javaforce.ULong[]
 *  - array of dictionary entries (JFDictionary)
 *  - array of struct (JFArray[])
 *  - array of variant (JFVariant[])
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
 *      Clients should just use a system supplied bus name
 *      by returning null from EndPoint.getEndPointName();
 *      Use getBusName() to determine supplied name after connect().
 *  - there is no way to implement a member with unsigned types
 *      since Java does not have primitive unsigned data types
 *      they are provided only for outbound calls to native methods that use them
 *      such as org.freedesktop.DBus.RequestName(String, uint32)
 *  - dictionary, struct and variant types are only supported in return data from an invoke()
 *
 * @author pquiring
 */

public class DBus implements IPC {

  public static boolean debug = false;
  public static boolean debug_msg = false;
  public static boolean debug_reading = false;

  //common interfaces
  public static final String DBUS_MESSAGE_BUS = "org.freedesktop.DBus";
  public static final String DBUS_PEER = "org.freedesktop.DBus.Peer";
    //Ping()
    //GetMachineId()
  public static final String DBUS_PROPERTIES = "org.freedesktop.DBus.Properties";
    //Get()
    //Set()
    //GetAll()
  public static final String DBUS_INTROSPECTABLE = "org.freedesktop.DBus.Introspectable";
    //Introspect()
  public static final String DBUS_OBJECT_MANAGER = "org.freedesktop.DBus.ObjectManager";
    //GetManagedObjects()
    //InterfacesAdded()
    //InterfacesRemoved()

  private static final long max_packet_size = 32 * JF.MB;

  private static class Field {
    public byte type;
    public String sign;
    public Object value;  //variant
    public int idx = -1;  //value index in rpkt
    public String toString() {
      return field_names[type] + "=" + value;
    }
  }
  private static class Index {
    public int idx;
  }
  private static class Type extends Index {
    public int length;

    public Type clone() {
      Type c = new Type();
      c.idx = idx;
      c.length = length;
      return c;
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
    "NULL",
    "PATH",
    "INTERFACE",
    "MEMBER",
    "ERROR",
    "REPLY_SERIAL",
    "DEST",
    "SENDER",
    "SIGNATURE",
    "FD"
  };

  //Data Types
  public static final String TYPE_UINT8 = "y";

  public static final String TYPE_INT16 = "n";
  public static final String TYPE_UINT16 = "q";

  public static final String TYPE_INT32 = "i";
  public static final String TYPE_UINT32 = "u";

  public static final String TYPE_INT64 = "x";
  public static final String TYPE_UINT64 = "t";

  public static final String TYPE_DOUBLE = "d";
  public static final String TYPE_BOOLEAN = "b";
  public static final String TYPE_STRING = "s";
  public static final String TYPE_ARRAY = "a";
  public static final String TYPE_STRUCT = "r";
  public static final String TYPE_STRUCT_OPEN = "(";
  public static final String TYPE_STRUCT_CLOSE = ")";
  public static final String TYPE_DICT = "e";  //illegal : must use TYPE_ARRAY_DICT
  public static final String TYPE_DICT_OPEN = "{";
  public static final String TYPE_DICT_CLOSE = "}";
  public static final String TYPE_VARIANT = "v";
  public static final String TYPE_OBJECT_PATH = "o";
  public static final String TYPE_SIGNATURE = "g";
  public static final String TYPE_FD = "h";

  public static final String TYPE_ARRAY_UINT8 = "ay";

  public static final String TYPE_ARRAY_INT16 = "an";
  public static final String TYPE_ARRAY_UINT16 = "aq";

  public static final String TYPE_ARRAY_INT32 = "ai";
  public static final String TYPE_ARRAY_UINT32 = "au";

  public static final String TYPE_ARRAY_INT64 = "ax";
  public static final String TYPE_ARRAY_UINT64 = "at";

  public static final String TYPE_ARRAY_DOUBLE = "ad";
  public static final String TYPE_ARRAY_BOOLEAN = "ab";
  public static final String TYPE_ARRAY_STRING = "as";
  public static final String TYPE_ARRAY_DICT = "ae";
  public static final String TYPE_ARRAY_STRUCT = "ar";
  public static final String TYPE_ARRAY_VARIANT = "av";
  public static final String TYPE_ARRAY_OBJECT_PATH = "ao";

  public static final byte TYPE__UINT8 = 'y';

  public static final byte TYPE__INT16 = 'n';
  public static final byte TYPE__UINT16 = 'q';

  public static final byte TYPE__INT32 = 'i';
  public static final byte TYPE__UINT32 = 'u';

  public static final byte TYPE__INT64 = 'x';
  public static final byte TYPE__UINT64 = 't';

  public static final byte TYPE__DOUBLE = 'd';
  public static final byte TYPE__BOOLEAN = 'b';
  public static final byte TYPE__byte = 's';
  public static final byte TYPE__ARRAY = 'a';
  public static final byte TYPE__STRUCT = 'r';
  public static final byte TYPE__STRUCT_OPEN = '(';
  public static final byte TYPE__STRUCT_CLOSE = ')';
  public static final byte TYPE__DICT = 'e';  //illegal : must use TYPE__ARRAY_DICT
  public static final byte TYPE__DICT_OPEN = '{';
  public static final byte TYPE__DICT_CLOSE = '}';
  public static final byte TYPE__VARIANT = 'v';
  public static final byte TYPE__OBJECT_PATH = 'o';
  public static final byte TYPE__SIGNATURE = 'g';
  public static final byte TYPE__FD = 'h';

  /** Returns DBus data type of obj. */
  public static String getObjectType(Object obj) {
    //float32 is not supported ???
    if (obj instanceof Byte) {
      return TYPE_UINT8;
    } else if (obj instanceof Short) {
      return TYPE_INT16;
    } else if (obj instanceof UShort) {
      return TYPE_UINT16;
    } else if (obj instanceof Integer) {
      return TYPE_INT32;
    } else if (obj instanceof UInteger) {
      return TYPE_UINT32;
    } else if (obj instanceof Double) {
      return TYPE_DOUBLE;
    } else if (obj instanceof Boolean) {
      return TYPE_BOOLEAN;
    } else if (obj instanceof String) {
      return TYPE_STRING;
    } else if (obj instanceof JFObjectPath) {
      return TYPE_OBJECT_PATH;
    } else if (obj instanceof JFVariant) {
      return TYPE_VARIANT;
    } else if (obj instanceof JFTuple) {
      return TYPE_DICT;
    } else if (obj instanceof JFArray) {
      return TYPE_STRUCT;
    } else if (obj instanceof byte[]) {
      return TYPE_ARRAY_UINT8;
    } else if (obj instanceof short[]) {
      return TYPE_ARRAY_INT16;
    } else if (obj instanceof UShort[]) {
      return TYPE_ARRAY_UINT16;
    } else if (obj instanceof int[]) {
      return TYPE_ARRAY_INT32;
    } else if (obj instanceof UInteger[]) {
      return TYPE_ARRAY_UINT32;
    } else if (obj instanceof long[]) {
      return TYPE_ARRAY_INT64;
    } else if (obj instanceof ULong[]) {
      return TYPE_ARRAY_UINT64;
    } else if (obj instanceof double[]) {
      return TYPE_ARRAY_DOUBLE;
    } else if (obj instanceof boolean[]) {
      return TYPE_ARRAY_BOOLEAN;
    } else if (obj instanceof String[]) {
      return TYPE_ARRAY_STRING;
    } else if (obj instanceof JFObjectPath[]) {
      return TYPE_ARRAY_OBJECT_PATH;
    } else if (obj instanceof JFDictionary) {
      return TYPE_ARRAY_DICT;
    } else if (obj instanceof JFArray[]) {
      return TYPE_ARRAY_STRUCT;
    } else if (obj instanceof JFVariant[]) {
      return TYPE_ARRAY_VARIANT;
    } else {
      JFLog.log("DBus:Error:Unknown type:" + obj.getClass());
      return "-";
    }
  }

  private String getClassType(Class<?> cls) {
    if (cls == byte.class) {
      return TYPE_UINT8;
    }
    if (cls == short.class) {
      return TYPE_INT16;
    }
    if (cls == UShort.class) {
      return TYPE_UINT16;
    }
    if (cls == int.class) {
      return TYPE_INT32;
    }
    if (cls == UInteger.class) {
      return TYPE_UINT32;
    }
    if (cls == long.class) {
      return TYPE_INT64;
    }
    if (cls == ULong.class) {
      return TYPE_UINT64;
    }
    if (cls == double.class) {
      return TYPE_DOUBLE;
    }
    if (cls == boolean.class) {
      return TYPE_BOOLEAN;
    }
    if (cls == String.class) {
      return TYPE_STRING;
    }
    if (cls == JFObjectPath.class) {
      return TYPE_OBJECT_PATH;
    }
    if (cls == JFVariant.class) {
      return TYPE_VARIANT;
    }
    if (cls == JFTuple.class) {
      return TYPE_DICT;
    }
    if (cls == JFArray.class) {
      return TYPE_STRUCT;
    }
    if (cls == String[].class) {
      return TYPE_ARRAY_STRING;
    }
    if (cls == JFObjectPath[].class) {
      return TYPE_ARRAY_OBJECT_PATH;
    }
    if (cls == JFVariant[].class) {
      return TYPE_ARRAY_VARIANT;
    }
    if (cls == JFDictionary.class) {
      return TYPE_ARRAY_DICT;
    }
    if (cls == JFArray[].class) {
      return TYPE_ARRAY_STRUCT;
    }
    JFLog.log("DBus.getClassType() : Unknown Class : " + cls);
    return null;
  }

  private Class<?> getType(String sign) {
    switch (sign) {
      case TYPE_UINT8:
        return Byte.class;
      case TYPE_INT16:
        return Short.class;
      case TYPE_UINT16:
        return UShort.class;
      case TYPE_INT32:
        return Integer.class;
      case TYPE_UINT32:
        return UInteger.class;
      case TYPE_INT64:
        return Long.class;
      case TYPE_UINT64:
        return ULong.class;
      case TYPE_DOUBLE:
        return Double.class;
      case TYPE_BOOLEAN:
        return Boolean.class;
      case TYPE_STRING:
        return String.class;
      case TYPE_OBJECT_PATH:
        return JFObjectPath.class;
      case TYPE_VARIANT:
        return JFVariant.class;
      case TYPE_DICT:
        return JFTuple.class;
      case TYPE_STRUCT:
        return JFArray.class;
      case TYPE_ARRAY_UINT8:
        return byte[].class;
      case TYPE_ARRAY_INT16:
        return short[].class;
      case TYPE_ARRAY_UINT16:
        return UShort[].class;
      case TYPE_ARRAY_INT32:
        return int[].class;
      case TYPE_ARRAY_UINT32:
        return UInteger[].class;
      case TYPE_ARRAY_INT64:
        return long[].class;
      case TYPE_ARRAY_UINT64:
        return ULong[].class;
      case TYPE_ARRAY_DOUBLE:
        return double[].class;
      case TYPE_ARRAY_STRING:
        return String[].class;
      case TYPE_ARRAY_OBJECT_PATH:
        return JFObjectPath[].class;
      case TYPE_ARRAY_VARIANT:
        return JFVariant[].class;
      case TYPE_ARRAY_DICT:
        return JFDictionary.class;
      case TYPE_ARRAY_STRUCT:
        return JFArray[].class;
    }
    JFLog.log("DBus:Error:Type unknown:" + sign);
    return null;
  }

  private static int tcp_port = -1;

  private EndPoint ep;
  private Reader reader;
  private DBusTransport transport;
  private int timeout = 30 * 1000;
  private int serial = 1;
  private Object serial_lock = new Object();
  private ThreadQueue queue;
  private Options options;

  /** Options provides increase control of DBus operations. */
  public static class Options {
    /** Transport to use (null = create default for OS) */
    public DBusTransport transport = null;
    /** min threads for processing inbound calls queue. Default = 1 */
    public int minThreads = 1;
    /** max threads for processing inbound calls queue. Default = 16 */
    public int maxThreads = 16;
    /** idle time before an idle thread is closed (if # threads is more than minThreads). Default = 60 */
    public int idleSeconds = 60;
  }

  /** Create DBus with specified EndPoint. */
  public DBus(EndPoint ep) {
    this.ep = ep;
    this.transport = createTransport();
    this.options = new Options();
  }

  /** Create DBus with specified EndPoint and transport. */
  public DBus(EndPoint ep, DBusTransport transport) {
    this.ep = ep;
    this.transport = transport;
    this.options = new Options();
  }

  /** Create DBus with specified EndPoint and more detailed options. */
  public DBus(EndPoint ep, Options options) {
    this.ep = ep;
    if (options.transport == null) {
      this.transport = createTransport();
    } else {
      this.transport = options.transport;
    }
    this.options = options;
  }

  /** Create transport suitable for OS. */
  public static DBusTransport createTransport() {
    if (JF.isUnix()) {
      if (JF.isMac() && tcp_port != -1) {
        return new TCPTransport(tcp_port);
      }
      return new UnixSocketTransport();
    } else {
      //Windows Pipes requires native support
      if (!JF.hasNativeSupport() && tcp_port != -1) {
        return new TCPTransport(tcp_port);
      }
      return new WinPipeTransport();
    }
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

  /** Set TCP port for systems that do not fully support DBus.
   *
   * Examples:
   *  -  Windows without native Windows Pipes support.
   *  -  MacOS
   */
  public static void setTCPTransportPort(int port) {
    tcp_port = port;
  }

  /** Connects to message bus. */
  public boolean connect() {
    String busname = ep.getEndPointName();
    if (debug) JFLog.log("DBus:busName=" + busname);
    if (queue != null) {
      queue.close();
      queue = null;
    }
    queue = new ThreadQueue(options.minThreads, options.maxThreads, options.idleSeconds);
    return transport.connect(busname, this, new Runnable() {
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
      if (queue != null) {
        queue.close();
        queue = null;
      }
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

  /** Returns bus name requested or assigned. */
  public String getBusName() {
    return transport.getBusName();
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
    public String return_signal;  //return signal
    public boolean nothing;
    public Object value;  //return value
    public Object error;  //return error
    public Object lock = new Object();  //timeout/notify lock
  }

  private Object invokes_lock = new Object();
  private ArrayList<Invoke> invokes = new ArrayList<>();

  /** Invokes method in remote end point.
   * @param dest = dot destination end point on bus
   * @param path = slash path within service
   * @param iface = dot interface name (optional if unique)
   * @param method = method to invoke
   * @param args = arguments
   * @return return value from remote method
   * @exception Exception thrown if method returned at error message or no reply within timeout duration
   *   Errors could be method not found, mismatch arguments, etc.
   */
  public Object invoke(String dest, String path, String iface, String method, Object... args) throws Exception {
    if (debug) JFLog.log("DBus.invoke:" + dest + "." + method);
    Invoke invoke = new Invoke();
    invoke.serial = nextSerial();
    if (dest.equals(DBUS_MESSAGE_BUS)) {
      if (method.equals("RequestName")) {
        invoke.return_signal = "NameAcquired";
      }
    }
    synchronized (invoke.lock) {
      synchronized (invokes_lock) {
        write_msg(MSG_CALL, dest, path, iface, invoke.serial, -1, method, args);
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
      if (invoke.nothing) {
        return null;
      }
      if (invoke.value == null) {
        throw new Exception("DBus.timeout");
      }
      return invoke.value;
    }
  }

  /** Invokes method in remote end point.
   * @param dest = dot destination end point on bus
   * @param method = method to invoke
   * @param args = arguments
   * @return return value from remote method
   * @exception Exception thrown if method returned at error message or no reply within timeout duration
   *   Errors could be method not found, mismatch arguments, etc.
   * Path and iface are inferred from dest.
   */
  public Object invoke(String dest, String method, Object... args) throws Exception {
    boolean dest_generic = dest.startsWith(":");
    String iface = dest_generic ? "javaforce.endpoint" : dest;
    String path = nameToPath(iface);
    return invoke(dest, path, iface, method, args);
  }

  /** Invokes a method in all bus members that have subscribed to the method.
   *
   * @see subscribe
   * @see unsubscribe
   */
  public boolean signal(String path, String iface, String method, Object... args) {
    try {
      write_msg(MSG_SIGNAL, DBUS_MESSAGE_BUS, path, iface, nextSerial(), -1, method, args);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Subscribe to a signal from another client.
   * @param rule = dbus rule (command separated list of key/value pairs)
   *   type='signal'
   *   sender='javaforce.originator'
   *   path='/javaforce/originator'
   *   interface='javaforce.originator'
   *   member='method_name'
   *   destination='javaforce.recipient'
   *   arg0,...='string_value'
   * Typical rule = "type='signal', interface='javaforce.originator', member='Event'"
   */
  public boolean subscribe(String rule) {
    try {
      invoke(DBUS_MESSAGE_BUS, "AddMatch", rule);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Unsubscribe to a signal from another client. */
  public boolean unsubscribe(String rule) {
    try {
      invoke(DBUS_MESSAGE_BUS, "RemoveMatch", rule);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
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

  private int read(byte[] data, int offset, int length) {
    return transport.read(data, offset, length);
  }

  int bodyLength;
  /** Align buffer position to data type size. */
  private void balign(int size) {
    int align = bodyLength & (size-1);
    if (align == 0) return;
    int pad = size - align;
    bodyLength += pad;
  }

  /** Generates method signature. */
  private String gen_sign(Object[] args) {
    int argsLength = args.length;
    StringBuilder sign = new StringBuilder();
    for (int a = 0; a < argsLength; a++) {
      String dt = getObjectType(args[a]);
      switch (dt) {
        case TYPE_STRUCT: {
          JFArray arr = (JFArray)args[a];
          Object[] objs = arr.toArray();
          sign.append("(");
          sign.append(objs);
          sign.append(")");
          break;
        }
        case TYPE_DICT: {
          JFTuple tuple = (JFTuple)args[a];
          sign.append("{");
          sign.append(getClassType(tuple.key_type));
          sign.append(getClassType(tuple.value_type));
          sign.append("}");
          break;
        }
        case TYPE_ARRAY_DICT: {
          JFDictionary map = (JFDictionary)args[a];
          sign.append("a");
          sign.append("{");
          sign.append(getClassType(map.key_type));
          sign.append(getClassType(map.value_type));
          sign.append("}");
          break;
        }
        case TYPE_ARRAY_STRUCT: {
          JFTuple[] tuples = (JFTuple[])args[a];
          sign.append("a");
          sign.append(gen_sign(new Object[] {tuples[0]}));
          break;
        }
        default:
          sign.append(dt);
          break;
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
  private void write_short(short value) throws Exception {
    walign(2);
    wcheck(2);
    LE.setuint16(wpkt, wpos, value);
    wpos += 2;
  }
  private void write_ushort(UShort value) throws Exception {
    walign(2);
    wcheck(2);
    LE.setuint16(wpkt, wpos, value.getValue());
    wpos += 2;
  }
  private void write_int(int value) throws Exception {
    walign(4);
    wcheck(4);
    LE.setuint32(wpkt, wpos, value);
    wpos += 4;
  }
  private void write_uint(UInteger value) throws Exception {
    walign(4);
    wcheck(4);
    LE.setuint32(wpkt, wpos, value.getValue());
    wpos += 4;
  }
  private void write_long(long value) throws Exception {
    walign(8);
    wcheck(8);
    LE.setuint64(wpkt, wpos, value);
    wpos += 8;
  }
  private void write_ulong(ULong value) throws Exception {
    walign(8);
    wcheck(8);
    LE.setuint64(wpkt, wpos, value.getValue());
    wpos += 8;
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
  private void write_variant(JFVariant value) throws Exception {
    String dt = getObjectType(value.value);
    write_sign(dt);
    write_type(value.value);
  }
  private void write_dict(JFTuple value) throws Exception {
    walign(8);
    write_type(value.key);
    write_type(value.value);
  }
  private void write_struct(JFArray value) throws Exception {
    walign(8);
    Object[] arr = value.toArray();
    for(Object obj : arr) {
      write_type(obj);
    }
  }
  private void write_array_byte(byte[] value) throws Exception {
    write_int(value.length);
    for(byte b : value) {
      write_byte(b);
    }
  }
  private void write_array_short(short[] value) throws Exception {
    write_int(value.length * 2);
    for(short b : value) {
      write_short(b);
    }
  }
  private void write_array_ushort(UShort[] value) throws Exception {
    write_int(value.length * 2);
    for(UShort b : value) {
      write_ushort(b);
    }
  }
  private void write_array_int(int[] value) throws Exception {
    write_int(value.length * 4);
    for(int b : value) {
      write_int(b);
    }
  }
  private void write_array_uint(UInteger[] value) throws Exception {
    write_int(value.length * 4);
    for(UInteger b : value) {
      write_uint(b);
    }
  }
  private void write_array_long(long[] value) throws Exception {
    write_int(value.length * 8);
    for(long b : value) {
      write_long(b);
    }
  }
  private void write_array_ulong(ULong[] value) throws Exception {
    write_int(value.length * 8);
    for(ULong b : value) {
      write_ulong(b);
    }
  }
  private void write_array_double(double[] value) throws Exception {
    write_int(value.length * 8);
    for(double b : value) {
      write_double(b);
    }
  }
  private void write_array_boolean(boolean[] value) throws Exception {
    write_int(value.length * 4);
    for(boolean b : value) {
      write_boolean(b);
    }
  }
  private void write_array_String(String[] value) throws Exception {
    int len = 0;
    for(String b : value) {
      {
        //align int
        int align = len & 3;
        if (align > 0) {
          int pad = 4 - align;
          len += pad;
        }
      }
      len += 4;  //String length
      len += b.length();  //String
      len++;  //null
    }
    write_int(len);
    for(String b : value) {
      write_String(b);
    }
  }
  @SuppressWarnings("unchecked")
  private void write_array_dict(JFDictionary value) throws Exception {
    write_int(-1);  //array size (patch later)
    int array_offset = wpos - 4;
    walign(8);
    int array_start = wpos;
    Object[] keys = value.map.keySet().toArray(new String[0]);
    JFTuple tuple = new JFTuple(value.key_type, value.value_type);
    for(Object key : keys) {
      tuple.key = key;
      tuple.value = value.map.get(key);
      write_dict(tuple);
    }
    //patch array size
    int array_size = wpos - array_start;
    LE.setuint32(wpkt, array_offset, array_size);
  }
  private void write_array_struct(JFArray[] value) throws Exception {
    write_int(-1);  //array size (patch later)
    int array_offset = wpos - 4;
    walign(8);
    int array_start = wpos;
    for(JFArray arr : value) {
      write_struct(arr);
    }
    //patch array size
    int array_size = wpos - array_start;
    LE.setuint32(wpkt, array_offset, array_size);
  }
  private void write_array_variant(JFVariant[] value) throws Exception {
    write_int(-1);  //array size (patch later)
    int array_offset = wpos - 4;
    //NOTE : Variants start with sign which has 1 byte alignment
    int array_start = wpos;
    for(JFVariant arr : value) {
      write_variant(arr);
    }
    //patch array size
    int array_size = wpos - array_start;
    LE.setuint32(wpkt, array_offset, array_size);
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

  @SuppressWarnings("unchecked")
  private void write_type(Object obj) throws Exception {
    String dt = getObjectType(obj);
    switch (dt) {
      case TYPE_UINT8:
        write_byte((byte)obj);
        break;
      case TYPE_INT16:
        write_short((short)obj);
        break;
      case TYPE_UINT16:
        UShort ushort = (UShort)obj;
        write_short(ushort.getValue());
        break;
      case TYPE_INT32:
        write_int((int)obj);
        break;
      case TYPE_UINT32:
        UInteger uint = (UInteger)obj;
        write_int(uint.getValue());
        break;
      case TYPE_INT64:
        write_long((long)obj);
        break;
      case TYPE_UINT64:
        ULong ulong = (ULong)obj;
        write_long(ulong.getValue());
        break;
      case TYPE_DOUBLE:
        write_double((double)obj);
        break;
      case TYPE_BOOLEAN:
        write_boolean((boolean)obj);
        break;
      case TYPE_STRING:
        write_String((String)obj);
        break;
      case TYPE_OBJECT_PATH:
        write_String(((JFObjectPath)obj).value);
        break;
      case TYPE_VARIANT:
        write_variant((JFVariant)obj);
        break;
      case TYPE_DICT:
        write_dict((JFTuple)obj);
        break;
      case TYPE_STRUCT:
        write_struct((JFArray)obj);
        break;
      case TYPE_ARRAY_UINT8:
        byte[] ay = (byte[])obj;
        write_array_byte(ay);
        break;
      case TYPE_ARRAY_INT16:
        short[] an = (short[])obj;
        write_array_short(an);
        break;
      case TYPE_ARRAY_UINT16:
        UShort[] aq = (UShort[])obj;
        write_array_ushort(aq);
        break;
      case TYPE_ARRAY_INT32:
        int[] ai = (int[])obj;
        write_array_int(ai);
        break;
      case TYPE_ARRAY_UINT32:
        UInteger[] au = (UInteger[])obj;
        write_array_uint(au);
        break;
      case TYPE_ARRAY_INT64:
        long[] ax = (long[])obj;
        write_array_long(ax);
        break;
      case TYPE_ARRAY_UINT64:
        ULong[] at = (ULong[])obj;
        write_array_ulong(at);
        break;
      case TYPE_ARRAY_DOUBLE:
        double[] ad = (double[])obj;
        write_array_double(ad);
        break;
      case TYPE_ARRAY_BOOLEAN:
        boolean[] ab = (boolean[])obj;
        write_array_boolean(ab);
        break;
      case TYPE_ARRAY_STRING:
        String[] as = (String[])obj;
        write_array_String(as);
        break;
      case TYPE_ARRAY_OBJECT_PATH:
        JFObjectPath[] ao = (JFObjectPath[])obj;
        String[] aostrs = new String[ao.length];
        int idx = 0;
        for(JFObjectPath o : ao) {
          aostrs[idx++] = o.value;
        }
        write_array_String(aostrs);
        break;
      case TYPE_ARRAY_DICT:
        write_array_dict((JFDictionary)obj);
        break;
      case TYPE_ARRAY_STRUCT:
        write_array_struct((JFArray[])obj);
        break;
      case TYPE_ARRAY_VARIANT:
        write_array_variant((JFVariant[])obj);
        break;
      default: {
        throw new Exception("DBus:Error:Unknown type:" + obj.getClass());
      }
    }
  }

  private Object write_msg_lock = new Object();

  private static final Object[] empty = new Object[0];

  private void write_msg(byte msg_type, String dest, String path, String iface, int serial, int serial_reply, String member, Object[] args) {
    if (args == null) args = empty;
    if (debug_msg) JFLog.log("DBus.invoke:" + dest + ":" + member + ":" + serial + ":" + serial_reply);
    synchronized (write_msg_lock) {
      boolean write_to_dbus = dest.equals(DBUS_MESSAGE_BUS);
      String sign = gen_sign(args);
      wpos = 0;
      if (args == null) args = new Object[0];

      try {
        write_byte((byte)'l');  //little endian
        write_byte(msg_type);
        write_byte((byte)0);  //flags
        write_byte((byte)1);  //major version

        write_int(-1);  //body_size (patch later)
        int body_offset = wpos - 4;
        if (debug) JFLog.log("write.serial=" + serial);
        write_int(serial);

        //write fields (DEST, METHOD, SIGNATURE, SENDER, [REPLY_SERIAL])
        //each field is a struct so it must be 8 byte aligned
        write_int(-1);  //fields_size (patch later) (excluding padding before body)
        int fields_offset = wpos - 4;
        int fields_start = wpos;
        if (msg_type != MSG_RETURN) {
          //field:obj_path
          walign(8);
          if (debug) JFLog.log("write.field:PATH=" + path);
          write_byte(FIELD_PATH);
          write_sign(TYPE_OBJECT_PATH);
          write_String(path);
        }
        if (msg_type != MSG_RETURN) {
          //field:interface
          walign(8);
          if (debug) JFLog.log("write.field:INTERFACE=" + iface);
          write_byte(FIELD_INTERFACE);
          write_sign(TYPE_STRING);
          write_String(iface);
        }
        //field:member
        walign(8);
        if (debug) JFLog.log("write.field:MEMBER=" + member);
        write_byte(FIELD_MEMBER);
        write_sign(TYPE_STRING);
        write_String(member);
        if (args.length > 0) {
          //field:signature
          walign(8);
          if (debug) JFLog.log("write.field:SIGNATURE=" + sign);
          write_byte(FIELD_SIGNATURE);
          write_sign(TYPE_SIGNATURE);
          write_sign(sign);
        }
        if (!write_to_dbus) {
          //field:sender
          walign(8);
          if (debug) JFLog.log("write.field:SENDER=" + transport.getBusName());
          write_byte(FIELD_SENDER);
          write_sign(TYPE_STRING);
          write_String(transport.getBusName());
        }
        if (msg_type != MSG_SIGNAL) {
          //field:dest
          walign(8);
          if (debug) JFLog.log("write.field:DEST=" + dest);
          write_byte(FIELD_DEST);
          write_sign(TYPE_STRING);
          write_String(dest);
          if (msg_type == MSG_RETURN) {
            //field:reply_serial
            walign(8);
            if (debug) JFLog.log("write.field:REPLY_SERIAL=" + serial_reply);
            write_byte(FIELD_REPLY_SERIAL);
            write_sign(TYPE_UINT32);
            write_int(serial_reply);
          }
        }

        //patch fields size (excluding end of header padding to 8 bytes)
        int fields_size = wpos - fields_start;
        if (debug) {
          JFLog.log("fields_size=" + fields_size);
        }
        LE.setuint32(wpkt, fields_offset, fields_size);

        walign(8);  //end of header padding to 8 bytes

        //write args (body)
        int body_start = wpos;
        for(Object obj : args) {
          write_type(obj);
        }

        //patch body size
        int body_size = wpos - body_start;
        if (debug) {
          JFLog.log("body_size=" + body_size);
        }
        LE.setuint32(wpkt, body_offset, body_size);

        //write packet
        write(dest, wpkt, 0, wpos);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  private void write_msg(byte msg_type, String dest, int serial, int serial_reply, String member, Object[] args) {
    boolean dest_generic = dest.startsWith(":");
    String iface = dest_generic ? "javaforce.endpoint" : dest;
    String path = nameToPath(iface);
    write_msg(msg_type, dest, path, iface, serial, serial_reply, member, args);
  }

  private class Reader extends Thread {
    byte[] rpkt = new byte[1024];
    int rpos = 0;
    int rpkt_len;
    boolean le;
    byte msg_type;
    byte msg_flags;
    byte msg_ver;
    int msg_body_length;
    int msg_serial;
    int field_size;
    ArrayList<Field> fields = new ArrayList<>();
    public void run() {
      while (transport.isAlive()) {
        if (debug_reading) {
          JFLog.log("DBus.reading...");
        }
        rpkt_len = read(rpkt, 0, 16);
        if (debug_reading) {
          JFLog.log("DBus.read.length=" + rpkt_len);
        }
        if (rpkt_len < 0) {
          JFLog.log("DBus.Reader:read error");
          disconnect();
          break;
        }
        if (rpkt_len == 0) {
          JF.sleep(100);
          continue;
        }
        if (rpkt_len != 16) {
          JFLog.log("DBus.Reader:read header error");
          disconnect();
          break;
        }
        rpos = 0;
        fields.clear();
        if (debug) {
          JFLog.log("packet.header=", rpkt, 0, 16);
        }
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
          msg_body_length = read_int();
          msg_serial = read_int();
          field_size = read_int();

          int length = field_size;
          //add padding to 8 bytes
          int padding = (8 - (length % 8)) & 0x7;
          length += padding;
          //add body length
          length += msg_body_length;

          if (length > max_packet_size) {
            JFLog.log("DBus.Reader:packet too large");
            disconnect();
            break;
          }
          while (rpkt.length < length) {
            rpkt = new byte[rpkt.length << 1];
          }
          rpkt_len = 0;
          int left = length;
          while (rpkt_len != length) {
            if (debug_reading) {
              JFLog.log("DBus.reading:" + left);
            }
            int read = read(rpkt, rpkt_len, left);
            if (debug_reading) {
              JFLog.log("DBus.read.length=" + read);
            }
            if (read == 0) {
              JF.sleep(100);
              continue;
            }
            if (read < 0) {
              JFLog.log("DBus.Reader:read error");
              disconnect();
              break;
            }
            if (read > left) {
              read = left;
            }
            if (read > 0) {
              rpkt_len += read;
              left -= read;
            }
          }
          transport.reconnect();
          rpos = 0;
          if (debug) {
            JFLog.log("packet.fields+body=", rpkt, 0, rpkt_len);
          }
          if (debug) {
            JFLog.log("msg:" + msg_names[msg_type]);
            JFLog.log("field_size=" + field_size);
          }
          int field_end = rpos + field_size;
          while (rpos < field_end) {
            Field field = new Field();
            fields.add(field);
            ralign(8);  //each field is a struct that is aligned to 8 bytes
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
                if (debug) JFLog.log("field.sign@" + rpos);
                field.idx = rpos;
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
              method_return(msg_type);
              break;
            case MSG_ERROR:
              method_error();
              break;
            case MSG_SIGNAL:
              method_return(msg_type);
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
      int signidx = -1;
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
            signidx = field.idx;
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
      if (debug_msg) JFLog.log("DBus.method_call:" + member);
      //get args using signature and body
      Object[] args = null;
      if (signidx != -1) {
        Index idx = new Index();
        idx.idx = signidx + 1;
        args = read_args(idx);
      } else {
        args = new Object[0];
      }
      try {
        //to avoid deadlock this must be done on a seperate thread
        String _member = member;
        String _sender = sender;
        int _msg_serial = msg_serial;  //field value may change with next inbound msg
        Object[] _args = args;
        queue.add(
          new Runnable() {
            public void run() {
              try {
                Object ret = ep.dispatch(_member, _args);
                if (ret == null) throw new Exception("null");
                write_msg(MSG_RETURN, _sender, nextSerial(), _msg_serial, _member, new Object[] {ret});
              } catch (Exception e) {
                if (debug) JFLog.log(e);
                write_msg(MSG_ERROR, _sender, nextSerial(), _msg_serial, _member, new Object[] {e.toString()});
              }
            }
          }
        );
      } catch (Exception e) {
        if (debug) JFLog.log(e);
        write_msg(MSG_ERROR, sender, nextSerial(), msg_serial, member, new Object[] {e.toString()});
      }
    }
    private void method_return(byte msg_type) throws Exception {
      String path = null;
      String sender = null;
      String member = null;
      int signidx = -1;
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
            signidx = field.idx;
            break;
          case FIELD_SENDER:
            sender = (String)field.value;
            break;
          case FIELD_REPLY_SERIAL:
            reply_serial = (Integer)field.value;
            break;
        }
      }
      if (debug_msg) JFLog.log("DBus.method_return:" + member + ":" + reply_serial);
      Object[] args = null;
      if (signidx != -1) {
        Index idx = new Index();
        idx.idx = signidx + 1;
        args = read_args(idx);
      }
      synchronized (invokes_lock) {
        switch (msg_type) {
          case MSG_RETURN: {
            for(Invoke invoke : invokes) {
              if (invoke.serial == reply_serial) {
                if (args == null) {
                  invoke.nothing = true;
                } else {
                  invoke.value = args[0];
                }
                synchronized (invoke.lock) {
                  invoke.lock.notify();
                }
                return;
              }
            }
            break;
          }
          case MSG_SIGNAL: {
            for(Invoke invoke : invokes) {
              if (invoke.return_signal == null) continue;
              if (invoke.return_signal.equals(member)) {
                if (args == null) {
                  invoke.nothing = true;
                } else {
                  invoke.value = args[0];
                }
                synchronized (invoke.lock) {
                  invoke.lock.notify();
                }
                return;
              }
            }
            break;
          }
        }
      }
      JFLog.log("DBus:Warning:msg_return():no pending invoke found");
    }
    private void method_error() throws Exception {
      String path = null;
      String sender = null;
      String member = null;
      int signidx = -1;
      String error = null;
      int reply_serial = -1;
      int cnt = fields.size();
      for(int a=0;a<cnt;a++) {
        Field field = fields.get(a);
        switch (field.type) {
          case FIELD_ERROR_MSG:
            error = (String)field.value;
            break;
          case FIELD_MEMBER:
            member = (String)field.value;
            break;
          case FIELD_PATH:
            path = (String)field.value;
            break;
          case FIELD_SIGNATURE:
            signidx = field.idx;
            break;
          case FIELD_SENDER:
            sender = (String)field.value;
            break;
          case FIELD_REPLY_SERIAL:
            reply_serial = (Integer)field.value;
            break;
        }
      }
      if (debug_msg) JFLog.log("DBus.method_error:" + member + ":" + reply_serial);
      Object[] args = null;
      if (signidx != -1) {
        Index idx = new Index();
        idx.idx = signidx + 1;
        args = read_args(idx);
      } else if (error != null) {
        args = new String[] {error};
      } else {
        args = new String[] {"Unknown Error"};
      }
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
    private Type read_type(Index sign) throws Exception {
      //read one complete type from Sign
      Type stype = new Type();
      stype.idx = sign.idx;
      stype.length = 0;
      boolean done = false;
      int dict = 0;
      int struct = 0;
      do {
        byte type = rpkt[sign.idx++];
        stype.length++;
        switch (type) {
          case TYPE__ARRAY:
            continue;
          case TYPE__DICT_OPEN:
            dict++;
          case TYPE__DICT_CLOSE:
            if (dict == 0) throw new Exception("unexpected dict close");
            dict--;
          case TYPE__STRUCT_OPEN:
            struct++;
          case TYPE__STRUCT_CLOSE:
            if (struct == 0) throw new Exception("unexpected struct close");
            struct--;
        }
        if (dict == 0 && struct == 0) {
          done = true;
        }
      } while (!done);
      return stype;
    }
    private Object[] read_args(Index sign) throws Exception {
      //get args using signature and body
      int start = sign.idx;
      int end = start;
      while (rpkt[end] != 0) {
        end++;
      }
      if (debug) {
        String str = new String(rpkt, start, end - start);
        JFLog.log("read_args:sign=" + str + "@" + start + ":body=" + rpos);
      }
      ArrayList<Object> args = new ArrayList<>();
      while (rpkt[sign.idx] != 0) {
        Object obj = read_arg(sign);
        args.add(obj);
      }
      return args.toArray();
    }
    private Object read_arg(Index sign) throws Exception {
      int start = sign.idx;
      Type dict_key = null;
      Type dict_value = null;
      byte type = rpkt[sign.idx++];
      StringBuilder typestr = new StringBuilder();
      typestr.append((char)type);
      if (type == TYPE__ARRAY) {
        type = rpkt[sign.idx++];
        switch (type) {
          case TYPE__DICT_OPEN:
            type = TYPE__DICT;
            break;
          case TYPE__STRUCT_OPEN:
            type = TYPE__STRUCT;
            break;
          case 0:
            throw new Exception("DBus:read_arg():invalid sign");
        }
        typestr.append((char)type);
      }
      String str = typestr.toString();
      if (debug) JFLog.log("read_arg:sign=" + str + "@" + start + ":body=" + rpos);
      if (str.length() == 0) throw new Exception("DBus:read_arg():error:zero length sign");
      Object arg;
      switch (str) {
        case TYPE_UINT8: {
          arg = read_byte();
          break;
        }
        case TYPE_UINT16:
        case TYPE_INT16: {
          arg = read_short();
          break;
        }
        case TYPE_UINT32:
        case TYPE_INT32: {
          arg = read_int();
          break;
        }
        case TYPE_UINT64:
        case TYPE_INT64: {
          arg = read_long();
          break;
        }
        case TYPE_DOUBLE: {
          arg = read_double();
          break;
        }
        case TYPE_BOOLEAN: {
          arg = (read_int() == 1);
          break;
        }
        case TYPE_OBJECT_PATH: {
          arg = new JFObjectPath(read_String());
          break;
        }
        case TYPE_STRING: {
          arg = read_String();
          break;
        }
        case TYPE_VARIANT:
          arg = read_variant();
          break;
        case TYPE_DICT: {
          dict_key = read_type(sign);
          dict_value = read_type(sign);
          if (rpkt[sign.idx++] != TYPE_DICT_CLOSE.charAt(0)) throw new Exception("DBus:expected DICT CLOSE");
          arg = read_dict(dict_key, dict_value);
          break;
        }
        case TYPE_STRUCT: {
          int struct_start = sign.idx;
          int depth = 1;
          byte _type = rpkt[sign.idx++];
          do {
            _type = rpkt[sign.idx++];
            switch (_type) {
              case TYPE__STRUCT_OPEN:
                depth++;
                break;
              case TYPE__STRUCT_CLOSE:
                depth--;
                break;
            }
          } while (depth > 0);
          int struct_end = sign.idx - 1;
          rpkt[struct_end] = 0;
          arg = (JFArray)read_struct(struct_start);
          rpkt[struct_end] = ')';
          break;
        }
        case TYPE_ARRAY_UINT8: {
          arg = read_array_byte();
          break;
        }
        case TYPE_ARRAY_INT16: {
          arg = read_array_short();
          break;
        }
        case TYPE_ARRAY_UINT16: {
          arg = read_array_ushort();
          break;
        }
        case TYPE_ARRAY_INT32: {
          arg = read_array_int();
          break;
        }
        case TYPE_ARRAY_UINT32: {
          arg = read_array_uint();
          break;
        }
        case TYPE_ARRAY_INT64: {
          arg = read_array_long();
          break;
        }
        case TYPE_ARRAY_UINT64: {
          arg = read_array_ulong();
          break;
        }
        case TYPE_ARRAY_DOUBLE: {
          arg = read_array_double();
          break;
        }
        case TYPE_ARRAY_BOOLEAN: {
          arg = read_array_boolean();
          break;
        }
        case TYPE_ARRAY_STRING: {
          arg = read_array_String();
          break;
        }
        case TYPE_ARRAY_OBJECT_PATH: {
          String[] strs = read_array_String();
          JFObjectPath[] paths = new JFObjectPath[strs.length];
          int idx = 0;
          for(String o : strs) {
            paths[idx++] = new JFObjectPath(o);
          }
          arg = paths;
          break;
        }
        case TYPE_ARRAY_DICT: {
          dict_key = read_type(sign);
          dict_value = read_type(sign);
          if (rpkt[sign.idx++] != TYPE_DICT_CLOSE.charAt(0)) throw new Exception("DBus:expected DICT CLOSE");
          arg = read_array_dict(dict_key, dict_value);
          break;
        }
        case TYPE_ARRAY_STRUCT: {
          int struct_start = sign.idx;
          int depth = 1;
          byte _type = rpkt[sign.idx++];
          do {
            _type = rpkt[sign.idx++];
            switch (_type) {
              case TYPE__STRUCT_OPEN:
                depth++;
                break;
              case TYPE__STRUCT_CLOSE:
                depth--;
                break;
            }
          } while (depth > 0);
          int struct_end = sign.idx - 1;
          rpkt[struct_end] = 0;
          arg = (JFArray[])read_array_struct(struct_start);
          rpkt[struct_end] = ')';
          break;
        }
        case TYPE_ARRAY_VARIANT: {
          arg = read_array_variant();
          break;
        }
        default: {
          throw new Exception("DBus:Error:Unsupported type:" + str);
        }
      }
      return arg;
    }
    private String getTypeAsString(Type type) {
      return new String(rpkt, type.idx, type.length);
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
    private short read_short() throws Exception {
      ralign(2);
      rcheck(2);
      short value;
      if (le) {
        value = (short)LE.getuint16(rpkt, rpos);
      } else {
        value = (short)BE.getuint16(rpkt, rpos);
      }
      rpos += 2;
      return value;
    }
    private UShort read_ushort() throws Exception {
      short value = read_short();
      return new UShort(value);
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
    private UInteger read_uint() throws Exception {
      int value = read_int();
      return new UInteger(value);
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
    private ULong read_ulong() throws Exception {
      long value = read_long();
      return new ULong(value);
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
      int start = rpos - 4;
      rcheck(strlen + 1);  //+1 for null
      String str = new String(rpkt, rpos, strlen);
      rpos += strlen;
      rpos++;  //null
      if (debug) JFLog.log("read_String:" + str + "@" + start);
      return str;
    }
    @SuppressWarnings("unchecked")
    private JFTuple read_dict(Type K, Type V) throws Exception {
      ralign(8);
      JFTuple pair = new JFTuple(getType(getTypeAsString(K)), getType(getTypeAsString(V)));
      Object key = read_arg(K);
      Object value = read_arg(V);
      pair.key = key;
      pair.value = value;
      return pair;
    }
    private JFArray read_struct(int sidx) throws Exception {
      ralign(8);
      Index sign = new Index();
      sign.idx = sidx;
      Object[] arr = read_args(sign);
      JFArray<Object> struct = new JFArray<Object>(Object.class);
      struct.set(arr, 0);
      return struct;
    }
    @SuppressWarnings("unchecked")
    private JFVariant read_variant() throws Exception {
      Index idx = new Index();
      idx.idx = rpos + 1;
      String vartype = read_sign();
      if (debug) JFLog.log("read_variant<" + vartype);
      JFVariant v = new JFVariant(read_arg(idx));
      if (debug) JFLog.log(">");
      return v;
    }
    private byte[] read_array_byte() throws Exception {
      int len = read_int();
      rcheck(len);
      byte[] data = new byte[len];
      System.arraycopy(rpkt, rpos, data, 0, len);
      rpos += len;
      return data;
    }
    private short[] read_array_short() throws Exception {
      int len = read_int();
      rcheck(len);
      int cnt = len / 2;
      short[] data = new short[cnt];
      for(int i=0;i<cnt;i++) {
        data[i] = read_short();
      }
      return data;
    }
    private UShort[] read_array_ushort() throws Exception {
      int len = read_int();
      rcheck(len);
      int cnt = len / 2;
      UShort[] data = new UShort[cnt];
      for(int i=0;i<cnt;i++) {
        data[i] = read_ushort();
      }
      return data;
    }
    private int[] read_array_int() throws Exception {
      int len = read_int();
      rcheck(len);
      int cnt = len / 4;
      int[] data = new int[cnt];
      for(int i=0;i<cnt;i++) {
        data[i] = read_int();
      }
      return data;
    }
    private UInteger[] read_array_uint() throws Exception {
      int len = read_int();
      rcheck(len);
      int cnt = len / 4;
      UInteger[] data = new UInteger[cnt];
      for(int i=0;i<cnt;i++) {
        data[i] = read_uint();
      }
      return data;
    }
    private long[] read_array_long() throws Exception {
      int len = read_int();
      rcheck(len);
      int cnt = len / 8;
      long[] data = new long[cnt];
      for(int i=0;i<cnt;i++) {
        data[i] = read_long();
      }
      return data;
    }
    private ULong[] read_array_ulong() throws Exception {
      int len = read_int();
      rcheck(len);
      int cnt = len / 8;
      ULong[] data = new ULong[cnt];
      for(int i=0;i<cnt;i++) {
        data[i] = read_ulong();
      }
      return data;
    }
    private double[] read_array_double() throws Exception {
      int len = read_int();
      rcheck(len);
      int cnt = len / 8;
      double[] data = new double[cnt];
      for(int i=0;i<cnt;i++) {
        data[i] = read_double();
      }
      return data;
    }
    private boolean[] read_array_boolean() throws Exception {
      int len = read_int();
      rcheck(len);
      int cnt = len / 4;
      boolean[] data = new boolean[cnt];
      for(int i=0;i<cnt;i++) {
        data[i] = read_int() == 1;
      }
      return data;
    }
    private String[] read_array_String() throws Exception {
      ArrayList<String> array = new ArrayList<>();
      int len = read_int();
      rcheck(len);
      ralign(4);
      int end = rpos + len;
      if (debug) JFLog.log("read_array_String[" + len);
      while (rpos < end) {
        array.add(read_String());
      }
      if (debug) JFLog.log("]");
      return array.toArray(JF.StringArrayType);
    }
    private String read_sign() throws Exception {
      int strlen = read_byte() & 0xff;
      if (strlen == 0) throw new Exception("DBus:Invalid zero-length sign @ " + rpos);
      rcheck(strlen + 1);  //+1 for null
      String str = new String(rpkt, rpos, strlen);
      rpos += strlen;
      rpos++;  //null
      return str;
    }
    @SuppressWarnings("unchecked")
    private JFDictionary read_array_dict(Type K, Type V) throws Exception {
      JFDictionary dict = new JFDictionary<>(getType(getTypeAsString(K)), getType(getTypeAsString(V)));
      int len = read_int();
      int start = rpos - 4;
      int end = rpos + len;
      if (debug) JFLog.log("read_array_dict{" + len + "@" + start);

      while (rpos < end) {
        ralign(8);
        //K = String
        Object key = read_arg(K.clone());
        //V = Variant
        Object value = read_arg(V.clone());
        dict.map.put(key, value);
      }
      if (debug) JFLog.log("}");
      return dict;
    }
    private Object[] read_array_struct(int sidx) throws Exception {
      ArrayList<JFArray> list = new ArrayList<>();
      int len = read_int();
      int start = rpos - 4;
      ralign(8);
      int end = rpos + len;
      if (debug) JFLog.log("read_array_struct(" + len + "@" + start);
      while (rpos < end) {
        JFArray arr = read_struct(sidx);
        list.add(arr);
      }
      if (debug) JFLog.log(")");
      return list.toArray(new JFArray[0]);
    }
    private Object[] read_array_variant() throws Exception {
      ArrayList<JFVariant> list = new ArrayList<>();
      int len = read_int();
      int start = rpos - 4;
      int end = rpos + len;
      if (debug) JFLog.log("read_array_variant<<" + len + "@" + start);
      while (rpos < end) {
        JFVariant v = (JFVariant)read_variant();
        list.add(v);
      }
      if (debug) JFLog.log(">>");
      return list.toArray(new JFVariant[0]);
    }
  }
}
