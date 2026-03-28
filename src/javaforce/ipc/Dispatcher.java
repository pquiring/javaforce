package javaforce.ipc;

/** Dispatcher
 *
 * This class can dispatch RPC messages to methods in an Object using reflection.
 *
 * Supported Data Types:
 *   - String, Integer, UInteger, Double, Boolean
 *
 * @author pquiring
 */

import java.lang.reflect.*;

import javaforce.*;
import static javaforce.ipc.DBus.*;

public class Dispatcher {
  private static boolean debug = false;

  private Class<?> cls;
  private Object obj;

  /** Create Dispatcher that will dispatch methods in obj. */
  public Dispatcher(Object obj) {
    this.obj = obj;
    cls = obj.getClass();
  }

  /** Dispatches a method request and returns the return value from the method. */
  public Object dispatch(String method_name, Object[] args) throws Exception {
    int argsLength = args.length;
    Class[] types = new Class[argsLength];
    for (int a = 0; a < argsLength; a++) {
      char dt = DBus.getDataType(args[a]);
      switch (dt) {
        case TYPE_UINT8:
          types[a] = byte.class;  //not the same as Byte.class
          break;
        case TYPE_INT16:
          types[a] = short.class;  //not the same as Short.class
          break;
        case TYPE_UINT16:
          types[a] = short.class;  //NOTE:this is the same as 'short' since Java does NOT have a primitive ushort type
          break;
        case TYPE_INT32:
          types[a] = int.class;  //not the same as Integer.class
          break;
        case TYPE_UINT32:
          types[a] = int.class;  //NOTE:this is the same as 'int' since Java does NOT have a primitive uint type
          break;
        case TYPE_INT64:
          types[a] = long.class;  //not the same as Integer.class
          break;
        case TYPE_UINT64:
          types[a] = long.class;  //NOTE:this is the same as 'long' since Java does NOT have a primitive ulong type
          break;
        case TYPE_DOUBLE:
          types[a] = double.class;  //not the same as Double.class
          break;
        case TYPE_BOOLEAN:
          types[a] = boolean.class;  //not the same as Boolean.class
          break;
        case TYPE_STRING:
          types[a] = args[a].getClass();  //String.class
          break;
        default: {
          JFLog.log("Dispatcher:Error:Unknown type:" + args[a]);
          return null;
        }
      }
    }
    if (debug) JFLog.log("Dispatcher:lookup method");
    Method method = cls.getMethod(method_name, types);
    if (debug) JFLog.log("Dispatcher:invoke method");
    return method.invoke(obj, args);
  }
}
