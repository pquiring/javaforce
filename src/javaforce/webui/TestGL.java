package javaforce.webui;

/** Test WebUI / WebGL.
 *
 * @author pquiring
 */

import javaforce.webui.event.*;
import javaforce.gl.*;

public class TestGL implements WebUIHandler {
  public Resource img;

  public TestGL() {
  }

  public static void main(String args[]) {
    new WebUIServer().start(new TestGL(), 8080, false);
  }

  public void clientConnected(WebUIClient client) {}
  public void clientDisconnected(WebUIClient client) {}

  public byte[] getResource(String url) {
    //TODO : return static images, etc needed by webpage
    return null;
  }

  private String vs =
"      attribute vec3 aVertexPosition;\n" +
"      attribute vec4 aVertexColor;\n" +
"\n" +
"      uniform mat4 uPMatrix;\n" +
"      uniform mat4 uMVMatrix;\n" +
"      \n" +
"      varying lowp vec4 vColor;\n" +
"\n" +
"      void main(void) {\n" +
"        gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);\n" +
"        vColor = aVertexColor;\n" +
"      }\n";

  private String fs =
"      varying lowp vec4 vColor;\n" +
"\n" +
"      void main(void) {\n" +
"        gl_FragColor = vColor;\n" +
"      }\n";

  private float[] colors =
  {1.0f, 1.0f, 1.0f, 1.0f,  // white
 1.0f, 0.0f, 0.0f, 1.0f, // red
 0.0f, 1.0f, 0.0f, 1.0f, // green
 0.0f, 0.0f, 1.0f, 1.0f}; // blue

  private float[] vertices =
{1.0f, 1.0f, 0.0f,
-1.0f, 1.0f, 0.0f,
 1.0f,-1.0f, 0.0f,
-1.0f,-1.0f, 0.0f};

  public byte[] convertFloatArray(float m[]) {
    byte data[] = new byte[16 * 4];
    int off = 0;
    for(int a=0;a<m.length;a++) {
      javaforce.LE.setuint32(data, off, Float.floatToIntBits(m[a]));
      off += 4;
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
        canvas.sendEvent("getattrib", new String[] {"idx=1", "name=aVertexColor"});
        canvas.sendData(convertFloatArray(pMatrix.m));
        canvas.sendEvent("matrix", new String[] {"idx=0"});
        canvas.sendData(convertFloatArray(mMatrix.m));
        canvas.sendEvent("matrix", new String[] {"idx=1"});
        canvas.sendData(convertFloatArray(vertices));
        canvas.sendEvent("buffer", new String[] {"idx=0"});
        canvas.sendData(convertFloatArray(colors));
        canvas.sendEvent("buffer", new String[] {"idx=1"});
        //setup rendering pipeline
        canvas.sendEvent("r_matrix", new String[] {"idx=0", "uidx=0", "midx=0"});
        canvas.sendEvent("r_matrix", new String[] {"idx=1", "uidx=1", "midx=1"});
        canvas.sendEvent("r_attrib", new String[] {"idx=2", "aidx=0", "bufidx=0", "cnt=3"});
        canvas.sendEvent("r_attrib", new String[] {"idx=3", "aidx=1", "bufidx=1", "cnt=4"});
        canvas.sendEvent("r_drawArrays", new String[] {"idx=4", "type=" + GL.GL_TRIANGLE_STRIP, "cnt=4"});
      }
    };

    Canvas canvas = new Canvas();
    canvas.setSize(640, 480);
    panel.add(canvas);

    panel.setProperty("canvas", canvas);

    return panel;
  }
}
