package javaforce.gl.model;

import java.io.*;
import javaforce.*;
import javaforce.gl.GL;
import javaforce.gl.GL;
import javaforce.gl.Model;
import javaforce.gl.Model;
import javaforce.gl.Object3;
import javaforce.gl.Object3;
import javaforce.gl.UVMap;
import javaforce.gl.UVMap;

/**
 * JF3D - New format designed for JavaForce
 *   Chunk based format
 *
 * Supports:
 *   - GLModel, GLObject, GLUVMap(s)
 * TODO:
 *   - animation data
 *
 * struct ChunkHeader {
 *   int id;
 *   int len;  //size of data excluding ChunkHeader
 * }
 *
 * Everything is Little Endian (Intel based)
 *
 * @author pquiring
 */

public class ModelJF3D {
  private byte[] data;
  private int datapos;
  private int skip;

  private static boolean debug = false;

  private static final int MAGIC = 0x4433464a;  //'JF3D'
  private static final int VERSION = 0x100;

  private static final int ID_MODEL = 0x010000;
  private static final int ID_OBJECT = 0x020000;
  private static final int ID_UVMAP = 0x030000;
// future reserved
//  private static final int ID_CAMERA = 0x40000;
//  private static final int ID_LIGHT = 0x50000;

  private Model model;
  private Object3 obj;

  public Model load(String filename) {
    try {
      InputStream is = new FileInputStream(filename);
      Model model = loadJF3D(is);
      is.close();
      return model;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  public Model load(InputStream is) {
    try {
      return loadJF3D(is);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  private boolean eof() {
    return datapos >= data.length;
  }
  private int readuint16() {
    int uint16 = LE.getuint16(data, datapos);
    datapos += 2;
    skip -= 2;
    return uint16;
  }
  private int readuint32() {
    int uint32 = LE.getuint32(data, datapos);
    datapos += 4;
    skip -= 4;
    return uint32;
  }
  private float readfloat() {
    return Float.intBitsToFloat(readuint32());
  }
  private String readString() {
    String ret = "";
    char ch;
    while (!eof()) {
      ch = (char)data[datapos++];
      skip--;
      if (ch == 0) break;
      ret += ch;
    }
    return ret;
  }
  private Model loadJF3D(InputStream is) throws Exception {
    datapos = 0;
    data = JF.readAll(is);

    int magic = readuint32();
    if (magic != MAGIC) {
      throw new Exception("GL_JF3D:Not JF3D file");
    }
    int version = readuint32();
    if (version < VERSION) {
      throw new Exception("GL_JF3D:Bad version");
    }

    while (!eof()) {
      int head_id = readuint32();
      int head_len = readuint32();
      skip = head_len;
      int head_ver = head_id & 0xffff;
      head_id &= 0xffff0000;
      switch (head_id) {
        case ID_MODEL:
          if (model != null) {
            throw new Exception("GL_JF3D:Multiple Model chunks found");
          }
          model = new Model();
          int fcnt = readuint32();
          for(int a=0;a<fcnt;a++) {
            String txt = readString();
            model.textures.add(txt);
            if (debug) JFLog.log("JF3D:Texture=" + txt);
          }
          if (head_ver > 0) {
            //future reserved
          }
          break;
        case ID_OBJECT:
          obj = new Object3();
          model.addObject(obj);
          obj.name = readString();
          if (debug) JFLog.log("JF3D:Object=" + obj.name);
          obj.type = readuint32();
          obj.org.x = readfloat();
          obj.org.y = readfloat();
          obj.org.z = readfloat();
          int vcnt = readuint32();  //vertex count
          for(int v=0;v<vcnt;v++) {
            float fx = readfloat();
            float fy = readfloat();
            float fz = readfloat();
            obj.addVertex(new float[] {fx, fy, fz});
            if (debug) JFLog.log("JF3D:Vertex:" + fx + "," + fy + "," + fz);
          }
          int pcnt = readuint32();  //poly count
          switch (obj.type) {
            case GL.GL_TRIANGLES:
              pcnt *= 3;
              break;
            case GL.GL_QUADS:
              pcnt *= 4;
              break;
            default:
              JFLog.log("GL_JF3D:Error Unknown GL Type:" + obj.type);
              return null;
          }
          for(int p=0;p<pcnt;p++) {
            int pt = readuint32();
            if (pt >= vcnt) {
              JFLog.log("Error:Poly includes invalid vertex !!!");
            }
            if (debug) JFLog.log("JF3D:Poly:" + pt);
            obj.addPoly(new int[] {pt});
          }
          break;
        case ID_UVMAP:
          UVMap map = obj.createUVMap();
          map.name = readString();
          map.textureIndex = readuint32();
          int uvcnt = readuint32();
          if (uvcnt != obj.getVertexCount()) {
            JFLog.log("Warning:UVMAP size != vertex count");
          }
          for(int i=0;i<uvcnt;i++) {
            float u = readfloat();
            float v = readfloat();
            map.addText(new float[] {u, v});
            if (debug) JFLog.log("JF3D:UV:" + u + "," + v);
          }
          break;
        default:
          break;
      }
      if (skip > 0) {
        datapos += skip;
      }
    }
    return model;
  }

  public boolean save(Model model, String filename) {
    try {
      FileOutputStream fos = new FileOutputStream(filename);
      saveJF3D(model, fos);
      fos.close();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  public boolean save(Model model, OutputStream os) {
    try {
      saveJF3D(model, os);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private ByteArrayOutputStream baos;
  private byte[] tmp;

  private void writeString(String str) throws Exception {
    baos.write(str.getBytes());
    baos.write(0);
  }

  private void writeuint16(int val) {
    LE.setuint16(tmp, 0, val);
    baos.write(tmp, 0, 2);
  }

  private void writeuint32(int val) {
    LE.setuint32(tmp, 0, val);
    baos.write(tmp, 0, 4);
  }

  private void writefloat(float f) {
    writeuint32(Float.floatToIntBits(f));
  }

  private void saveJF3D(Model model, OutputStream os) throws Exception {
    baos = new ByteArrayOutputStream();
    tmp = new byte[8];
    int size;

    writeuint32(MAGIC);
    writeuint32(VERSION);
    writeuint32(ID_MODEL);
    size = 0;
    int tcnt = model.textures.size();
    for(int a=0;a<tcnt;a++) {
      size += model.textures.get(a).length() + 1;
    }
    writeuint32(size);
    writeuint32(tcnt);
    for(int a=0;a<tcnt;a++) {
      writeString(model.textures.get(a));
    }
    for(int o=0;o<model.ol.size();o++) {
      Object3 obj = model.ol.get(o);
      writeuint32(ID_OBJECT);
      int vcnt = obj.vpl.size();
      int pcnt = obj.vil.size();
      size = obj.name.length() + 1 + 4 + (4*3) + (4 + (vcnt * 4)) + (4 + (pcnt * 4));
      writeuint32(size);
      writeString(obj.name);
      writeuint32(obj.type);
      writefloat(obj.org.x);
      writefloat(obj.org.y);
      writefloat(obj.org.z);
      writeuint32(vcnt / 3);
      float[] xyz = obj.vpl.toArray();
      for(int a=0;a<vcnt;a++) {
        writefloat(xyz[a]);
      }
      switch (obj.type) {
        case GL.GL_TRIANGLES:
          writeuint32(pcnt / 3);
          break;
        case GL.GL_QUADS:
          writeuint32(pcnt / 4);
          break;
      }
      int[] pts = obj.vil.toArray();
      for(int a=0;a<pcnt;a++) {
        writeuint32(pts[a]);
      }
      int maps = obj.maps.size();
      if (maps == 0) {
        JFLog.log("GL_JF3D:Warning:No UVMaps found for object:" + obj.name);
      }
      for(int m=0;m<maps;m++) {
        UVMap map = obj.maps.get(m);
        writeuint32(ID_UVMAP);
        int uvcnt = map.uvl.size();
        size = map.name.length() + 1 + 4 + (4 + (uvcnt * 4));
        writeuint32(size);
        writeString(map.name);
        writeuint32(map.textureIndex);
        writeuint32(uvcnt/2);
        float[] uv = map.uvl.toArray();
        for(int a=0;a<uvcnt;a++) {
          writefloat(uv[a]);
        }
      }
    }
    os.write(baos.toByteArray());
  }
  public static void main(String[] args) {
    ModelJF3D jf3d = new ModelJF3D();
    jf3d.debug = true;
    jf3d.load(args[0]);
  }
}
