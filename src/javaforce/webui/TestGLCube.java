package javaforce.webui;

/** Test WebUI / WebGL.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.gl.*;

public class TestGLCube implements WebUIHandler {
  public TestGLCube() {
  }

  public static void main(String args[]) {
    new WebUIServer().start(new TestGLCube(), 8080, false);
  }

  public void clientDisconnected(WebUIClient client) {
  }

  public byte[] getResource(String url) {
    //TODO : return static images, etc needed by webpage
    return null;
  }

  private String vs =
"  attribute vec3 aVertexPosition;\n" +
"  attribute vec2 aTextureCoord;\n" +
"\n" +
"  uniform mat4 uPMatrix;\n" +
"  uniform mat4 uMVMatrix;\n" +
"\n" +
"  varying highp vec2 vTextureCoord;\n" +
"\n" +
"  void main(void) {\n" +
"    gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);\n" +
"    vTextureCoord = aTextureCoord;\n" +
"  }\n";

  private String fs =
"  varying highp vec2 vTextureCoord;\n" +
"\n" +
"  uniform sampler2D uSampler;" +
"\n" +
"  void main(void) {\n" +
"    gl_FragColor = texture2D(uSampler, vec2(vTextureCoord.s, vTextureCoord.t));\n" +
"  }\n";

  private float[] vertices =
  {
     1.0f,-1.0f, 0.0f,
    -1.0f,-1.0f, 0.0f,
     1.0f, 1.0f, 0.0f,
    -1.0f, 1.0f, 0.0f,
  };

  private float[] textCoords =
  {  // Front
    1.0f, 1.0f,
    0.0f, 1.0f,
    1.0f, 0.0f,
    0.0f, 0.0f,
    // Back
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f,
    0.0f, 1.0f,
    // Top
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f,
    0.0f, 1.0f,
    // Bottom
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f,
    0.0f, 1.0f,
    // Right
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f,
    0.0f,  1.0f,
    // Left
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f,
    0.0f, 1.0f
  };

  public byte[] convertFloatArray(float m[]) {
    byte data[] = new byte[m.length * 4];
    int off = 0;
    for(int a=0;a<m.length;a++) {
      javaforce.LE.setuint32(data, off, Float.floatToIntBits(m[a]));
      off += 4;
    }
    return data;
  }

  public byte[] convertIntArray(int i[]) {
    byte data[] = new byte[i.length * 4];
    int offset = 0;
    for(int a=0;a<i.length;a++) {
      int num = i[a];
      data[offset+2] = (byte)(num & 0xff);
      num >>= 8;
      data[offset+1] = (byte)(num & 0xff);
      num >>= 8;
      data[offset+0] = (byte)(num & 0xff);
      num >>= 8;
      data[offset+3] = (byte)(num & 0xff);
      offset += 4;
    }
    return data;
  }

  public Panel getRootPanel(WebUIClient client) {
    Panel panel = new Panel() {
      public void onLoaded(String args[]) {
        GLMatrix pMatrix = new GLMatrix();
        pMatrix.perspective(45f, 640.0f/480.0f, 0.1f, 100.0f);
        GLMatrix mMatrix = new GLMatrix();
        mMatrix.addTranslate(0, 0, -4.0f);
        Canvas canvas = (Canvas)getProperty("canvas");
        //init gl resources
        canvas.sendEvent("initwebgl", null);
        canvas.sendEvent("loadvs", new String[] {"src=" + vs});
        canvas.sendEvent("loadfs", new String[] {"src=" + fs});
        canvas.sendEvent("link", null);
        canvas.sendEvent("getuniform", new String[] {"idx=0", "name=uPMatrix"});
        canvas.sendEvent("getuniform", new String[] {"idx=1", "name=uMVMatrix"});
        canvas.sendEvent("getattrib", new String[] {"idx=0", "name=aVertexPosition"});
        canvas.sendEvent("getattrib", new String[] {"idx=1", "name=aTextureCoord"});
        canvas.sendData(convertFloatArray(pMatrix.m));
        canvas.sendEvent("matrix", new String[] {"idx=0"});
        canvas.sendData(convertFloatArray(mMatrix.m));
        canvas.sendEvent("matrix", new String[] {"idx=1"});
        canvas.sendData(convertFloatArray(vertices));
        canvas.sendEvent("buffer", new String[] {"idx=0"});
        canvas.sendData(convertFloatArray(textCoords));
        canvas.sendEvent("buffer", new String[] {"idx=1"});
        JFImage img = new JFImage();
        img.loadPNG("data\\opengl.png");
        canvas.sendData(convertIntArray(img.getBuffer()));
        canvas.sendEvent("loadt", new String[] {"idx=0", "x=" + img.getWidth(), "y=" + img.getHeight()});
        //setup rendering pipeline
        canvas.sendEvent("r_matrix", new String[] {"idx=0", "uidx=0", "midx=0"});
        canvas.sendEvent("r_matrix", new String[] {"idx=1", "uidx=1", "midx=1"});
        canvas.sendEvent("r_attrib", new String[] {"idx=2", "aidx=0", "bufidx=0", "cnt=3"});
        canvas.sendEvent("r_attrib", new String[] {"idx=3", "aidx=1", "bufidx=1", "cnt=2"});
        canvas.sendEvent("r_bindt", new String[] {"idx=4", "tidx=0"});
        canvas.sendEvent("r_drawArrays", new String[] {"idx=5", "type=" + GL.GL_TRIANGLE_STRIP, "cnt=4"});
      }
    };

    Canvas canvas = new Canvas();
    canvas.setSize(640, 480);
    panel.add(canvas);

    panel.setProperty("canvas", canvas);

    return panel;
  }
}
