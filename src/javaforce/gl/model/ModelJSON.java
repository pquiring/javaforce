package javaforce.gl.model;

/** Loads JSON Model.
 *
 * see https://minecraft.fandom.com/wiki/Tutorials/Models
 *
 * Notes:
 *   - xyz coords and uv texture coords are based on pixels
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import static javaforce.JSON.Element;
import javaforce.gl.*;

public class ModelJSON implements Model_IO {
  private static boolean debug = false;
  public float scale = 16.0f;
  private float tw, th;
  private float width, height, depth;
  private boolean mirror;
  public ModelJSON() {
    this.scale = 16f;
  }
  public ModelJSON(float scale) {
    this.scale = scale;
  }
  public Model load(String filename) {
    try {
      InputStream is = new FileInputStream(filename);
      Model model = load(is);
      is.close();
      return model;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  public Model load(InputStream is) {
    Model model = new Model();
    int mapidx = 0;
    try {
      String json = new String(is.readAllBytes());
      JSON.debug = debug;
      Element root = JSON.parse(json);
      if (debug) {
        root.print();
      }

      for(Element child : root.children) {
        if (child.key.startsWith("geometry.")) {
          Element geo = child;
          //TODO : find entity based on name
          String ename = geo.key.substring(9);
          int idx = ename.indexOf('.');
          if (idx != -1) {
            ename = ename.substring(0, idx);
          }
          Element etw = geo.getChild("texturewidth");
          tw = getValue(etw.value);
          Element eth = geo.getChild("textureheight");
          th = getValue(eth.value);
          Element bones = geo.getChild("bones");
          for(Element box : bones.children) {
            Element name = box.getChild("name");
            Element pvt = box.getChild("pivot");
            float[] pvts = getValues(pvt);
            //Element rot = box.getChild("rotation");  //optional
            //float[] rots = getValues(rot);
            Element cubes = box.getChild("cubes");
            if (cubes == null) continue;
            for(Element cube : cubes.children) {
              Element org = cube.getChild("origin");
              //origin is lowest coord of cube in all 3 dims
              float[] orgs = getValues(org);
              Element size = cube.getChild("size");
              float[] sizes = getValues(size);
              Element uv = cube.getChild("uv");
              float[] uvs = getValues(uv);
              //text coords have 0,0 at top left corner
              float u = uvs[0];
              float v = uvs[1];
              Element _inflate = cube.getChild("inflate");
              if (_inflate != null) {
                float inflate = getValue(_inflate.value) / scale;
                float inflate_2 = inflate * 2.0f;
                sizes[0] += inflate_2;
                sizes[1] += inflate_2;
                sizes[2] += inflate_2;
                orgs[0] -= inflate;
                orgs[1] -= inflate;
                orgs[2] -= inflate;
              }
              Element _mirror = cube.getChild("mirror");
              mirror = _mirror != null && _mirror.value.equals("true");
              float x = orgs[0];
              float y = orgs[1];
              float z = orgs[2];
              width = sizes[0];  //x
              height = sizes[1];  //y
              depth = sizes[2];  //z
              {
                Object3 obj = new Object3();
                obj.type = GL.GL_QUADS;
                UVMap map = obj.createUVMap();
                map.name = "map-" + mapidx++;
                model.addObject(obj);
                //set origin (pivot)
                obj.org.x = pvts[0] / scale;
                obj.org.y = pvts[1] / scale;
                obj.org.z = pvts[2] / scale;
                obj.name = name.value;
                //each face starts at bottom left corner of texture
                //start @ origin (x,y,z)
                //uv starts high, bring down to first face (E)
                v += depth + height;

                //move to E
                x += width;
                z += depth;

                if (mirror)
                  addFaceE(obj, x, y, z, u + depth + width, v);
                else
                  addFaceE(obj, x, y, z, u, v);

                //move to N
                z -= depth;
                u += depth;

                addFaceN(obj, x, y, z, u, v);
                if (mirror) mirror(obj);

                //move to W
                x -= width;
                u += width;

                if (mirror)
                  addFaceW(obj, x, y, z, u - depth - width, v);
                else
                  addFaceW(obj, x, y, z, u, v);

                //move to S
                z += depth;
                u += depth;

                addFaceS(obj, x, y, z, u, v);
                if (mirror) mirror(obj);

                //move to B (two steps)
                v -= height;
                u -= depth;

                addFaceB(obj, x, y, z, u, v);
                if (mirror) mirror(obj);

                //move to A
                y += height;
                u -= width;

                addFaceA(obj, x, y, z, u, v);
                if (mirror) mirror(obj);
              }
            }
          }
        }
      }
      return model;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  private float[] xyz = new float[3];
  private float[] uv = new float[2];
  private int[] pts = new int[4];

  private void addFaceW(Object3 obj, float x, float y, float z, float u, float v) {
    int vidx = obj.getVertexCount();

    //add 4 vertex in CCW order starting at origin
    addVertex(obj, x, y, z, u, v);

    z += depth;
    u += depth;

    addVertex(obj, x, y, z, u, v);

    y += height;
    v -= height;

    addVertex(obj, x, y, z, u, v);

    z -= depth;
    u -= depth;

    addVertex(obj, x, y, z, u, v);

    for(int a=0;a<4;a++) {
      pts[a] = vidx++;
    }
    obj.addPoly(pts);
  }

  private void addFaceS(Object3 obj, float x, float y, float z, float u, float v) {
    int vidx = obj.getVertexCount();

    //add 4 vertex in CCW order
    addVertex(obj, x, y, z, u, v);

    x += width;
    u += width;

    addVertex(obj, x, y, z, u, v);

    y += height;
    v -= height;

    addVertex(obj, x, y, z, u, v);

    x -= width;
    u -= width;

    addVertex(obj, x, y, z, u, v);

    for(int a=0;a<4;a++) {
      pts[a] = vidx++;
    }
    obj.addPoly(pts);
  }

  private void addFaceE(Object3 obj, float x, float y, float z, float u, float v) {
    int vidx = obj.getVertexCount();

    //add 4 vertex in CCW order
    addVertex(obj, x, y, z, u, v);

    z -= depth;
    u += depth;

    addVertex(obj, x, y, z, u, v);

    y += height;
    v -= height;

    addVertex(obj, x, y, z, u, v);

    z += depth;
    u -= depth;

    addVertex(obj, x, y, z, u, v);

    for(int a=0;a<4;a++) {
      pts[a] = vidx++;
    }
    obj.addPoly(pts);
  }

  private void addFaceN(Object3 obj, float x, float y, float z, float u, float v) {
    int vidx = obj.getVertexCount();

    //add 4 vertex in CCW order
    addVertex(obj, x, y, z, u, v);

    x -= width;
    u += width;

    addVertex(obj, x, y, z, u, v);

    y += height;
    v -= height;

    addVertex(obj, x, y, z, u, v);

    x += width;
    u -= width;

    addVertex(obj, x, y, z, u, v);

    for(int a=0;a<4;a++) {
      pts[a] = vidx++;
    }
    obj.addPoly(pts);
  }

  //TODO : not sure if this needs to be mirrored in the x axis
  private void addFaceB(Object3 obj, float x, float y, float z, float u, float v) {
    int vidx = obj.getVertexCount();

    //add 4 vertex in CCW order
    addVertex(obj, x, y, z, u, v);

    z -= depth;
    v -= depth;

    addVertex(obj, x, y, z, u, v);

    x += width;
    u += width;

    addVertex(obj, x, y, z, u, v);

    z += depth;
    v += depth;

    addVertex(obj, x, y, z, u, v);

    for(int a=0;a<4;a++) {
      pts[a] = vidx++;
    }
    obj.addPoly(pts);
  }

  private void addFaceA(Object3 obj, float x, float y, float z, float u, float v) {
    int vidx = obj.getVertexCount();

    //add 4 vertex in CCW order
    addVertex(obj, x, y, z, u, v);

    x += width;
    u += width;

    addVertex(obj, x, y, z, u, v);

    z -= depth;
    v -= depth;

    addVertex(obj, x, y, z, u, v);

    x -= width;
    u -= width;

    addVertex(obj, x, y, z, u, v);

    for(int a=0;a<4;a++) {
      pts[a] = vidx++;
    }
    obj.addPoly(pts);
  }

  private void mirror(Object3 obj) {
    //mirror u text coord in vertex 1 <-> 2 and 3 <-> 4
    float[] uv = obj.getUVMap(0).uvl.getBuffer();
    int idx = obj.getVertexCount();
    int v1 = (idx - 4) * 2;
    int v2 = (idx - 3) * 2;
    int v3 = (idx - 2) * 2;
    int v4 = (idx - 1) * 2;
    float tmp;
    //swap 1 <-> 2
    tmp = uv[v1];
    uv[v1] = uv[v2];
    uv[v2] = tmp;
    //swap 3 <-> 4
    tmp = uv[v3];
    uv[v3] = uv[v4];
    uv[v4] = tmp;
  }

  private void addVertex(Object3 obj, float x, float y, float z, float u, float v) {
    //coords are based on pixels, the org game is based on 8x textures
    xyz[0] = x / scale;
    xyz[1] = y / scale;
    xyz[2] = z / scale;
    //scale uv : 0-1
    uv[0] = u / tw;
    uv[1] = v / th;
    obj.addVertex(xyz, uv);
  }

  private float getValue(String e) {
    return Float.valueOf(e);
  }

  private float[] getValues(Element array) {
    int cnt = array.getChildCount();
    float[] values = new float[cnt];
    for(int i=0;i<cnt;i++) {
      Element child = array.getChild(i);
      values[i] = Float.valueOf(child.value);
    }
    return values;
  }

  public boolean save(Model model, OutputStream os) {
    return false;
  }
}

/*

JSON format:

{
  "format_version": "",
  "geometry_*": {
    "texturewidth": "*",
    "textureheight": "*",
    "bones": [
      {
        "name": "*",
        "pivot": [ x, y, z ],
        "rotation": [ x, y, z ],
        "cubes": [
          {
            "origin": [ x, y, z ],
            "size": [ x, y, z ],
            "uv": [ u ,v ]
          }
        ]
      },
      ...
    ]
  }
}


*/
