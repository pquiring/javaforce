package javaforce.pi;

/**
 * Modbus Server for Raspberry PI 2/3
 *
 * Supports : coils (digital outputs) and discrete input (digital inputs) only.
 *
 * @author User
 */

import java.net.*;
import java.io.*;
import java.util.*;

import javaforce.*;

public class ModbusServer extends Thread {
  public static void main(String args[]) {
    new ModbusServer().start();
  }
  public static String version = "0.1";
  public ServerSocket ss;
  //NOTE:Everything is 1 based (zero not used)
  public static int port = 502;
  public static int outs[] = new int[41];
  public static boolean coils[] = new boolean[41];
  public static int ins[] = new int[41];
  public void run() {
    JFLog.log("jfModbusServer/" + version);
    //init GPIO
    if (!GPIO.init()) {
      JFLog.log("Failed to init GPIO library");
      return;
    }
    //read config
    Properties p = new Properties();
    try {
      FileInputStream fis = new FileInputStream("modbus.cfg");
      p.load(fis);
      fis.close();
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    String str;
    str = p.getProperty("port");
    if (str != null) {
      port = JF.atoi(str);
    }
    int inpos = 1;
    int outpos = 1;
    for(int a=1;a<40;a++) {
      str = p.getProperty("GPIO" + a);
      if (str == null) continue;
      if (str.length() == 0) continue;
      char type = str.charAt(0);
      switch (type) {
        case 'O':
        case 'o':
          outs[outpos++] = a;
          GPIO.configOutput(a);
          break;
        case 'I':
        case 'i':
          ins[inpos++] = a;
          GPIO.configInput(a);
          break;
      }
    }
    //open TCP socket
    try {
      ss = new ServerSocket(502);
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    //wait for clients
    while (true) {
      try {
        Socket s = ss.accept();
        Client c = new Client();
        c.s = s;
        c.start();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public static class Packet {
    public short id;
    public short res;
    public short length;
    public byte unit;
    public byte func;
    //byte data[] per func
  }

  //see https://en.wikipedia.org/wiki/Modbus

  public static class Client extends Thread {
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public byte data[] = new byte[1500];
    public int length;
    public void run() {
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        while (s.isConnected()) {
          int pos = 0;
          //read request header (6 bytes)
          while (pos != 6) {
            int read = is.read(data, pos, 6 - pos);
            if (read > 0) {
              pos += read;
            }
          }
          getLength();
          int toRead = length + 6;
          //read the rest of the packet/frame
          while (pos != toRead) {
            int read = is.read(data, pos, toRead - pos);
            if (read > 0) {
              pos += read;
            }
          }
          byte func = data[7];
          switch (func) {
            case 1: readCoils(); break;
            case 2: readDiscreteInputs(); break;
            case 5: writeCoilSingle(); break;
            case 15: writeCoilMulti(); break;
            default:
              JFLog.log("Unsupported func:" + func);
              data[7] |= 0x80;
              setLength(3);
              break;
          }
          reply();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void getLength() {
      length = BE.getuint16(data, 4);
    }
    public void setLength(int length) {
      this.length = length;
      BE.setuint16(data, 4, length);
    }
    public void reply() {
      try {
        os.write(data, 0, 6 + length);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void readCoils() {
      //func : 1
      //request:short start_addr; short num_coils;
      //  reply:byte num_bytes; byte[] coils;
      int coil_idx = BE.getuint16(data, 8);
      int num_coils = BE.getuint16(data, 10);
      int num_bytes = (num_coils + 7) >> 3;
      setLength(3 + num_bytes);
      data[8] = (byte)num_bytes;
      int bytePos = 9;
      int bitPos = 0x01;
      data[bytePos] = 0;
      for(int cnt = 0; cnt < num_coils; cnt++) {
        if (bitPos == 0x100) {
          bitPos = 0x01;
          bytePos++;
          data[bytePos] = 0;
        }
        if (coils[coil_idx++]) {
          data[bytePos] |= bitPos;
        }
        bitPos <<= 1;
      }
    }
    public void readDiscreteInputs() {
      //func : 2
      //request:short start_addr; short num_inputs;
      //  reply:byte num_bytes; byte[] inputs;
      int coil_idx = BE.getuint16(data, 8);
      int num_coils = BE.getuint16(data, 10);
      int num_bytes = (num_coils + 7) >> 3;
      setLength(3 + num_bytes);
      data[8] = (byte)num_bytes;
      int bytePos = 9;
      int bitPos = 0x01;
      data[bytePos] = 0;
      for(int cnt = 0; cnt < num_coils; cnt++) {
        if (bitPos == 0x100) {
          bitPos = 0x01;
          bytePos++;
          data[bytePos] = 0;
        }
        if (GPIO.read(ins[coil_idx++])) {
          data[bytePos] |= bitPos;
        }
        bitPos <<= 1;
      }
    }
    public void writeCoilSingle() {
      //func : 5
      //request:short addr_coil; short value;  //0=off 0xff00=on
      //  reply:same
      int coil = BE.getuint16(data, 8);
      boolean state = BE.getuint16(data, 10) == 0xff00;
      GPIO.write(outs[coil], state);
      coils[coil] = state;
    }
    public void writeCoilMulti() {
      //func : 15
      //request:short start_addr; short num_coils; byte num_bytes; byte[] coils;
      //  reply:short start_addr; short num_coils;
      int coil_idx = BE.getuint16(data, 8);
      int num_coils = BE.getuint16(data, 10);
      int num_bytes = data[12];
      int bytePos = 13;
      int bitPos = 0x01;
      for(int cnt = 0; cnt < num_coils; cnt++) {
        if (bitPos == 0x100) {
          bitPos = 0x01;
          bytePos++;
        }
        boolean state = (data[bytePos] & bitPos) != 0;
        GPIO.write(outs[coil_idx], state);
        coils[coil_idx] = state;
        coil_idx++;
        bitPos <<= 1;
      }
      setLength(6);
    }
  }
}
