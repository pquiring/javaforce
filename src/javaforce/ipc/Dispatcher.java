package javaforce.ipc;

import java.lang.reflect.*;

import javaforce.*;
import static javaforce.ipc.DBus.*;

/** Dispatcher.
 *
 * This class can dispatch RPC messages to methods in an Object using reflection.
 *
 * @author pquiring
 */

public class Dispatcher {
  private static boolean debug = false;

  private Class<?> cls;
  private Object obj;

  private InterfaceSelector selector;

  /** Create Dispatcher that will dispatch methods in obj. */
  public Dispatcher(Object obj) {
    this.obj = obj;
    cls = obj.getClass();
  }

  /** Create Dispatcher that will dispatch methods based on object and interface selection. */
  public Dispatcher(InterfaceSelector selector) {
    this.selector = selector;
  }

  /** Dispatches a method request and returns the return value from the method. */
  public Object dispatch(String object, String iface_name, String method_name, Object[] args) throws Exception {
    int argsLength = args.length;
    Class[] types = new Class[argsLength];
    for (int a = 0; a < argsLength; a++) {
      String dt = DBus.getObjectType(args[a]);
      switch (dt) {
        case TYPE_UINT8:
          types[a] = byte.class;  //not the same as Byte.class
          break;
        case TYPE_INT16:
          types[a] = short.class;  //not the same as Short.class
          break;
        case TYPE_UINT16:
          types[a] = UShort.class;
          break;
        case TYPE_INT32:
          types[a] = int.class;  //not the same as Integer.class
          break;
        case TYPE_UINT32:
          types[a] = UInteger.class;
          break;
        case TYPE_INT64:
          types[a] = long.class;  //not the same as Integer.class
          break;
        case TYPE_UINT64:
          types[a] = ULong.class;
          break;
        case TYPE_DOUBLE:
          types[a] = double.class;  //not the same as Double.class
          break;
        case TYPE_BOOLEAN:
          types[a] = boolean.class;  //not the same as Boolean.class
          break;
        case TYPE_STRING:
          types[a] = args[a].getClass();
          break;
        case TYPE_VARIANT:
          types[a] = JFVariant.class;
          break;
        case TYPE_DICT:
          types[a] = JFTuple.class;
          break;
        case TYPE_STRUCT:
          types[a] = JFArray.class;
          break;
        case TYPE_ARRAY_UINT8:
          types[a] = byte[].class;
          break;
        case TYPE_ARRAY_INT16:
          types[a] = short[].class;
          break;
        case TYPE_ARRAY_UINT16:
          types[a] = UShort[].class;
          break;
        case TYPE_ARRAY_INT32:
          types[a] = int[].class;
          break;
        case TYPE_ARRAY_UINT32:
          types[a] = UInteger[].class;
          break;
        case TYPE_ARRAY_INT64:
          types[a] = long[].class;
          break;
        case TYPE_ARRAY_UINT64:
          types[a] = ULong[].class;
          break;
        case TYPE_ARRAY_DOUBLE:
          types[a] = double[].class;
          break;
        case TYPE_ARRAY_BOOLEAN:
          types[a] = boolean[].class;
          break;
        case TYPE_ARRAY_STRING:
          types[a] = String[].class;
          break;
        case TYPE_ARRAY_VARIANT:
          types[a] = JFVariant[].class;
          break;
        case TYPE_ARRAY_DICT:
          types[a] = JFDictionary.class;
          break;
        case TYPE_ARRAY_STRUCT:
          types[a] = JFArray[].class;
          break;
        default: {
          JFLog.log("Dispatcher:Error:Unknown type:" + args[a]);
          return null;
        }
      }
    }
    if (selector != null) {
      obj = selector.getInterface(iface_name, object);
      if (obj == null) throw new Exception("Unknown Interface:" + iface_name);
      cls = obj.getClass();
    }
    if (debug) JFLog.log("Dispatcher:lookup method");
    Method method = cls.getMethod(method_name, types);
    if (method == null) {
      JFLog.log("Dispatcher:Error:method not found:" + method_name);
      return null;
    }
    if (debug) JFLog.log("Dispatcher:invoke method");
    if (method.getReturnType() == Void.TYPE) {
      method.invoke(obj, args);
      return null;
    } else {
      return method.invoke(obj, args);
    }
  }
}
