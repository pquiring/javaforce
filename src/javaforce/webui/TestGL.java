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
        WebUIClient client = getClient();
        //init gl resources
        client.sendEvent(canvas.id, "initwebgl", null);
        client.sendEvent(canvas.id, "loadvs", new String[] {"src=" + vs});
        client.sendEvent(canvas.id, "loadfs", new String[] {"src=" + fs});
        client.sendEvent(canvas.id, "link", null);
        client.sendEvent(canvas.id, "getuniform", new String[] {"idx=0", "name=uPMatrix"});
        client.sendEvent(canvas.id, "getuniform", new String[] {"idx=1", "name=uMVMatrix"});
        client.sendEvent(canvas.id, "getattrib", new String[] {"idx=0", "name=aVertexPosition"});
        client.sendEvent(canvas.id, "getattrib", new String[] {"idx=1", "name=aVertexColor"});
        client.sendData(convertFloatArray(pMatrix.m));
        client.sendEvent(canvas.id, "matrix", new String[] {"idx=0"});
        client.sendData(convertFloatArray(mMatrix.m));
        client.sendEvent(canvas.id, "matrix", new String[] {"idx=1"});
        client.sendData(convertFloatArray(vertices));
        client.sendEvent(canvas.id, "buffer", new String[] {"idx=0"});
        client.sendData(convertFloatArray(colors));
        client.sendEvent(canvas.id, "buffer", new String[] {"idx=1"});
        //setup rendering pipeline
        client.sendEvent(canvas.id, "r_matrix", new String[] {"idx=0", "uidx=0", "midx=0"});
        client.sendEvent(canvas.id, "r_matrix", new String[] {"idx=1", "uidx=1", "midx=1"});
        client.sendEvent(canvas.id, "r_attrib", new String[] {"idx=2", "aidx=0", "bufidx=0", "cnt=3"});
        client.sendEvent(canvas.id, "r_attrib", new String[] {"idx=3", "aidx=1", "bufidx=1", "cnt=4"});
        client.sendEvent(canvas.id, "r_drawArrays", new String[] {"idx=4", "type=" + GL.GL_TRIANGLE_STRIP, "cnt=4"});
      }
    };

    Canvas canvas = new Canvas();
    canvas.setWidth("640px");
    canvas.setHeight("480px");
    panel.add(canvas);

    panel.setProperty("canvas", canvas);

    return panel;
  }
}
