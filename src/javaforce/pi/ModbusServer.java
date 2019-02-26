package javaforce.pi;

/**
 * Modbus Server for Raspberry PI 2/3
 *
 * Supports:
 *  - coils (digital outputs via GPIO)
 *  - discrete input (digital inputs via GPIO)
 *  - read/write values from i2c devices (int8, int16, int24, int32, int64, float32, float64)
 *
 * Notes:
 *  - input and holding registers are treated as the same
 *  - changes to config require a reboot
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
  public enum IO {input, output, unknown};
  public static String version = "0.2";
  public ServerSocket ss;
  //NOTE:Everything is 1 based (zero not used)
  public static int port = 502;
  public static int outs[] = new int[40];
  public static boolean coils[] = new boolean[40];
  public static int ins[] = new int[40];
  public static boolean invert = false;
  public static ArrayList<I2C_I> i2cins = new ArrayList<>();
  public static ArrayList<I2C_O> i2couts = new ArrayList<>();
  public static Object i2cslaveaddrlock = new Object();
  public void run() {
    JFLog.log("jfModbusServer/" + version);
    //init GPIO
    if (!GPIO.init()) {
      JFLog.log("Failed to init GPIO library");
      return;
    }
    //init I2C
    if (!I2C.init()) {
      JFLog.log("Failed to init I2C library");
      return;
    }
    //read config
    String lns[];
    try {
      FileInputStream fis = new FileInputStream("modbus.cfg");
      lns = new String(JF.readAll(fis)).replaceAll("\r", "").split("\n");
      fis.close();
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    for(int a=0;a<lns.length;a++) {
      String ln = lns[a].toLowerCase().replaceAll(" ", "");
      if (ln.startsWith("gpio:")) {
        //read GPIO config
        String fs[] = ln.substring(5).split(":");
        IO io = IO.unknown;
        int addr = -1;
        int bit = -1;
        for(int b=0;b<fs.length;b++) {
          ln = fs[b];
          int idx = ln.indexOf('=');
          if (idx == -1) {
            switch (ln) {
              case "i":
                io = IO.input;
                break;
              case "o":
                io = IO.output;
                break;
            }
          } else {
            String key = ln.substring(0, idx).trim();
            String value = ln.substring(idx+1).trim();
            switch (key) {
              case "bit": bit = Integer.valueOf(value); break;
              case "addr": addr = Integer.valueOf(value); break;
            }
          }
        }
        if (io == IO.unknown) {
          JFLog.log("Error:Invalid GPIO:I/O not specified");
          System.exit(0);
        }
        if (bit == -1) {
          JFLog.log("Error:Invalid GPIO:bit not specified");
          System.exit(0);
        }
        if (addr == -1) {
          JFLog.log("Error:Invalid GPIO:addr not specified");
          System.exit(0);
        }
        switch (io) {
          case input:
            ins[addr] = bit + 1;
            GPIO.configInput(bit + 1);
            break;
          case output:
            outs[addr] = bit + 1;
            GPIO.configOutput(bit + 1);
            break;
        }
      }
      else if (ln.startsWith("i2c:")) {
        //read I2C config
        String fs[] = ln.substring(4).split(":");
        IO io = IO.unknown;
        Value type = null;
        int addr = -1;  //modbus
        int slaveaddr = -1;  //i2c slave addr
        int avginterval = -1;
        int avgsamples = -1;
        int readBytes[] = null;
        int writeBytes[] = null;
        for(int b=0;b<fs.length;b++) {
          ln = fs[b];
          int idx = ln.indexOf('=');
          if (idx == -1) {
            switch (ln) {
              case "i":
                io = IO.input;
                break;
              case "o":
                io = IO.output;
                break;
            }
          } else {
            String key = ln.substring(0, idx).trim();
            String value = ln.substring(idx+1).trim();
            switch (key) {
              case "addr": addr = Integer.valueOf(value); break;
              case "slaveaddr": addr = Integer.valueOf(value); break;
              case "avg":
                String vs[] = value.split(",");
                if (vs.length != 2) {
                  JFLog.log("Error:I2C avg=requires samples,interval");
                  System.exit(0);
                }
                avgsamples = Integer.valueOf(vs[0].trim());
                if (avgsamples < 10) {
                  avgsamples = 10;
                }
                avginterval = Integer.valueOf(vs[1].trim());
                if (avginterval < 100) {
                  avginterval = 100;
                }
                break;
              case "read":
                readBytes = decodeBytes(value.split(","), io == IO.input);
                break;
              case "write":
                writeBytes = decodeBytes(value.split(","), io == IO.output);
                break;
              case "type":
                //int8 | int16 | int24 | int32 | int64 | float32 | float64
                //NOTE : int24 is read as int32 with extra byte of padding
                switch (value) {
                  case "int8": type = new int8(); break;
                  case "int16": type = new int16(); break;
                  case "int24": type = new int32(); break;
                  case "int32": type = new int32(); break;
                  case "int64": type = new int64(); break;
                  case "float32": type = new float32(); break;
                  case "float64": type = new float64(); break;
                  default: JFLog.log("Error:I2C data type unknown:" + value); System.exit(0);
                }
                break;
            }
          }
        }
        if (io == IO.unknown) {
          JFLog.log("Error:Invalid I2C:I/O not specified");
          System.exit(0);
        }
        if (addr == -1) {
          JFLog.log("Error:Invalid I2C:addr not specified");
          System.exit(0);
        }
        if (slaveaddr == -1) {
          JFLog.log("Error:Invalid I2C:slaveaddr not specified");
          System.exit(0);
        }
        switch (io) {
          case input:
            if (readBytes == null) {
              JFLog.log("Error:Invalid I2C:read not specified");
              System.exit(0);
            }
            I2C_I i = new I2C_I();
            i.addr = addr;
            i.slaveaddr = slaveaddr;
            i.type = type;
            i.typeSize = type.getSize();
            i.writeBytes = writeBytes;
            i.readBytes = readBytes;
            if (avginterval != -1) {
              i.avg = true;
              i.avginterval = avginterval;
              i.avgsamples = avgsamples;
              i.samples = type.newArray(avgsamples);
              i.avg_lock = new Object();
              i.startTimer();
            }
            i2cins.add(i);
            break;
          case output:
            if (writeBytes == null) {
              JFLog.log("Error:Invalid I2C:write not specified");
              System.exit(0);
            }
            I2C_O o = new I2C_O();
            o.addr = addr;
            o.slaveaddr = slaveaddr;
            o.type = type;
            o.typeSize = type.getSize();
            o.readBytes = readBytes;
            o.writeBytes = writeBytes;
            i2couts.add(o);
            break;
        }
      }
      else {
        int idx = ln.indexOf('=');
        if (idx == -1) continue;
        String key = ln.substring(0, idx).trim();
        String value = ln.substring(idx+1).trim();
        switch (key) {
          case "port": port = Integer.valueOf(value); break;
          case "invert": invert = Boolean.valueOf(value); break;
        }
      }
    }
    //open TCP socket
    try {
      ss = new ServerSocket(port);
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

  private static final int CS8flag = 0x100;  //checksum 8bit
  private static final int IOflag = 0x200;  //I/O byte

  //hex values : xx = xx
  //checksum8 : CS8 = 0x100
  //input/output bytes : I/O# = -01 thru -ff
  public int[] decodeBytes(String fs[], boolean allowIO) {
    int ret[] = new int[fs.length];
    for(int a=0;a<fs.length;a++) {
      String f = fs[a].trim();
      if (f.equals("cs8")) {
        ret[a] = CS8flag;
        continue;
      }
      switch (f.charAt(0)) {
        case 'i':  //input byte
          if (!allowIO) {
            JFLog.log("Error:bad I2C");
            System.exit(0);
          }
          ret[a] = IOflag & Integer.valueOf(f.substring(1));
          break;
        case 'o':  //output byte
          if (!allowIO) {
            JFLog.log("Error:bad I2C");
            System.exit(0);
          }
          ret[a] = IOflag & Integer.valueOf(f.substring(1));
          break;
        default:  //hex value
          ret[a] = Integer.valueOf(f, 16) & 0xff;
          break;
      }
    }
    return ret;
  }

  public static byte checksum8(byte data[], int start, int end) {
    byte ret = 0;
    for(int a=start;a<=end;a++) {
      ret += data[a];
    }
    return ret;
  }

  public static class Packet {
    public short id;
    public short res;
    public short length;
    public byte unit;
    public byte func;
    //byte data[] per func
  }

  //I2C Input Mapping
  public static class I2C_I extends TimerTask {
    public int addr;  //modbus register #
    public int slaveaddr;  //slave addr
    public Value type;
    public int typeSize;
    public int[] writeBytes;
    public int[] readBytes;
    public boolean avg;
    public int avginterval;
    public int avgsamples;
    public Value[] samples;
    public Object avg_lock;  //lock to read avg
    private Timer timer;
    public void startTimer() {
      timer = new Timer();
      timer.scheduleAtFixedRate(this, avginterval, avginterval);
    }
    public void run() {
      Value value;
      //read next value
      synchronized(i2cslaveaddrlock) {
        I2C.setSlave(slaveaddr);
        if (writeBytes != null) write();
        value = read();
      }
      //store value in last avg slot
      synchronized(avg_lock) {
        System.arraycopy(samples, 1, samples, 0, avgsamples-1);
        samples[avgsamples-1] = value;
      }
    }
    private void write() {
      byte data[] = new byte[writeBytes.length];
      for(int a=0;a<writeBytes.length;a++) {
        if (writeBytes[a] > 0xff) {
          if (writeBytes[a] == CS8flag) {
            data[a] = checksum8(data, 0, a-1);
          } else {
            //BUG : there should be no I/O values for I2C_I.write()
          }
        } else {
          data[a] = (byte)(writeBytes[a] & 0xff);
        }
      }
      I2C.write(data);
    }
    private Value read() {
      byte data[] = new byte[readBytes.length];
      I2C.read(data);
      Value value = type.newInstance();
      byte vs[] = new byte[value.getSize()];
      for(int a=0;a<readBytes.length;a++) {
        if ((readBytes[a] & IOflag) == IOflag) {
          vs[readBytes[a] & 0xff] = data[a];
        }
      }
      type.set(vs);
      return value;
    }
    public void read(byte data[], int start_addr) {
      Value value;
      if (avg) {
        value = type.newInstance();
        synchronized(avg_lock) {
          for(int a=0;a<avgsamples-1;a++) {
            value.add(samples[a]);
          }
          value.div(avgsamples);
        }
      } else {
        synchronized(i2cslaveaddrlock) {
          I2C.setSlave(slaveaddr);
          write();
          value = read();
        }
      }
      byte vb[] = value.getBytes();
      for(int a=0;a<typeSize;a++) {
        data[(addr - start_addr)*2 + a] = vb[a];
      }
    }
  }

  //I2C Output Mapping
  public static class I2C_O {
    public int addr;  //modbus register # (shorts)
    public int slaveaddr;  //slave addr
    public Value type;
    public int typeSize;
    public int[] readBytes;
    public int[] writeBytes;
    public void write(byte values[], int start_idx, int start_offset) {
      byte data[] = new byte[writeBytes.length];
      for(int a=0;a<writeBytes.length;a++) {
        if (writeBytes[a] > 0xff) {
          if (writeBytes[a] == CS8flag) {
            data[a] = checksum8(data, 0, a-1);
          } else {  //IOflag
            data[a] = values[start_offset + ((addr - start_idx)*2 + (writeBytes[a] & 0xff))];
          }
        } else {
          data[a] = (byte)(writeBytes[a] & 0xff);
        }
      }
      synchronized(i2cslaveaddrlock) {
        I2C.setSlave(slaveaddr);
        I2C.write(data);
      }
    }
  }

  public static abstract class Value {
    public abstract void set(byte data[]);
    public abstract void add(Value v2);
    public abstract void div(int divsor);
    public abstract byte[] getBytes();
    public abstract Value newInstance();
    public abstract int getSize();
    public Value[] newArray(int size) {
      Value ret[] = new Value[size];
      for(int a=0;a<size;a++) {
        ret[a] = newInstance();
      }
      return ret;
    }
  }

  public static class int8 extends Value {
    private byte value;
    public void set(byte data[]) {
      value = data[0];
    }
    public void add(Value o) {
      value += ((int8)o).value;
    }
    public void div(int divsor) {
      value /= divsor;
    }
    public byte[] getBytes() {
      byte[] bytes = new byte[1];
      bytes[0] = value;
      return bytes;
    }
    public Value newInstance() {
      return new int8();
    }
    public int getSize() {
      return 1;
    }
  }

  public static class int16 extends Value {
    private short value;
    public void set(byte data[]) {
      value = (short)BE.getuint16(data, 0);
    }
    public void add(Value o) {
      value += ((int16)o).value;
    }
    public void div(int divsor) {
      value /= divsor;
    }
    public byte[] getBytes() {
      byte[] bytes = new byte[2];
      BE.setuint16(bytes, 0, value);
      return bytes;
    }
    public Value newInstance() {
      return new int16();
    }
    public int getSize() {
      return 2;
    }
  }

  public static class int32 extends Value {
    private int value;
    public void set(byte data[]) {
      value = BE.getuint32(data, 0);
    }
    public void add(Value o) {
      value += ((int32)o).value;
    }
    public void div(int divsor) {
      value /= divsor;
    }
    public byte[] getBytes() {
      byte[] bytes = new byte[4];
      BE.setuint32(bytes, 0, value);
      return bytes;
    }
    public Value newInstance() {
      return new int32();
    }
    public int getSize() {
      return 4;
    }
  }

  public static class int64 extends Value {
    private long value;
    public void set(byte data[]) {
      value = BE.getuint64(data, 0);
    }
    public void add(Value o) {
      value += ((int64)o).value;
    }
    public void div(int divsor) {
      value /= divsor;
    }
    public byte[] getBytes() {
      byte[] bytes = new byte[8];
      BE.setuint64(bytes, 0, value);
      return bytes;
    }
    public Value newInstance() {
      return new int64();
    }
    public int getSize() {
      return 8;
    }
  }

  public static class float32 extends Value {
    private float value;
    public void set(byte data[]) {
      value = Float.intBitsToFloat(BE.getuint32(data, 0));
    }
    public void add(Value o) {
      value += ((float32)o).value;
    }
    public void div(int divsor) {
      value /= divsor;
    }
    public byte[] getBytes() {
      byte[] bytes = new byte[4];
      BE.setuint32(bytes, 0, Float.floatToIntBits(value));
      return bytes;
    }
    public Value newInstance() {
      return new float32();
    }
    public int getSize() {
      return 4;
    }
  }

  public static class float64 extends Value {
    private double value;
    public void set(byte data[]) {
      value = Double.longBitsToDouble(BE.getuint64(data, 0));
    }
    public void add(Value o) {
      value += ((float64)o).value;
    }
    public void div(int divsor) {
      value /= divsor;
    }
    public byte[] getBytes() {
      byte[] bytes = new byte[8];
      BE.setuint64(bytes, 0, Double.doubleToLongBits(value));
      return bytes;
    }
    public Value newInstance() {
      return new float64();
    }
    public int getSize() {
      return 8;
    }
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
            case 3: readMultipleRegisters(); break;  //holding
            case 4: readMultipleRegisters(); break;  //input
            case 5: writeCoilSingle(); break;
            case 6: writeSingleRegister(); break; //holding
            case 15: writeCoilMulti(); break;
            case 16: writeMultipleRegisters(); break; //holding
            default:
              JFLog.log("Unsupported func:" + func);
              data[7] |= 0x80;
              data[8] = 1;  //illegal function
              setLength(1);
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
    /** Sets frame size excluding unit id and func code bytes. */
    public void setLength(int length) {
      this.length = length;
      BE.setuint16(data, 4, 2 + length);
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
      setLength(1 + num_bytes);
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
      setLength(1 + num_bytes);
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
      if (invert) state = !state;
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
        if (invert) state = !state;
        GPIO.write(outs[coil_idx], state);
        coils[coil_idx] = state;
        coil_idx++;
        bitPos <<= 1;
      }
      setLength(4);
    }
    public void readMultipleRegisters() {
      //func 3 or 4
      //request:short start_addr; short num_registers;
      //  reply:byte num_bytes; short[] registers;
      int start_idx = BE.getuint16(data, 8);
      int num_registers = BE.getuint16(data, 10);
      int end_idx = start_idx + num_registers - 1;
      for(int a=0;a<num_registers*2;a++) {
        data[2 + a] = 0;
      }
      for(int a=0;a<i2cins.size();a++) {
        I2C_I i = i2cins.get(a);
        if (i.addr >= start_idx && i.addr <= end_idx) {
          i.read(data, start_idx);
        }
      }
      setLength(1 + num_registers * 2);
    }
    public void writeSingleRegister() {
      //func 6
      //request:short start_addr; short value;
      //  reply:short start_addr; short value;
      int start_idx = BE.getuint16(data, 8);
      int end_idx = start_idx;
      for(int a=0;a<i2couts.size();a++) {
        I2C_O o = i2couts.get(a);
        if (o.addr >= start_idx && o.addr <= end_idx) {
          o.write(data, start_idx, 10);
        }
      }
    }
    public void writeMultipleRegisters() {
      //func 16
      //request:short start_addr; short num_registers; byte num_bytes; short[] new_values;
      //  reply:short start_addr; short num_registers;
      int start_idx = BE.getuint16(data, 8);
      int num_registers = BE.getuint16(data, 10);
      int end_idx = start_idx + num_registers - 1;
      for(int a=0;a<i2couts.size();a++) {
        I2C_O o = i2couts.get(a);
        if (o.addr >= start_idx && o.addr <= end_idx) {
          o.write(data, start_idx, 13);
        }
      }
      setLength(4);
    }
  }
}
