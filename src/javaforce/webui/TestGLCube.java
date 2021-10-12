package javaforce.webui;

/** Test WebUI / WebGL.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.gl.*;
import javaforce.awt.*;

public class TestGLCube implements WebUIHandler {
  public TestGLCube() {
  }

  public static void main(String args[]) {
    new WebUIServer().start(new TestGLCube(), 8080, false);
  }

  public void clientConnected(WebUIClient client) {
    Context context = new Context(client);
    client.setProperty("context", context);
  }
  public void clientDisconnected(WebUIClient client) {
    Context context = (Context)client.getProperty("context");
    context.close();
  }

  public byte[] getResource(String url) {
    //TODO : return static images, etc needed by webpage
    return null;
  }

  private static String vs =
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

  private static String fs =
"  varying highp vec2 vTextureCoord;\n" +
"\n" +
"  uniform sampler2D uSampler;" +
"\n" +
"  void main(void) {\n" +
"    gl_FragColor = texture2D(uSampler, vec2(vTextureCoord.s, vTextureCoord.t));\n" +
"  }\n";

  private static float[] vertices =
  {
     1.0f,-1.0f, 0.0f,
    -1.0f,-1.0f, 0.0f,
     1.0f, 1.0f, 0.0f,
    -1.0f, 1.0f, 0.0f,
  };

  private static float[] textCoords =
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

  public static byte[] convertFloatArray(float m[]) {
    byte data[] = new byte[m.length * 4];
    int off = 0;
    for(int a=0;a<m.length;a++) {
      javaforce.LE.setuint32(data, off, Float.floatToIntBits(m[a]));
      off += 4;
    }
    return data;
  }

  public static byte[] convertIntArray(int i[]) {
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
        Context context = (Context)client.getProperty("context");
        context.send();
      }
    };

    Canvas canvas = new Canvas();
    canvas.setSize(640, 480);
    panel.add(canvas);

    client.setProperty("canvas", canvas);

    return panel;
  }
  public static class Context extends TimerTask {
    public WebUIClient client;
    public Matrix pMatrix, mMatrix;
    public Timer timer;
    public Canvas canvas;

    public Context(WebUIClient client) {
      this.client = client;
    }
    public void run() {
      Canvas canvas = (Canvas)client.getProperty("canvas");
      mMatrix.addRotate(1, 1, 0, 0);
      canvas.sendData(convertFloatArray(mMatrix.m));
      canvas.sendEvent("matrix", new String[] {"idx=1"});
    }
    public void send() {
      pMatrix = new Matrix();
      pMatrix.perspective(45f, 640.0f/480.0f, 0.1f, 100.0f);
      mMatrix = new Matrix();
      mMatrix.addTranslate(0, 0, -4.0f);
      canvas = (Canvas)client.getProperty("canvas");
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
      canvas.sendEvent("array", new String[] {"idx=0"});
      canvas.sendEvent("array", new String[] {"idx=1"});
      canvas.sendEvent("array", new String[] {"idx=2"});
      canvas.sendEvent("r_matrix", new String[] {"idx=0", "uidx=0", "midx=0"});
      canvas.sendEvent("r_matrix", new String[] {"idx=0", "uidx=1", "midx=1"});
      canvas.sendEvent("r_attrib", new String[] {"idx=0", "aidx=0", "bufidx=0", "cnt=3"});
      canvas.sendEvent("r_attrib", new String[] {"idx=1", "aidx=1", "bufidx=1", "cnt=2"});
      canvas.sendEvent("r_bindt", new String[] {"idx=2", "tidx=0"});
      canvas.sendEvent("r_drawArrays", new String[] {"idx=2", "type=" + GL.GL_TRIANGLE_STRIP, "cnt=4"});
      timer = new Timer();
      timer.schedule(this, 100, 100);  //10fps
    }
    public void close() {
      timer.cancel();
    }
  }
}
