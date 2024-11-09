package javaforce.gl.model;

/** Wavefront .obj file.
 *
 * https://en.wikipedia.org/wiki/Wavefront_.obj_file
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.gl.*;

public class ModelOBJ {
  private static boolean debug = false;
  public Model load(String filename) {
    try {
      InputStream is = new FileInputStream(filename);
      Model model = loadObj(is);
      is.close();
      return model;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  public Model load(InputStream is) {
    try {
      return loadObj(is);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  private Model loadObj(InputStream is) {
    try {
      Model model = new Model();
      Object3 object = null;
      UVMap map = null;
      float[] xyz = new float[3];
      float[] uv = new float[2];
      int[] pts = new int[1];
      int pidx = 0;
      ArrayList<Vertex> verts = new ArrayList<>();
      ArrayList<UV> uvs = new ArrayList<>();
      byte[] data = is.readAllBytes();
      String[] lns = new String(data).replaceAll("\r", "").split("\n");
      //indexes are one based and on the entire file (not per object)
      verts.add(new Vertex());  //zero index not used
      uvs.add(new UV());  //zero index not used
      for(String ln : lns) {
        ln = ln.trim();
        if (ln.startsWith("#")) continue;  //comment
        String[] fs = ln.split("[ ]+");  //space one or more times
        if (fs.length == 0) continue;
        switch (fs[0]) {
          case "o":  //object
            object = new Object3();
            object.name = fs[1];
            object.type = GL.GL_QUADS;  //assume quads?
            pidx = 0;
            model.addObject(object);
            map = object.createUVMap();
            map.name = "normal";
            if (debug) JFLog.log("obj:object=" + object.name);
            break;
          case "v":  //vertex : v # # #
            xyz[0] = Float.valueOf(fs[1]);
            xyz[1] = Float.valueOf(fs[2]);
            xyz[2] = Float.valueOf(fs[3]);
            verts.add(new Vertex(xyz));
            if (debug) JFLog.log("obj:vert=" + xyz[0] + "," + xyz[1] + "," + xyz[2]);
            break;
          case "vt":  //vert text coords : vt u v
            uv[0] = Float.valueOf(fs[1]);
            uv[1] = Float.valueOf(fs[2]);
            uvs.add(new UV(uv));
            if (debug) JFLog.log("obj:uv=" + uv[0] + "," + uv[1]);
            break;
          case "f":  //face : f v/vt/vn
            if (debug) JFLog.log("obj:face.length=" + (fs.length-1) );
            for(int i=1;i<fs.length;i++) {
              String[] idxs = fs[i].split("/");
              int vidx = Integer.valueOf(idxs[0]);
              int vtidx = Integer.valueOf(idxs[1]);
              pts[0] = pidx++;
              if (debug) JFLog.log("obj:face.idxs=" + vidx + "," + vtidx);
              object.addVertex(verts.get(vidx).xyz);
              object.addText(uvs.get(vtidx).uv);
              object.addPoly(pts);
            }
            break;
        }
      }
      return model;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
  private static class Vertex {
    float[] xyz = new float[3];
    public Vertex() {}
    public Vertex(float[] in) {
      xyz[0] = in[0];
      xyz[1] = in[1];
      xyz[2] = in[2];
    }
  }
  private static class UV {
    float[] uv = new float[2];
    public UV() {}
    public UV(float[] in) {
      uv[0] = in[0];
      uv[1] = in[1];
    }
  }
}
