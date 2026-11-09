package javaforce.controls;

import java.util.*;
import java.net.*;

import javaforce.*;
import javaforce.controls.ab.*;
import javaforce.controls.s7.*;
import javaforce.controls.mod.*;
import javaforce.controls.jfc.*;
import javaforce.controls.plc4j.*;

/** PLC "Factory".
 *
 * PLCFactory manages registering PLCDrivers for the creation of PLC instances.
 *
 * @author pquiring
 */

public class PLCFactory {
  private static int timeout = 3000;
  private static HashMap<String, PLCDriver> drivers = new HashMap<>();
  private static String socks;

  public static boolean debug;

  public final static String PLC_S7 = S7Driver.name;
  public final static String PLC_AB = ABDriver.name;
  public final static String PLC_MB = ModbusDriver.name;
  public final static String PLC_JFC = JFCDriver.name;
  public final static String PLC_PLC4J = PLC4JDriver.name;

  static {
    //register default PLC Drivers
    registerDriver(new ABDriver());
    registerDriver(new S7Driver());
  }

  public static void registerDriver(PLCDriver driver) {
    String name = driver.getName();
    drivers.put(name, driver);
  }

  /** Creates a PLC instance registered to name.
   */
  public static PLC create(String name) {
    if (name == null) return null;
    PLCDriver driver = drivers.get(name);
    if (driver == null) return null;
    return driver.create();
  }

  /** Returns connection/read timeout (ms) (default = 3000) */
  public static int getTimeout() {
    return timeout;
  }

  /** Specify timeout (ms) for socket connect and read operations.
   * @param ms = timeout (default = 3000)
   */
  public static void setTimeout(int ms) {
    if (ms < 1000) ms = 1000;
    if (ms > 30000) ms = 30000;
    timeout = ms;
  }

  public static Socket connect(String host, int port) throws Exception {
    Socket socket;
    if (socks != null) {
      socket = new Socket();
      socket.connect(new InetSocketAddress(socks, 1080), timeout);
      if (!SOCKS.connect(socket, host, port)) {
        throw new Exception("SOCKS connection failed");
      }
    } else {
      socket = new Socket();
      socket.connect(new InetSocketAddress(host, port), timeout);
    }
    socket.setSoTimeout(getTimeout());
    return socket;
  }

  /** Connects to PLCs via a SOCKS4 server.
   * Must call this before calling PLC.connect(String host)
   *
   * @param host = IP4 address of SOCKS4 server.
   */
  public static void setSOCKS(String host) {
    socks = host;
  }

  public static int getDataType(String type) {
    int dt = 0;
    switch (type) {
      case "ANY": dt = TagType.any; break;
      case "INT8": dt = TagType.int8; break;
      case "INT16": dt = TagType.int16; break;
      case "INT32": dt = TagType.int32; break;
      case "INT64": dt = TagType.int64; break;
      case "FLOAT": dt = TagType.float32; break;
      case "DOUBLE": dt = TagType.float64; break;
      case "BOOLEAN": dt = TagType.bit; break;
      default: System.out.println("Error:unknown data type:" + type); break;
    }
    return dt;
  }
}
