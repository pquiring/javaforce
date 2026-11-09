package javaforce.controls.plc4j;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javaforce.*;
import javaforce.controls.*;

import org.apache.plc4x.java.api.*;
import org.apache.plc4x.java.api.messages.*;
import org.apache.plc4x.java.api.model.*;
import org.apache.plc4x.java.api.types.*;
import org.apache.plc4x.java.api.value.*;

/** PLC4J PLC.
 *
 * @author pquiring
 */

public class PLC4JPLC implements PLC {
  private static Object connect_lock = new Object();
  private PlcConnection plcConn;
  private boolean connected;
  private static boolean netty;

  public static boolean debug;
  public Exception lastException;

  static {
    if (!netty) {
      netty = true;
      JFLog.log("Disabling netty unsafe usage!");
      System.setProperty("org.jboss.netty.debug", "true");
      System.setProperty("io.netty.allocator.numDirectArenas", "0");
      System.setProperty("io.netty.noPreferDirect", "true");
      System.setProperty("io.netty.noUnsafe", "true");
    }
  }

  public boolean connect(String host) {
    try {
      plcConn = PlcDriverManager.getDefault().getConnectionFactory().getConnection(host);
      if (plcConn == null) throw new Exception("PlcDriverManager failed to connect");
      connected = true;
      return true;
    } catch (Throwable e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean disconnect() {
    if (plcConn == null) return false;
    try {
      plcConn.close();
      plcConn = null;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return true;
  }

  public boolean isConnected() {
    return plcConn != null;
  }

  public boolean isBE() {
    return true;  //varies
  }

  public boolean keepAlive() {
    return false;
  }

  private static byte[] empty = new byte[0];

  public byte[] readTag(String tag) {
    try {
      PlcReadRequest.Builder reader = plcConn.readRequestBuilder();
      reader.addTagAddress("tag0", tag);
      PlcReadResponse data = reader.build().execute().get(5000, TimeUnit.MILLISECONDS);
      PlcValue value = data.getPlcValue("tag0");
      JFLog.log("value=" + value);
      if (value != null) {
        return value.getRaw();
      } else {
        return empty;
      }
    } catch (Throwable e) {
      JFLog.log(e);
      return null;
    }
  }

  public byte[][] readTags(String[] tags) {
    try {
      PlcReadRequest.Builder reader = plcConn.readRequestBuilder();
      int idx = 0;
      for(String tag : tags) {
        reader.addTagAddress("tag" + idx, tag);
        idx++;
      }
      PlcReadResponse data = reader.build().execute().get(5000, TimeUnit.MILLISECONDS);
      byte[][] ret = new byte[tags.length][];
      idx = 0;
      for(String tag : data.getTagNames()) {
        PlcValue value = data.getPlcValue("tag" + idx);
        if (value != null) {
          ret[idx] = value.getRaw();
        } else {
          ret[idx] = empty;
        }
        idx++;
      }
      return ret;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public boolean writeTag(String tag, byte[] data, int tagType) {
    try {
      PlcWriteRequest.Builder writer = plcConn.writeRequestBuilder();
      writer.addTagAddress("tag0", tag, data);
      PlcWriteResponse reply = writer.build().execute().get(5000, TimeUnit.MILLISECONDS);
      //TODO : validate reply ???
      return true;
    } catch (Throwable e) {
      JFLog.log(e);
      return false;
    }
  }

  public Calendar getClock() {
    return null;
  }

  public boolean setClock(Calendar clock) {
    return false;
  }

  public Exception getException() {
    return lastException;
  }
}
