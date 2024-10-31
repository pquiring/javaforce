package javaforce.gl.model;

import java.io.*;
import java.util.*;
import javaforce.*;
import javaforce.gl.*;

/**
 * Blender .blend reader
 *
 * NOTE:
 *   Supports Blender v2.63+ thru 4.02 (some versions in between may not be supported)
 *   Supports objects with multiple UVMaps
 *   Rotation/Scale on objects are ignored, please rotate/scale in edit mode (the vertex data)
 *   BHead chunks can have duplicate old pointer addresses in which case they must be used in order.
 *     See : https://developer.blender.org/T45471
 * TODO:
 *   Animation data
 *
 * Blender Source : https://github.com/blender/blender/tree/main
 *   - look in blenloader and makesdna folders
 *   - most important to understand DNA : makesdna/intern/dna_genfile.c:init_structDNA()
 *   - also see doc/blender_file_format/mystery_of_the_blend.html
 *   - see https://github.com/blender/blender/tree/main/source/blender/blenloader/intern
 *
 * @author pquiring
 */

public class GL_BLEND {

  public static boolean debug = false;
  public static boolean debugDNA = false;
  public static boolean debugScene = false;  //obsolete
  public static boolean debugCD = false;
  public static boolean debugCDProp = false;

  private byte[] data;
  private int datapos;

  private boolean x64;  //64bit file format (else 32bit)
  private boolean le;  //little endian file format (else big endian)

  private Model model;
  private Object3 obj;

  private float[] org = new float[3];
  private boolean haveDups;

  private HashMap<Long, Chunk> chunk_map = new HashMap<Long, Chunk>();

// typedef enum ID_Type {...} see https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_ID_enums.h
  private static final int ID_ME = 0x454d;  //ME (mesh)
  private static final int ID_OB = 0x424f;  //OB (object)
  private static final int ID_SC = 0x4353;  //SCE (scene)
  private static final int ID_DNA1 = 0x31414e44;  //DNA1

// typedef enum ObjectType {...} see https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_object_types.h
  private static final int OB_MESH             = 1;  //only one of interest

// typedef enum CustomDataType {...}
  private static final int CD_MVERT            = 0;
  private static final int CD_MSTICKY          = 1;  /* DEPRECATED */
  private static final int CD_MDEFORMVERT      = 2;
  private static final int CD_MEDGE            = 3;
  private static final int CD_MFACE            = 4;
  private static final int CD_MTFACE           = 5;
  private static final int CD_MCOL             = 6;
  private static final int CD_ORIGINDEX        = 7;
  private static final int CD_NORMAL           = 8;
/*  private static final int CD_POLYINDEX        = 9; */
  private static final int CD_PROP_FLT         = 10;
  private static final int CD_PROP_INT         = 11;
  private static final int CD_PROP_STR         = 12;
  private static final int CD_ORIGSPACE        = 13;  /* for modifier stack face location mapping */
  private static final int CD_ORCO             = 14;
  private static final int CD_MTEXPOLY         = 15;
  private static final int CD_MLOOPUV          = 16;
  private static final int CD_MLOOPCOL         = 17;
  private static final int CD_TANGENT          = 18;
  private static final int CD_MDISPS           = 19;
  private static final int CD_PREVIEW_MCOL     = 20;  /* for displaying weightpaint colors */
  private static final int CD_ID_MCOL          = 21;
  private static final int CD_TEXTURE_MCOL     = 22;
  private static final int CD_CLOTH_ORCO       = 23;
  private static final int CD_RECAST           = 24;

/* BMESH ONLY START */
  private static final int CD_MPOLY            = 25;
  private static final int CD_MLOOP            = 26;
  private static final int CD_SHAPE_KEYINDEX   = 27;
  private static final int CD_SHAPEKEY         = 28;
  private static final int CD_BWEIGHT          = 29;
  private static final int CD_CREASE           = 30;
  private static final int CD_ORIGSPACE_MLOOP  = 31;
  private static final int CD_PREVIEW_MLOOPCOL = 32;
  private static final int CD_BM_ELEM_PYPTR    = 33;
/* BMESH ONLY END */

  private static final int CD_PAINT_MASK       = 34;
  private static final int CD_GRID_PAINT_MASK  = 35;
  private static final int CD_MVERT_SKIN       = 36;
  private static final int CD_FREESTYLE_EDGE   = 37;
  private static final int CD_FREESTYLE_FACE   = 38;
  private static final int CD_MLOOPTANGENT     = 39;
  private static final int CD_TESSLOOPNORMAL   = 40;
  private static final int CD_CUSTOMLOOPNORMAL = 41;

  private static final int CD_SCULPT_FACE_SETS = 42;
  private static final int CD_LOCATION = 43;  //unused
  private static final int CD_RADIUS = 44;  //unused
  private static final int CD_PROP_INT8 = 45;
  private static final int CD_PROP_INT32_2D = 46;
  private static final int CD_PROP_COLOR = 47;
  private static final int CD_PROP_FLOAT3 = 48;  //vertex:xyz
  private static final int CD_PROP_FLOAT2 = 49;
  private static final int CD_PROP_BOOL = 50;
  private static final int CD_HAIRLENGTH = 51;  //unused
  private static final int CD_PROP_QUATERNION = 52;

  private static final int CD_NUMTYPES         = 53;

  //DNA stuff
  private ArrayList<String> names = new ArrayList<String>();  //member names
  private ArrayList<String> types = new ArrayList<String>();  //struct names
  private ArrayList<Short> typelen = new ArrayList<Short>();
  private class struct {  //struct SDNA_Struct
    short typeidx;  //index into types
    short mem_nr;  //# of members
    String name;
    ArrayList<member> members = new ArrayList<member>();
  }
  private class member {  //struct SDNA_Struct
    short typelenidx;  //index into typelen
    short nameidx;  //index into names
    String name;
    int typelen;
    int size;  //total size of member
  }
  private ArrayList<struct> structs = new ArrayList<struct>();
  private struct getStruct(String name) throws Exception {
    for(int a=0;a<structs.size();a++) {
      struct s = structs.get(a);
      if (s.name.equals(name)) return s;
    }
    throw new Exception("struct not found:" + name);
  }
  private int calcMemberSize(member m) {
    if (m.name.startsWith("*")) {
      if (x64) return 8;
      return 4;
    }
    if (m.name.indexOf("[") != -1) {
      //array type
      String[] f = m.name.replaceAll("\\]", "").split("\\[");
      if (f.length == 2) {
        //single array
        return m.typelen * Integer.valueOf(f[1]);
      } else {
        //double array
        return m.typelen * Integer.valueOf(f[1]) * Integer.valueOf(f[2]);
      }
    }
    return m.typelen;
  }

  public Model load(String filename) {
    try {
      return loadBlend(new FileInputStream(filename));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  public Model load(InputStream is) {
    try {
      return loadBlend(is);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  private boolean eof() {
    return datapos >= data.length;
  }
  private byte readuint8() {
    byte uint8 = data[datapos++];
    return uint8;
  }
  private short readuint16() {
    int uint16;
    if (le) uint16 = LE.getuint16(data, datapos);
    else uint16 = BE.getuint16(data, datapos);
    datapos += 2;
    return (short)uint16;
  }
  private int readuint32() {
    int uint32;
    if (le) uint32 = LE.getuint32(data, datapos);
    else uint32 = BE.getuint32(data, datapos);
    datapos += 4;
    return uint32;
  }
  private long readuint64() {
    long uint64;
    if (le) uint64 = LE.getuint64(data, datapos);
    else uint64 = BE.getuint64(data, datapos);
    datapos += 8;
    return uint64;
  }
  private long readptr() {
    if (x64) return readuint64();
    return readuint32();
  }
  private void readByteArray(byte[] in) {
    System.arraycopy(data, datapos, in, 0, in.length);
    datapos+=in.length;
  }
  private void readPtrArray(long[] in) {
    for(int a=0;a<in.length;a++) {
      in[a] = readptr();
    }
  }
  private void readFloatArray(float[] in) {
    for(int a=0;a<in.length;a++) {
      in[a] = readfloat();
    }
  }
  private float readfloat() {
    return Float.intBitsToFloat(readuint32());
  }
  /* Read fixed size char[] string */
  private String readString(int len) {
    int sl = strlen(data, datapos, len);
    String str = new String(data, datapos, sl);
    datapos+=len;
    return str;
  }
  /* Read C style string (NULL terminated) */
  private String readString() {
    int sl = strlen(data, datapos, data.length - datapos);
    String str = new String(data, datapos, sl);
    datapos += sl+1;
    return str;
  }
  private void setData(byte[] in) {
    data = in;
    datapos = 0;
  }
  private class Context {
    byte[] data;
    int datapos;
  }
  private Context pushData() {
    Context ctx = new Context();
    ctx.data = data;
    ctx.datapos = datapos;
    return ctx;
  }
  private void popData(Context ctx) {
    data = ctx.data;
    datapos = ctx.datapos;
  }
  private int strlen(byte[] str, int offset, int max) {
    for(int a=0;a<max;a++) {
      if (str[a+offset] == 0) return a;
    }
    return max;
  }
  private Chunk findChunkByPtr(long ptr) {
    if (ptr == 0) return null;
    Chunk chunk = chunk_map.get(ptr);
    if (chunk == null) return null;
    if (chunk.dup) {
//      JFLog.log("Duplicate:" + Long.toString(ptr, 16) + ",idx=" + chunk.dupidx);
      int cnt = chunk.dupidx;
      chunk.dupidx++;
      for(int a=0;a<cnt;a++) {
        chunk = chunk.nextdup;
      }
    }
    return chunk;
  }
  private class Vertex {
    float[] xyz = new float[3];
  }
  private class UV {
    float[] uv = new float[2];
  }
  private Model loadBlend(InputStream is) throws Exception {
    setData(JF.readAll(is));

    if (data.length < 12) {
      throw new Exception("BLEND:File too small");
    }

    model = new Model();

    //load signature (12 bytes) "BLENDER_V100"
    if (!new String(data, 0, 7).equals("BLENDER")) {
      throw new Exception("Not a blender file");
    }
    switch (data[7]) {
      case '-': x64 = true; break;
      case '_': x64 = false; break;
      default:
        throw new Exception("BLEND:Unknown bit size");
    }

    switch (data[8]) {
      case 'v': le = true; break;
      case 'V': le = false; break;
      default:
        throw new Exception("BLEND:Unknown Endianness");
    }

    String version = new String(data, 9, 3);
    JFLog.log("Blender file version:" + version);
    int ver = Integer.valueOf(version);
    if (ver < 263) {
      throw new Exception("Error:Blender file too old, can not read.");
    }

    datapos = 12;  //skip main header

    //first phase - read raw chunks
    while (!eof()) {
      Chunk chunk = new Chunk();
      chunk.filepos = datapos;
      chunk.read();
      Chunk ochunk = chunk_map.get(chunk.ptr);
      if (ochunk != null) {
        if (!haveDups) {
          JFLog.log("Warning:This file contains duplicate BHeads.");
          haveDups = true;
        }
        ochunk.dup = true;
        while (ochunk.nextdup != null) {
          ochunk = ochunk.nextdup;
        }
        ochunk.nextdup = chunk;
      } else {
        chunk_map.put(chunk.ptr, chunk);
      }
    }

    int chunkCnt = chunk_map.size();
    Chunk[] chunks = chunk_map.values().toArray(new Chunk[chunkCnt]);

    //2nd phase - parse DNA chunk
    for(Chunk chunk : chunks) {
      if (chunk.id == ID_DNA1) {
        if (debug) {
          JFLog.log("Blend:ID_DNA1 @ " + chunk.ptr);
        }
        setData(chunk.raw);
        //SDNA
        String SDNA = readString(4);
        if (!SDNA.equals("SDNA")) throw new Exception("Bad DNA Struct:SDNA");
        //NAME
        String NAME = readString(4);
        if (!NAME.equals("NAME")) throw new Exception("Bad DNA Struct:NAME");
        int nr_names = readuint32();
        for(int a=0;a<nr_names;a++) {
          String str = readString();
          if (debugDNA) JFLog.log("DNA.name=" + str);
          names.add(str);
        }
        //align pointer
        datapos += 3;
        datapos &= 0xfffffffc;
        //TYPE
        String TYPE = readString(4);
        if (!TYPE.equals("TYPE")) throw new Exception("Bad DNA Struct:TYPE");
        int nr_types = readuint32();
        for(int a=0;a<nr_types;a++) {
          String str = readString();
          if (debugDNA) JFLog.log("DNA.type=" + str);
          types.add(str);
        }
        //align pointer
        datapos += 3;
        datapos &= 0xfffffffc;
        //TLEN
        String TLEN = readString(4);
        if (!TLEN.equals("TLEN")) throw new Exception("Bad DNA Struct:TLEN");
        for(int a=0;a<nr_types;a++) {
          typelen.add(readuint16());
        }
        //align pointer
        datapos += 3;
        datapos &= 0xfffffffc;
        //STRC
        String STRC = readString(4);
        if (!STRC.equals("STRC")) throw new Exception("Bad DNA Struct:STRC");
        int nr_structs = readuint32();
        for(int a=0;a<nr_structs;a++) {
          struct s = new struct();
          s.typeidx = readuint16();
          s.mem_nr = readuint16();
          s.name = types.get(s.typeidx);
          if (debugDNA) JFLog.log("DNA.struct:" + s.name + "==" + a);
          for(int b=0;b<s.mem_nr;b++) {
            member m = new member();
            m.typelenidx = readuint16();
            m.nameidx = readuint16();
            m.name = names.get(m.nameidx);
            m.typelen = typelen.get(m.typelenidx);
            m.size = calcMemberSize(m);
            if (debugDNA) JFLog.log("  member:" + m.name + "=" + m.typelen);
            s.members.add(m);
          }
          structs.add(s);
        }
        break;
      }
    }

    //3nd phase - now look for objects and piece together chunks
    for(Chunk chunk : chunks) {
      if (chunk.id == ID_SC) {
        if (debug) {
          JFLog.log("Blend:ID_SC @ " + chunk.ptr);
        }
        if (false) {  //ignore - use Objects instead
          setData(chunk.raw);
          Scene scene = new Scene();
          scene.read();
          long ptr = scene.last;
          if (debug) JFLog.log("  scene.last=" + ptr);
          while (ptr != 0) {
            Chunk chunk2 = findChunkByPtr(ptr);
            if (chunk2 == null) break;
            setData(chunk2.raw);
            Base base = new Base();
            base.read();
            chunk2 = findChunkByPtr(base.object);
            if (chunk2.id == ID_OB) {
              readObject(chunk2);
            }
            ptr = base.prev;
          }
        }
      }

      if (chunk.id == ID_OB) {
        if (debug) {
          JFLog.log("Blend:ID_OB @ " + chunk.ptr);
        }
        readObject(chunk);
      }

      if (chunk.id == ID_ME) {
        if (debug) {
          JFLog.log("Blend:ID_ME @ " + chunk.ptr);
        }
      }
    }

    return model;
  }

  private void readObject(Chunk chunk) throws Exception {
    setData(chunk.raw);
    bObject bObj = new bObject();
    bObj.read();
    if (debug) JFLog.log("Blend:object.type=" + bObj.type);
    if (bObj.type != OB_MESH) return;  //not a mesh object (could be camera, light, etc.)
    obj = new Object3();
    model.addObject(obj);
    obj.name = bObj.id.name.substring(2);
    obj.org.x = bObj.loc[0];
    org[0] = bObj.loc[0];
    obj.org.y = bObj.loc[1];
    org[1] = bObj.loc[1];
    obj.org.z = bObj.loc[2];
    org[2] = bObj.loc[2];
    if (debug) JFLog.log("Blend:object.name=" + obj.name + ",org=" + org[0] + "," + org[1] + "," + org[2]);
    chunk = findChunkByPtr(bObj.data);
    if (chunk == null) {
      throw new Exception("BLEND:Unable to find Mesh for Object");
    }
    readMesh(chunk);
  }

  private void readMesh(Chunk chunk) throws Exception {
    ArrayList<Vertex> vertexList = new ArrayList<Vertex>();
    ArrayList<Integer> loopList = new ArrayList<Integer>();
    Mesh mesh = new Mesh();
    setData(chunk.raw);
    if (debug) JFLog.log("Blend.Mesh @ " + chunk.fileOffset);
    mesh.read();
    //find mvert (obsolete)
    if (mesh.mvert != 0) {
      chunk = findChunkByPtr(mesh.mvert);
      if (chunk == null) {
        JFLog.log("BLEND:Unable to find MVert for Mesh:" + mesh.mvert);
        mesh.mvert = 0;
      } else {
        setData(chunk.raw);
        for(int a=0;a<chunk.array_nr;a++) {
          MVert mvert = new MVert();
          mvert.read();
    //          obj.addVertex(mvert.co);
          Vertex v = new Vertex();
          v.xyz[0] = mvert.v[0];
          v.xyz[1] = mvert.v[1];
          v.xyz[2] = mvert.v[2];
          vertexList.add(v);
          if (debugCDProp) JFLog.log("PROP_FLOAT3:" + v.xyz[0] + "," + v.xyz[1] + "," + v.xyz[2]);
        }
      }
    }
    //find mloop (obsolete)
    if (mesh.mloop != 0) {
      chunk = findChunkByPtr(mesh.mloop);
      if (chunk == null) {
        JFLog.log("BLEND:Unable to find MLoop for Mesh:" + mesh.mloop);
        mesh.mloop = 0;
      } else {
        setData(chunk.raw);
        for(int a=0;a<chunk.array_nr;a++) {
          MLoop mloop = new MLoop();
          mloop.read();
          loopList.add(mloop.v);
          if (debugCDProp) JFLog.log("PROP_INT_LOOP:" + mloop.v);
        }
      }
    }
    //find mloopuv
/*  //use the UVMaps in the CustomData instead - this is only the active one
    raw = findChunkByPtr(mesh.mloopuv);
    if (raw == null) {
      throw new Exception("BLEND:Unable to find MLoopUV for Mesh");
    }
    setData(raw.raw);
    JFLog.log("MLoopUV:nr=" + raw.nr);
    for(int a=0;a<raw.nr;a++) {
      MLoopUV mloopuv = new MLoopUV();
      mloopuv.read();
    }
*/
    //find mpoly
    if (mesh.mpoly != 0) {
      chunk = findChunkByPtr(mesh.mpoly);
      if (chunk == null) {
        JFLog.log("BLEND:Unable to find MPoly for Mesh:" + mesh.mpoly);
        mesh.mpoly = 0;
      } else {
        setData(chunk.raw);
        //TODO : calc which vertex needed to be dup'ed for each unique uv value (Blender does this in their 3ds export script)
        int type = -1;
        int pcnt = -1;
        int vidx = 0;
        //MPoly = faces
        for(int a=0;a<chunk.array_nr;a++) {
          MPoly mpoly = new MPoly();
          mpoly.read();
          switch (mpoly.totloop) {
            case 3:
              if (type == GL.GL_QUADS) {
                throw new Exception("BLEND:Mixed QUADS/TRIANGLES not supported");
              }
              type = GL.GL_TRIANGLES;
              pcnt = 3;
              break;
            case 4:
              if (type == GL.GL_TRIANGLES) {
                throw new Exception("BLEND:Mixed QUADS/TRIANGLES not supported");
              }
              type = GL.GL_QUADS;
              pcnt = 4;
              break;
            default:
              throw new Exception("BLEND:Polygon not supported:nr=" + mpoly.totloop);
          }
          int loopidx = mpoly.loopstart;
          if (debugCDProp) JFLog.log("PROP_INT_START:" + loopidx);
          int[] poly = new int[1];
          for(int p=0;p<pcnt;p++) {
            int idx = loopList.get(loopidx++);
            if (debugCDProp) JFLog.log("PROP_INT_POLY:" + idx);
            obj.addVertex(vertexList.get(idx).xyz);
            poly[0] = vidx++;
            obj.addPoly(poly);
          }
        }
        obj.type = type;
        if (debugCDProp) JFLog.log("Blend:obj.type=" + obj.type);
      }
    }
    //find customdata types
    readCustomDataLayer(mesh, mesh.vdata, "vdata", vertexList, loopList);  //vert_data
    readCustomDataLayer(mesh, mesh.edata, "edata", vertexList, loopList);  //edge_data
    readCustomDataLayer(mesh, mesh.fdata, "fdata", vertexList, loopList);  //face_data
    readCustomDataLayer(mesh, mesh.pdata, "pdata", vertexList, loopList);  //poly_data
    readCustomDataLayer(mesh, mesh.ldata, "ldata", vertexList, loopList);  //layer_data
  }
  private void readCustomDataLayer(Mesh mesh, CustomData cd, String name, ArrayList<Vertex> vertexList, ArrayList<Integer> loopList) throws Exception {
    if (debugCD) JFLog.log("readLayer:" + name + ".layers=" + Long.toString(cd.layers, 16));
    if (cd == null || cd.layers == 0) return;
    Chunk raw = findChunkByPtr(cd.layers);
    if (raw == null) {
      throw new Exception("BLEND:Unable to find " + name + ".layers for Mesh");
    }
    setData(raw.raw);
    if (debugCD) JFLog.log("readLayer:#layers=" + cd.totlayer + ",max=" + cd.maxlayer);
    for(int a=0;a<cd.totlayer;a++) {
      CustomDataLayer layer = new CustomDataLayer();
      layer.read();
      String layer_name = layer.name;
      if (layer.data == 0) {
        if (debugCD) JFLog.log("readLayer:layer.data == null");
        continue;
      }
      Chunk layer_data = findChunkByPtr(layer.data);
      if (layer_data == null) {
        throw new Exception("BLEND:Unable to find " + name + ".layers.data for Mesh");
      }
      Context ctx = pushData();
      setData(layer_data.raw);
      if (debugCD) JFLog.log("layer.data=" + Long.toString(layer.data, 16) + ",type=" + layer.type + ",a=" + a + ",nr=" + layer_data.array_nr + ",size=" + layer_data.raw.length);
      switch (layer.type) {
        case CD_MVERT: {  //0 (obsolete)
          //only use if mvert was missing
          if (mesh.mvert == 0) {
            MVert mvert = new MVert();
            if (debugCD) JFLog.log("Vertex:" + layer_data.array_nr);
            for(int i=0;i<layer_data.array_nr;i++) {
              mvert.read();
              Vertex v = new Vertex();
              v.xyz[0] = mvert.v[0];
              v.xyz[1] = mvert.v[1];
              v.xyz[2] = mvert.v[2];
              vertexList.add(v);
              obj.addVertex(v.xyz);
            }
          }
          break;
        }
        case CD_PROP_INT: {  //11
          //poly lists
          //24 per cube
          //NOTE : There are 2 lists : the 2nd one looks invalid ???
          if (loopList.isEmpty()) {
            int pcnt = -1;
            obj.type = GL.GL_QUADS;  //TODO : find proper type
            pcnt = 4;
            if (debugCDProp) JFLog.log("Blend:obj.type=" + obj.type);
            int[] poly = new int[pcnt];
            int pi = 0;
            int vidx = 0;
            if (debugCD) JFLog.log("Poly:" + layer_data.array_nr);
            for(int i=0;i<layer_data.array_nr;i++) {
              int idx = readuint32();
              if (debugCDProp) JFLog.log("PROP_INT:" + idx);
              loopList.add(idx);
              obj.addVertex(vertexList.get(idx).xyz);
              poly[pi++] = vidx++;
              if (pi == pcnt) {
                obj.addPoly(poly);
                pi = 0;
              }
            }
          }
          break;
        }
        case CD_PROP_FLOAT2: {  //49
          //UV coords
          //24 per cube
          if (obj.getUVMaps() == 0) {
            int tidx = model.addTexture("fake.png");
            UVMap map = obj.createUVMap();
            map.name = layer_name;
            map.textureIndex = tidx;
            float[] uv = new float[2];
            if (debugCD) JFLog.log("UV:" + layer_data.array_nr);
            for(int i=0;i<layer_data.array_nr;i++) {
              uv[0] = readfloat();
              uv[1] = readfloat();
              uv[1] = 1.0f - uv[1];  //invert V(y)
              if (debugCDProp) JFLog.log("PROP_FLOAT2:" + uv[0] + "," + uv[1]);
              obj.addText(uv);
            }
          }
          break;
        }
        case CD_PROP_FLOAT3: {  //48
          //vertex list (only use if mvert was missing)
          //8 per cube
          if (mesh.mvert == 0) {
            if (debugCD) JFLog.log("Vertex:" + layer_data.array_nr);
            for(int i=0;i<layer_data.array_nr;i++) {
              Vertex v = new Vertex();
              v.xyz[0] = readfloat() + org[0];
              v.xyz[1] = readfloat() + org[1];
              v.xyz[2] = readfloat() + org[2];
              if (debugCDProp) JFLog.log("PROP_FLOAT3:" + v.xyz[0] + "," + v.xyz[1] + "," + v.xyz[2]);
              vertexList.add(v);
            }
          }
          break;
        }
        case CD_PROP_INT32_2D: {  //46
          //12 per cube - edges? (not used)
          if (debugCD) JFLog.log("Edge:" + layer_data.array_nr);
          for(int i=0;i<layer_data.array_nr;i++) {
            int v1 = readuint32();
            int v2 = readuint32();
//            if (debugCDProp) JFLog.log("PROP_INT32_2D:" + v1 + "," + v2);
          }
          break;
        }
        case CD_MTEXPOLY: {  //15 (obsolete)
          //NOTE:There is a MTexPoly per face, I only read the first
          //contains texture filenames
          MTexPoly tex = new MTexPoly();
          tex.read();
          Chunk imageChunk = findChunkByPtr(tex.tpage);
          if (imageChunk == null) {
            throw new Exception("BLEND:No texture found for UVMap:" + a);
          }
          setData(imageChunk.raw);
          Image image = new Image();
          image.read();
          UVMap map;
          if (a < obj.getUVMaps())
            map = obj.getUVMap(a);
          else
            map = obj.createUVMap();
          String tn = image.name;
          //string texture path for now
          int tnidx = tn.lastIndexOf("/");
          if (tnidx != -1) {
            tn = tn.substring(tnidx+1);
          }
          tnidx = tn.lastIndexOf("\\");
          if (tnidx != -1) {
            tn = tn.substring(tnidx+1);
          }
          int tidx = model.addTexture(tn);
          if (debugCDProp) JFLog.log("Blend:texture=" + tn + "=" + tidx);
          map.textureIndex = tidx;
          map.name = layer_name;
          if (debugCD) JFLog.log("texpoly=" + map.name);
          break;
        }
        case CD_MLOOPUV: { //16 (obsolete)
          //UV per face per vertex
          if (a >= obj.getUVMaps()) {
            obj.createUVMap();
          }
          for(int i=0;i<layer_data.array_nr;i++) {
            MLoopUV uv = new MLoopUV();
            uv.read();
            uv.uv[1] = 1.0f - uv.uv[1];  //invert V(y)
            obj.addText(uv.uv, a);
            if (debugCDProp) JFLog.log("PROP_FLOAT2:" + uv.uv[0] + "," + uv.uv[1]);
          }
          break;
        }
      }
      popData(ctx);
    }
  }

  private class Chunk {  //struct BHead
    int id;
    int len;
    long ptr;  //the actual memory address of this chunk when it was saved to disk !!!
    int SDNAnr;
    int array_nr;  //array count of struct

    byte raw[];

    int filepos;  //for debugging

    boolean dup;
    int dupidx;
    Chunk nextdup;

    int fileOffset;
    void read() {
      id = readuint32();
      len = readuint32();
      ptr = readptr();
      SDNAnr = readuint32();
      array_nr = readuint32();
      fileOffset = datapos;
      if (len == 0) return;
      raw = new byte[len];
      readByteArray(raw);
    }
  }
  private class ID {
    String name;
    void read() throws Exception {
      struct s = getStruct("ID");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.startsWith("name[")) {
          name = readString(m.size);
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class Scene {
    long first;  //first Base
    long last;
    void read() throws Exception {
      struct s = getStruct("Scene");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (debugScene) JFLog.log("  member:" + m.name + ",size=" + m.size);
        switch (m.name) {
          case "base":
            first = readptr();
            last = readptr();
            if (debug) JFLog.log("  scene.first=" + first + ",last=" + last);
            break;
          default:
            datapos += m.size;
            break;
        }
      }
    }
  }
  private class Base {
    long next;
    long prev;
    long object;
    void read() throws Exception {
      struct s = getStruct("Base");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("*next")) {
          next = readptr();
        }
        else if (m.name.equals("*prev")) {
          prev = readptr();
        }
        else if (m.name.equals("*object")) {
          object = readptr();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  //see https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_object_types.h
  private class bObject {
    ID id = new ID();
    int type;
    long data;  // -> Mesh
    float loc[] = new float[3];  //Location (aka Origin)
    void read() throws Exception {
      struct s = getStruct("Object");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("*data")) {
          data = readptr();
        }
        else if (m.name.equals("loc[3]")) {
          readFloatArray(loc);
        }
        else if (m.name.equals("type")) {
          type = readuint16();
        }
        else if (m.name.equals("id")) {
          id.read();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class CustomData {
    long layers;  //->CustomDataLayer
    int totlayer;  //# of layers used
    int maxlayer;  //# of layers avail
    void read(String name) throws Exception {
      struct s = getStruct("CustomData");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("*layers")) {
          layers = readptr();
        } else if (m.name.equals("totlayer")) {
          totlayer = readuint32();
        } else if (m.name.equals("maxlayer")) {
          maxlayer = readuint32();
        } else {
          datapos += m.size;
        }
      }
    }
  }
  private class CustomDataLayer {
    int type;         /* type of data in layer */
    String name;      /* layer name, MAX_CUSTOMDATA_LAYER_NAME */
    long data;        /* layer data */
    void read() throws Exception {
      struct s = getStruct("CustomDataLayer");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("type")) {
          type = readuint32();
        }
        else if (m.name.startsWith("name[")) {
          name = readString(m.size);
        }
        else if (m.name.equals("*data")) {
          data = readptr();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class Mesh {
    ID id = new ID();
    long mpoly, mloop, mloopuv, mvert;
    CustomData vdata = new CustomData();  //vert_data
    CustomData edata = new CustomData();  //edge_data
    CustomData fdata = new CustomData();  //face_data
    CustomData pdata = new CustomData();  //poly_data (obsolete)
    CustomData ldata = new CustomData();  //loop_data (obsolete)
    void read() throws Exception {
      struct s = getStruct("Mesh");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("*mpoly")) {
          mpoly = readptr();
        }
        else if (m.name.equals("*mloop")) {
          mloop = readptr();
        }
        else if (m.name.equals("*mloopuv")) {
          mloopuv = readptr();
        }
        else if (m.name.equals("*mvert")) {
          mvert = readptr();
        }
        else if (m.name.equals("vdata") || m.name.equals("vert_data")) {
          vdata.read("vdata");
        }
        else if (m.name.equals("edata") || m.name.equals("edge_data")) {
          edata.read("edata");
        }
        else if (m.name.equals("fdata") || m.name.equals("face_data")) {
          fdata.read("fdata");
        }
        else if (m.name.equals("pdata")) {
          pdata.read("pdata");
        }
        else if (m.name.equals("ldata")) {
          ldata.read("ldata");
        }
        else if (m.name.equals("id")) {
          id.read();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class MVert {
    float v[] = new float[3];
    void read() throws Exception {
      struct s = getStruct("MVert");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("co[3]")) {
          for(int b=0;b<3;b++) {
            v[b] = readfloat() + org[b];  //xyz position
          }
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class MPoly {
    /* offset into loop array and number of loops in the face */
    int loopstart;
    int totloop;
    void read() throws Exception {
      struct s = getStruct("MPoly");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("loopstart")) {
          loopstart = readuint32();
        }
        else if (m.name.equals("totloop")) {
          totloop = readuint32();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class MLoop {
    int v;  /* vertex index */
    void read() throws Exception {
      struct s = getStruct("MLoop");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("v")) {
          v = readuint32();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class MTexPoly {
    long tpage;  //Image
    void read() throws Exception {
      struct s = getStruct("MTexPoly");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("*tpage")) {
          tpage = readptr();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class MLoopUV {
    float uv[] = new float[2];
    void read() throws Exception {
      struct s = getStruct("MLoopUV");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("uv[2]")) {
          uv[0] = readfloat();
          uv[1] = readfloat();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class Image {
    ID id = new ID();
    String name;
    void read() throws Exception {
      struct s = getStruct("Image");
      for(int a=0;a<s.mem_nr;a++) {
        member m = s.members.get(a);
        if (m.name.startsWith("name[")) {
          name = readString(m.size);
        }
        else if (m.name.equals("id")) {
          id.read();
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
}
