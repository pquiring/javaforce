package javaforce.io;

/** SerialObject - replacement for java.io.Serializable
 *
 * GraalVM does not support Serialization so a replacement is required.
 *
 * @author pquiring
 */

import java.io.*;

public abstract class SerialObject {
  public static final short javaforce_magic = 0x4a46;  //JavaForce serial object
  public static final short java_magic = (short)0xaced;  //java serializable stream
  //recommended chunk IDs
  public static final short id_end = 0x0000;  //end of Object
  public static final short id_1 = 0x100;  //byte boolean
  public static final short id_2 = 0x200;  //short char
  public static final short id_4 = 0x400;  //int float
  public static final short id_8 = 0x800;  //long double
  public static final short id_len = 0x1000;  //variable length chunk (String, byte[], etc.)
  public static final short id_array = 0x2000;  //array of Objects

  public static boolean isJavaSerialObject(InputStream is) {
    try {
      DataInputStream dis = new DataInputStream(is);
      return dis.readShort() == java_magic;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  public static boolean isJavaSerialObject(String file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      boolean isJava = isJavaSerialObject(fis);
      fis.close();
      return isJava;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public DataInputStream dis;
  public void readInit(DataInputStream dis) {
    this.dis = dis;
  }
  public void readInit(SerialObject so) {
    dis = so.dis;
  }
  public abstract void readObject() throws Exception;
  public byte readByte() throws Exception {
    return dis.readByte();
  }
  public short readShort() throws Exception {
    return dis.readShort();
  }
  public int readInt() throws Exception {
    return dis.readInt();
  }
  public long readLong() throws Exception {
    return dis.readLong();
  }
  public char readChar() throws Exception {
    return dis.readChar();
  }
  public String readString() throws Exception {
    int len = dis.readInt();
    byte bytes[] = new byte[len];
    dis.read(bytes);
    return new String(bytes, "UTF-8");
  }
  public boolean readBoolean() throws Exception {
    return readByte() != 0;
  }
  public float readFloat() throws Exception {
    return dis.readFloat();
  }
  public double readDouble() throws Exception {
    return dis.readDouble();
  }
  public byte[] readBytes() throws Exception {
    int len = readInt();
    byte bytes[] = new byte[len];
    dis.read(bytes);
    return bytes;
  }
  public void skipChunk(short id) throws Exception {
    if (id == id_end) return;
    switch (id & 0xff00) {
      case id_1: readByte(); break;
      case id_2: readShort(); break;
      case id_4: readInt(); break;
      case id_8: readLong(); break;
      case id_len:
        int len = readInt();
        byte data[] = new byte[len];
        dis.read(data);
        break;
      case id_array:
        int cnt = readInt();
        for(int a=0;a<cnt;a++) {
          skipObject();
        }
        break;
    }
  }
  private void skipObject() throws Exception {
    do {
      short id = readShort();
      if (id == id_end) break;
      skipChunk(id);
    } while (true);
  }

  public DataOutputStream dos;
  public void writeInit(DataOutputStream dos) {
    this.dos = dos;
  }
  public void writeInit(SerialObject so) {
    dos = so.dos;
  }
  public abstract void writeObject() throws Exception;
  public void writeByte(byte value) throws Exception {
    dos.writeByte(value);
  }
  public void writeShort(short value) throws Exception {
    dos.writeShort(value);
  }
  public void writeInt(int value) throws Exception {
    dos.writeInt(value);
  }
  public void writeLong(long value) throws Exception {
    dos.writeLong(value);
  }
  public void writeChar(char value) throws Exception {
    dos.writeChar(value);
  }
  public void writeString(String value) throws Exception {
    byte bytes[] = value.getBytes("UTF-8");
    dos.writeInt(bytes.length);
    dos.write(bytes);
  }
  public void writeBoolean(boolean value) throws Exception {
    writeByte((byte)(value ? 1 : 0));
  }
  public void writeFloat(float value) throws Exception {
    dos.writeFloat(value);
  }
  public void writeDouble(double value) throws Exception {
    dos.writeDouble(value);
  }
  public void writeBytes(byte[] bytes) throws Exception {
    writeInt(bytes.length);
    dos.write(bytes);
  }
}
