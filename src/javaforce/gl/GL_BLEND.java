package javaforce.gl;

import java.io.*;
import java.util.*;
import javaforce.*;

/**
 * Blender .blend reader
 *
 * NOTE:
 *   Supports Blender v2.63+
 *   Supports objects with multiple UVMaps
 *   Rotation/Scale on objects are ignored, please rotate/scale in edit mode (the vertex data)
 * TODO:
 *   Animation data
 *
 * Blender Source : https://git.blender.org/gitweb/gitweb.cgi/blender.git/tree/HEAD:/source/blender
 *   - look in blenloader and makesdna folders
 *   - most important to understand DNA : makesdna/intern/dna_genfile.c:init_structDNA()
 *   - also see doc/blender_file_format/mystery_of_the_blend.html
 *
 * @author pquiring
 */

public class GL_BLEND {
  private byte data[];
  private int datapos;

  private boolean x64;  //64bit file format (else 32bit)
  private boolean le;  //little endian file format (else big endian)

  private GLModel model;
  private GLObject obj;

  private float org[] = new float[3];

  private HashMap<Long, Chunk> chunks = new HashMap<Long, Chunk>();

  private static final int ID_ME = 0x454d;  //ME (mesh)
  private static final int ID_OB = 0x424f;  //OB (object)
  private static final int ID_DNA1 = 0x31414e44;  //DNA1

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
/*	private static final int CD_POLYINDEX        = 9; */
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

	private static final int CD_NUMTYPES         = 42;

  //DNA stuff
  private ArrayList<String> names = new ArrayList<String>();  //member names
  private ArrayList<String> types = new ArrayList<String>();  //struct names
  private ArrayList<Short> typelen = new ArrayList<Short>();
  private class struct {
    short typeidx;  //index into types
    short nr;  //# of members
    String name;
    ArrayList<member> members = new ArrayList<member>();
  }
  private class member {
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
      String f[] = m.name.replaceAll("\\]", "").split("\\[");
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

  public GLModel load(String filename) {
    try {
      return loadBlend(new FileInputStream(filename));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  public GLModel load(InputStream is) {
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
  private void readByteArray(byte in[]) {
    System.arraycopy(data, datapos, in, 0, in.length);
    datapos+=in.length;
  }
  private void readPtrArray(long in[]) {
    for(int a=0;a<in.length;a++) {
      in[a] = readptr();
    }
  }
  private void readFloatArray(float in[]) {
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
  private void setData(byte in[]) {
    data = in;
    datapos = 0;
  }
  private class Context {
    byte data[];
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
  private int strlen(byte str[], int offset, int max) {
    for(int a=0;a<max;a++) {
      if (str[a+offset] == 0) return a;
    }
    return max;
  }
  private Chunk findChunkByPtr(long ptr) {
    if (ptr == 0) return null;
    return chunks.get(ptr);
  }
  private class Vertex {
    float xyz[];
  }
  private GLModel loadBlend(InputStream is) throws Exception {
    setData(JF.readAll(is));

    if (data.length < 12) {
      throw new Exception("GL_BLEND:File too small");
    }

    model = new GLModel();

    //load signature (12 bytes) "BLENDER_V100"
    if (!new String(data, 0, 7).equals("BLENDER")) {
      throw new Exception("Not a blender file");
    }
    switch (data[7]) {
      case '-': x64 = true; break;
      case '_': x64 = false; break;
      default:
        throw new Exception("GL_BLEND:Unknown bit size");
    }

    switch (data[8]) {
      case 'v': le = true; break;
      case 'V': le = false; break;
      default:
        throw new Exception("GL_BLEND:Unknown Endianness");
    }

    String version = new String(data, 9, 3);
//    JFLog.log("Blender file version:" + version);
    int ver = Integer.valueOf(version);
    if (ver < 263) {
      throw new Exception("Error:Blender file too old, can not read.");
    }

    datapos = 12;  //skip main header

    //first phase - read raw chunks
    while (!eof()) {
      Chunk chunk = new Chunk();
      chunk.read();
      if (chunks.get(chunk.ptr) != null) {
        JFLog.log("Warning:GL_BLEND:Found two chunks with same pointer! Ignoring old chunk:ptr=" + Long.toString(chunk.ptr, 16) + ",id=" + Integer.toString(chunk.id, 16) + ",len=" + chunk.len);
      }
      chunks.put(chunk.ptr, chunk);
    }

    int chunkCnt = chunks.size();
    Chunk chunkArray[] = chunks.values().toArray(new Chunk[chunkCnt]);
    Chunk raw;

    //2nd phase - parse DNA chunk
    for(int i=0;i<chunkCnt;i++) {
      if (chunkArray[i].id == ID_DNA1) {
        raw = chunkArray[i];
        setData(raw.raw);
        //SDNA
        String SDNA = readString(4);
        if (!SDNA.equals("SDNA")) throw new Exception("Bad DNA Struct:SDNA");
        //NAME
        String NAME = readString(4);
        if (!NAME.equals("NAME")) throw new Exception("Bad DNA Struct:NAME");
        int nr_names = readuint32();
        for(int a=0;a<nr_names;a++) {
          String str = readString();
//          JFLog.log("name=" + str);
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
//          JFLog.log("type=" + str);
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
          s.nr = readuint16();
          s.name = types.get(s.typeidx);
//          JFLog.log("struct:" + s.name);
          for(int b=0;b<s.nr;b++) {
            member m = new member();
            m.typelenidx = readuint16();
            m.nameidx = readuint16();
            m.name = names.get(m.nameidx);
            m.typelen = typelen.get(m.typelenidx);
            m.size = calcMemberSize(m);
//            JFLog.log("  member:" + m.name + "=" + m.length);
            s.members.add(m);
          }
          structs.add(s);
        }
        break;
      }
    }

    //3nd phase - now look for objects and piece together chunks
    for(int i=0;i<chunkCnt;i++) {
      if (chunkArray[i].id == ID_OB) {
        ArrayList<Vertex> vertexList = new ArrayList<Vertex>();
        ArrayList<Integer> loopList = new ArrayList<Integer>();
        raw = chunkArray[i];
        setData(raw.raw);
        bObject bObj = new bObject();
        bObj.read();
//        JFLog.log("object.type=" + bObj.type);
        if (bObj.type != 1) continue;  //not a mesh object (could be camera, light, etc.)
        obj = new GLObject();
        model.addObject(obj);
        obj.name = bObj.id.name.substring(2);
//        JFLog.log("object=" + obj.name);
        raw = findChunkByPtr(bObj.data);
        if (raw == null) {
          throw new Exception("GL_BLEND:Unable to find Mesh for Object");
        }
        Mesh mesh = new Mesh();
        setData(raw.raw);
//        JFLog.log("Mesh@" + Integer.toString(raw.fileOffset, 16));
        mesh.read();
        obj.org.x = bObj.loc[0];
        org[0] = bObj.loc[0];
        obj.org.y = bObj.loc[1];
        org[1] = bObj.loc[1];
        obj.org.z = bObj.loc[2];
        org[2] = bObj.loc[2];
        //find mvert
        raw = findChunkByPtr(mesh.mvert);
        if (raw == null) {
          throw new Exception("GL_BLEND:Unable to find MVert for Mesh");
        }
        setData(raw.raw);
        for(int a=0;a<raw.nr;a++) {
          MVert mvert = new MVert();
          mvert.read();
//          obj.addVertex(mvert.co);
          Vertex v = new Vertex();
          v.xyz = mvert.v;
          vertexList.add(v);
        }
        //find mloop
        raw = findChunkByPtr(mesh.mloop);
        if (raw == null) {
          throw new Exception("GL_BLEND:Unable to find MLoop for Mesh");
        }
        setData(raw.raw);
        for(int a=0;a<raw.nr;a++) {
          MLoop mloop = new MLoop();
          mloop.read();
          loopList.add(mloop.v);
        }
        //find mloopuv
/*  //use the UVMaps in the CustomData instead - this is only the active one
        raw = findChunkByPtr(mesh.mloopuv);
        if (raw == null) {
          throw new Exception("GL_BLEND:Unable to find MLoopUV for Mesh");
        }
        setData(raw.raw);
        JFLog.log("MLoopUV:nr=" + raw.nr);
        for(int a=0;a<raw.nr;a++) {
          MLoopUV mloopuv = new MLoopUV();
          mloopuv.read();
        }
*/
        //find mpoly
        raw = findChunkByPtr(mesh.mpoly);
        if (raw == null) {
          throw new Exception("GL_BLEND:Unable to find MPoly for Mesh");
        }
        setData(raw.raw);
//TODO : calc which vertex needed to be dup'ed for each unique uv value (Blender does this in their 3ds export script)
        int type = -1;
        int pcnt = -1;
        int vidx = 0;
        //MPoly = faces
        for(int a=0;a<raw.nr;a++) {
          MPoly mpoly = new MPoly();
          mpoly.read();
          switch (mpoly.totloop) {
            case 3:
              if (type == GL.GL_QUADS) {
                throw new Exception("GL_BLEND:Mixed QUADS/TRIANGLES not supported");
              }
              type = GL.GL_TRIANGLES;
              pcnt = 3;
              break;
            case 4:
              if (type == GL.GL_TRIANGLES) {
                throw new Exception("GL_BLEND:Mixed QUADS/TRIANGLES not supported");
              }
              type = GL.GL_QUADS;
              pcnt = 4;
              break;
            default:
              throw new Exception("GL_BLEND:Polygon not supported:nr=" + mpoly.totloop);
          }
          int loopidx = mpoly.loopstart;
          for(int p=0;p<pcnt;p++) {
            int idx = loopList.get(loopidx++);
            obj.addVertex(vertexList.get(idx).xyz);
            obj.addPoly(new int[] {vidx++});
          }
        }
        obj.type = type;
        //find customdata types
        readLayer(mesh.vdata.layers, "vdata");
        readLayer(mesh.edata.layers, "edata");
        readLayer(mesh.fdata.layers, "fdata");
        readLayer(mesh.pdata.layers, "pdata");
        readLayer(mesh.ldata.layers, "ldata");
      }
    }

    return model;
  }
  private void readLayer(long layers, String name) throws Exception {
    if (layers == 0) return;
//    JFLog.log(name + ".layers=" + Long.toString(layers, 16));
    Chunk raw = findChunkByPtr(layers);
    if (raw == null) {
      throw new Exception("GL_BLEND:Unable to find " + name + ".layers for Mesh");
    }
    setData(raw.raw);
    for(int a=0;a<raw.nr;a++) {
      CustomDataLayer layer = new CustomDataLayer();
      layer.read();
      String layer_name = layer.name;
      if (layer.data == 0) continue;
      Chunk layer_data = findChunkByPtr(layer.data);
      if (layer_data == null) {
        throw new Exception("GL_BLEND:Unable to find " + name + ".layers.data for Mesh");
      }
      Context ctx = pushData();
      setData(layer_data.raw);
//      JFLog.log("layer.type==" + layer.type + ",a=" + a);
      switch (layer.type) {
        case CD_MTEXPOLY: {  //15
          //NOTE:There is a MTexPoly per face, I only read the first
          MTexPoly tex = new MTexPoly();
          tex.read();
          Chunk imageChunk = findChunkByPtr(tex.tpage);
          if (imageChunk == null) {
            throw new Exception("GL_BLEND:No texture found for UVMap:" + a);
          }
          setData(imageChunk.raw);
          Image image = new Image();
          image.read();
          GLUVMap map;
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
          map.textureIndex = tidx;
          map.name = layer_name;
//          JFLog.log("texpoly=" + map.name);
          break;
        }
        case CD_MLOOPUV: { //16
          //There is a UV per face per vertex
          if (a >= obj.getUVMaps()) {
            obj.createUVMap();
          }
//          JFLog.log("loopuv.nr=" + layer_data.nr);
          for(int b=0;b<layer_data.nr;b++) {
            MLoopUV uv = new MLoopUV();
            uv.read();
            uv.uv[1] = 1.0f - uv.uv[1];  //invert V(y)
            obj.addText(uv.uv, a);
          }
          break;
        }
      }
      popData(ctx);
    }
  }
  private class Chunk {
    int id;
    int len;
    long ptr;  //the actual memory address of this chunk when it was saved to disk !!!
    int SDNAnr;
    int nr;  //array count of struct
    byte raw[];

    int fileOffset;
    void read() {
      id = readuint32();
      len = readuint32();
      ptr = readptr();
      SDNAnr = readuint32();
      nr = readuint32();
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
      for(int a=0;a<s.nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("name[66]")) {
          name = readString(m.size);
        }
        else {
          datapos += m.size;
        }
      }
    }
  }
  private class bObject {
    ID id = new ID();
    int type;
    long data;  // -> Mesh
    float loc[] = new float[3];  //Location (aka Origin)
    void read() throws Exception {
      struct s = getStruct("Object");
      for(int a=0;a<s.nr;a++) {
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
    void read(String name) throws Exception {
      struct s = getStruct("CustomData");
      for(int a=0;a<s.nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("*layers")) {
          layers = readptr();
        }
        else {
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
      for(int a=0;a<s.nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("type")) {
          type = readuint32();
        }
        else if (m.name.equals("name[64]")) {
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
    CustomData vdata = new CustomData();
    CustomData edata = new CustomData();
    CustomData fdata = new CustomData();
    CustomData pdata = new CustomData();
    CustomData ldata = new CustomData();
    void read() throws Exception {
      struct s = getStruct("Mesh");
      for(int a=0;a<s.nr;a++) {
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
        else if (m.name.equals("vdata")) {
          vdata.read("vdata");
        }
        else if (m.name.equals("edata")) {
          edata.read("edata");
        }
        else if (m.name.equals("fdata")) {
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
      for(int a=0;a<s.nr;a++) {
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
      for(int a=0;a<s.nr;a++) {
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
      for(int a=0;a<s.nr;a++) {
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
      for(int a=0;a<s.nr;a++) {
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
      for(int a=0;a<s.nr;a++) {
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
      for(int a=0;a<s.nr;a++) {
        member m = s.members.get(a);
        if (m.name.equals("name[1024]")) {
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
