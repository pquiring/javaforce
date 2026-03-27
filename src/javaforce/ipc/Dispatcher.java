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
      if (args[a] instanceof Integer) {
        types[a] = int.class;  //not the same as Integer.class
      } else if (args[a] instanceof UInteger) {
        types[a] = int.class;  //NOTE:this is the same as 'int' since Java does NOT have a primitive uint type
      } else if (args[a] instanceof Double) {
        types[a] = double.class;  //not the same as Double.class
      } else if (args[a] instanceof Boolean) {
        types[a] = boolean.class;  //not the same as Boolean.class
      } else if (args[a] instanceof String) {
        types[a] = args[a].getClass();  //String.class
      } else {
        JFLog.log("Dispatcher:Error:Unknown type:" + types[a]);
        return null;
      }
    }
    if (debug) JFLog.log("Dispatcher:lookup method");
    Method method = cls.getMethod(method_name, types);
    if (debug) JFLog.log("Dispatcher:invoke method");
    return method.invoke(obj, args);
  }
}
