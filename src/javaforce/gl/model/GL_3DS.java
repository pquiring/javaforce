package javaforce.gl.model;

import java.io.*;
import java.util.*;
import javaforce.*;
import javaforce.gl.GL;
import javaforce.gl.Model;
import javaforce.gl.Object3;
import javaforce.gl.Rotate3;
import javaforce.gl.Scale3;
import javaforce.gl.Translate3;
import javaforce.gl.UVMap;

/**
 * Autodesk .3DS reader
 *
 * Supports:
 *   - Mesh, UVMap, animation data
 *
 *
 * @author pquiring
 */

public class GL_3DS {
//Loading//
  private static final int _3DS_FLG_TENSION     =  0x01;
  private static final int _3DS_FLG_CONTINUITY  =  0x02;
  private static final int _3DS_FLG_BIAS        =  0x04;
  private static final int _3DS_FLG_EASE_TO     =  0x08;
  private static final int _3DS_FLG_EASE_FROM   =  0x10;
  //*.3DS loading (loades meshes, textures and animation - lights are not supported)
  //You should now use GLSCene.load3DS() instead
  public byte[] data;
  public int datapos;

  public static boolean debug = false;

  public Model load(String filename) {
    try {
      return load3ds(new FileInputStream(filename));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  public Model load(InputStream is) {
    try {
      return load3ds(is);
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
    return uint16;
  }
  private int readuint32() {
    int uint32 = LE.getuint32(data, datapos);
    datapos += 4;
    return uint32;
  }
  private float readfloat() {
    return Float.intBitsToFloat(readuint32());
  }
  private Model load3ds(InputStream is) throws Exception {
    Object3 obj = null;
    int off = 0;
    int head_id;
    int head_len;
    int _siz;
    float[] _float;
    int[] _pts;
    boolean done_vertex = false;
    boolean done_pts = false;
    int vertexidx = -1;
    int vertexcnt = -1;
    String name;
    ArrayList<material> matlist;  //materials (objects refer to material name)
    ArrayList<String> objlist;  //object names (keyframe data refers to object name)
    String objname = "";
    int objidx = -1;
    material mat;
    String matname = "", fn;
    int a, b, keys, u32;
    boolean ok;
    int u16;
    Translate3 trans;
    Rotate3 rot;
    Scale3 scale;
    Model mod;
    UVMap map;
    int mapidx = -1;
    int skip=0;
    int parent;
    int frameno;

    datapos = 0;
    data = JF.readAll(is);

    matlist = new ArrayList<material>();
    objlist = new ArrayList<String>();

    mod = new Model();

    while (!eof()) {
      head_id = readuint16();
      head_len = readuint32();
      if (head_len == -1) break;  //this does happen in some files
      if (head_len < 6) throw new Exception("head_len < 6 (" + head_len + ")");  //bad file
      head_len -= 6;
//JFLog.log("id="+Integer.toString(head_id,16));
      switch (head_id) {
        case 0x4d4d:  //main chunk
          break;
        case 0x3d3d:  //mesh chunk
          break;
        case 0xafff:  //material chunk
          matname = "";
          break;
        case 0xa000:  //material chunk name
          matname = readname(head_len);
          break;
        case 0xa200:  //texture details
          break;
        case 0xa300:  //texture filename
          mat = new material();
          fn = readname(head_len);
//JFLog.log("texture filename" + fn);
          mat.name = matname;
          mat.filename = fn;
          matlist.add(mat);
          break;
        case 0x4000:  //object chunk
          objname = readname(-1);  //don't skip the whole chunk
          if (debug) {
            JFLog.log("obj=" + objname);
          }
          done_vertex = false;
          done_pts = false;
          break;
        case 0x4100:  //triangular object chunk
          break;
        case 0x4110:  //vertex list of a polygon
          skip = head_len;
          if (done_vertex) {JFLog.log("Warning : 2 vertex lists found for 1 object?");break;}
          obj = new Object3();
          obj.type = GL.GL_TRIANGLES;  //3ds only supports triangles
          obj.name = objname;
          mapidx = -1;
          mod.ol.add(obj);
          objlist.add(objname);
          _siz = readuint16();
          skip-=2;
          vertexidx = obj.vpl.size();
          vertexcnt = _siz;
          if (_siz == 0) break;
          _float = new float[_siz * 3];
          for(a=0;a<_siz;a++) {
            for(b=0;b<3;b++) {
              _float[a*3+b] = readfloat();
              skip-=4;
            }
            if (debug) {
              JFLog.log(String.format("v=%3.3f,%3.3f,%3.3f", _float[a*3+0] , _float[a*3+1] , _float[a*3+2]));
            }
          }
          obj.addVertex(_float);
          _float = null;
          done_vertex = true;
          break;
        case 0x4120:  //Points list
          _siz = readuint16();
          skip = _siz * 2 * 4;
          if (!done_vertex) {JFLog.log("Warning : pts list before vertex list?");break;}
          if (done_pts) {JFLog.log("Warning : 2 pts lists found for 1 object?");break;}
          if (_siz == 0) break;
          _pts = new int[3];  //p1,p2,p3,flgs per triangle
          for(a=0;a<_siz;a++) {
            for(b=0;b<3;b++) {
              _pts[b] = (short)readuint16();
              skip -= 2;
            }
            readuint16();  //skip flgs
            skip -= 2;
            obj.addPoly(_pts);
            if (debug) {
              JFLog.log("p=" + _pts[0] + "," + _pts[1] + "," + _pts[2]);
            }
          }
          _pts = null;
          done_pts = true;
          break;
        case 0x4130:  //object material name
          name = readname(head_len);
          mapidx++;
          map = obj.createUVMap();
          map.name = "uvmap" + mapidx;
          if (obj != null) {
            //find name in matlist
            ok = false;
            for(a=0;a<matlist.size();a++) {
              if (matlist.get(a).name.equals(name)) {
                int idx = mod.addTexture(matlist.get(a).filename);
                map.textureIndex = idx;
                ok = true;
                break;
              }
            }
//            if (!ok) throw new Exception("0x4130 : object material name not found in list : " + name);
          }
          if (debug) {
            JFLog.log("mat=" + map.textureIndex);
          }
          break;
        case 0x4140:  //texture vertex list (UV)
          _siz = readuint16();
          skip = _siz * 2 * 4;
          if (!done_vertex) {JFLog.log("Warning:Texture coords (UV) list before vertex list");break;}
          if (_siz != vertexcnt) {JFLog.log("Warning:texture list siz != vertex list siz");break;}
          if (_siz == 0) break;
          _float = new float[2];
          for(a=0;a<_siz;a++) {
            _float[0] = readfloat();  //U is okay
            skip-=4;
            _float[1] = 1.0f - readfloat();  //V must be inverted
            skip-=4;
            obj.addText(_float, mapidx);
            if (debug) {
              JFLog.log(String.format("t=%3.3f,%3.3f", _float[0] , _float[1]));
            }
          }
          _float = null;
          break;
        case 0x4160:  //obj matrix
          //read in 3x3 matrix and show for now
          int s = 0;  //padding to convert to 4x4 matrix
          for(a=0;a<3*3;a++) {
            u32 = readuint32();
            if ((a > 0) && (a % 3 == 0)) s++;
//not sure what this matrix is for??? But I don't seem to need it
//            obj.m.m[a+s] = readfloat();
//            if (debug) JFLog.log("m=" + obj.m.m[a+s]);
          }
          obj.org.x = readfloat();
          obj.org.y = readfloat();
          obj.org.z = readfloat();
          if (debug) JFLog.log("pos=" + obj.org.x + "," + obj.org.y + "," + obj.org.z);
          break;
        case 0xb000:  //keyframe header
          break;
        case 0xb002:  //object node chunk
          objidx = -1;
          break;
        case 0xb010:  //keyframe object name
          name = readname(-1);
          readuint16();  //f1
          readuint16();  //f2
          parent = readuint16();  //parent
          //find name in objlist
          objidx = 0;
          ok = false;
          for(a=0;a<objlist.size();a++) {
            if (objlist.get(a).equals(name)) {
              ok = true;
              break;
            }
            objidx++;
          }
          if (!ok) {
            objidx = -1;
          } else {
            obj = mod.ol.get(objidx);
            if (parent != 65535) obj.parent = parent;
            obj = null;
          }
//JFLog.log("0xb010 : name=" + name + ":objidx=" + objidx + ":parent=" + parent);
          break;
        case 0xb020:  //keyframe pos
          skip = head_len;
          if (objidx == -1) break;
          obj = mod.ol.get(objidx);
          u16 = readuint16();  //flgs
          skip -= 2;
          u32 = readuint32();  //r1
          skip -= 4;
          u32 = readuint32();  //r2
          skip -= 4;
          keys = readuint32();  //keys
          skip -= 4;
          _float = new float[3];
          for(a=0;a<keys;a++) {
            frameno = readuint32();  //frame #
            skip -= 4;
            u16 = readuint16();  //flgs
            skip -= 2;
            u32 = 0;
            if ((u16 & _3DS_FLG_TENSION) != 0) u32++;
            if ((u16 & _3DS_FLG_CONTINUITY) != 0) u32++;
            if ((u16 & _3DS_FLG_BIAS) != 0) u32++;
            if ((u16 & _3DS_FLG_EASE_TO) != 0) u32++;
            if ((u16 & _3DS_FLG_EASE_FROM) != 0) u32++;
            if (u32 > 0) {
              datapos += u32 * 4;    //all ignored
              skip -= u32 * 4;
            }
            trans = new Translate3();
            for(b=0;b<3;b++) {
              _float[b] = readfloat();
              skip -= 4;
            }
            trans.x = _float[0];
            trans.y = _float[1];
            trans.z = _float[2];
//JFLog.log("pos["+frameno+"]:"+pos.x+","+pos.y+","+pos.z+":flgs="+u16);
            obj.tl.put(frameno, trans);
            if (obj.maxframeCount < frameno) obj.maxframeCount = frameno;
          }
          _float = null;
          obj = null;
          break;
        case 0xb021:  //keyframe rotate
          skip = head_len;
          if (objidx == -1) break;
          obj = mod.ol.get(objidx);
          u16 = readuint16();  //flgs
          skip -= 2;
          u32 = readuint32();  //r1
          skip -= 4;
          u32 = readuint32();  //r2
          skip -= 4;
          keys = readuint32();  //keys
          skip -= 4;
          _float = new float[4];
          for(a=0;a<keys;a++) {
            frameno = readuint32();  //frame #
            skip -= 4;
            u16 = readuint16();  //flgs
            skip -= 2;
            u32 = 0;
            if ((u16 & _3DS_FLG_TENSION) != 0) u32++;
            if ((u16 & _3DS_FLG_CONTINUITY) != 0) u32++;
            if ((u16 & _3DS_FLG_BIAS) != 0) u32++;
            if ((u16 & _3DS_FLG_EASE_TO) != 0) u32++;
            if ((u16 & _3DS_FLG_EASE_FROM) != 0) u32++;
            if (u32 > 0) {
              datapos += u32 * 4;    //all ignored
              skip -= u32 * 4;
            }
            rot = new Rotate3();
            for(b=0;b<4;b++) {
              _float[b] = readfloat();
              skip -= 4;
            }
            rot.angle = _float[0] * 57.2957795f;  //convert to degrees
            rot.x = _float[1];
            rot.y = _float[2];
            rot.z = _float[3];
//JFLog.log("rot["+frameno+"]:"+rot.angle+","+rot.x+","+rot.y+","+rot.z+":flgs="+u16);
            obj.rl.put(frameno, rot);
            if (obj.maxframeCount < frameno) obj.maxframeCount = frameno;
          }
          _float = null;
          obj = null;
          break;
        case 0xb022:  //keyframe scale
          skip = head_len;
          if (objidx == -1) break;
          obj = mod.ol.get(objidx);
          u16 = readuint16();  //flgs
          skip -= 2;
          u32 = readuint32();  //r1
          skip -= 4;
          u32 = readuint32();  //r2
          skip -= 4;
          keys = readuint32();  //keys
          skip -= 4;
          _float = new float[3];
          for(a=0;a<keys;a++) {
            frameno = readuint32();  //frame #
            skip -= 4;
            u16 = readuint16();  //flgs
            skip -= 2;
            u32 = 0;
            if ((u16 & _3DS_FLG_TENSION) != 0) u32++;
            if ((u16 & _3DS_FLG_CONTINUITY) != 0) u32++;
            if ((u16 & _3DS_FLG_BIAS) != 0) u32++;
            if ((u16 & _3DS_FLG_EASE_TO) != 0) u32++;
            if ((u16 & _3DS_FLG_EASE_FROM) != 0) u32++;
            if (u32 > 0) {
              datapos += u32 * 4;    //all ignored
              skip -= u32 * 4;
            }
            scale = new Scale3();
            for(b=0;b<3;b++) {
              _float[b] = readfloat();
              skip -= 4;
            }
            scale.x = _float[0];
            scale.y = _float[1];
            scale.z = _float[2];
//JFLog.log("scale["+frameno+"]:"+scale.x+","+scale.y+","+scale.z+":flgs="+u16);
            obj.sl.put(frameno, scale);
            if (obj.maxframeCount < frameno) obj.maxframeCount = frameno;
          }
          _float = null;
          obj = null;
          break;
        default:
          skip = head_len;
          break;
      }
      if (skip > 0) {
        datapos += skip;
        skip = 0;
      }
    }
    //setup any lights
    _siz = mod.ol.size();
    for(a=0;a<_siz;a++) {
      obj = mod.ol.get(a);
    }
    //delete temp lists
    matlist.clear();
    objlist.clear();
    return mod;
  }
  //for load3DS()
  private String readname(int maxread) {
    String ret = "";
    char ch;
    while (!eof()) {
      ch = (char)data[datapos++];
      if (maxread != -1) {
        maxread--;
        if (maxread == 0) break;
      }
      if (ch == 0) break;
      ret += ch;
    }
    if (maxread > 0) {datapos+=maxread;}
    return ret;
  }
  //private class for load3DS()
  private static class material {
    String name;
    String filename;
  }
}
