package javaforce.ui;

/** UIRender - main render loop.
 *
 * @author pquiring
 */

import javaforce.gl.*;
import static javaforce.gl.GL.*;

public class UIRender {

  private Scene scene;
  public Matrix m_ortho;  //perspective matrix
  public Matrix m_view;  //view matrix
  public Matrix m_model;  //model matrix

  public float s = 1.0f;

  public void setContext() {
    glUseProgram(scene.program);
    glUniformMatrix4fv(scene.mpu, 1, GL.GL_FALSE, m_ortho.m);  //perspective matrix
    glUniformMatrix4fv(scene.mvu, 1, GL.GL_FALSE, m_view.m);  //view matrix
    glUniformMatrix4fv(scene.mmu, 1, GL.GL_FALSE, m_model.m);  //model matrix
  }

  public void run() {
    run(-1);
  }

  public void run(int wait) {
    scene = new Scene();
    m_ortho = new Matrix();
    //+x right : +y up : +z toward user
    m_ortho.ortho(0, 1, 0, 1, 0.1f, 10f);  //left right bottom top near far
    m_view = new Matrix();
//    m_view.setIdentity();
    m_model = new Matrix();
//    m_model.setIdentity();
    scene.init(VertexShader.source, FragmentShader.source);
    while (true) {
      setContext();
      Window[] windowList = Window.getWindows();
      if (windowList.length == 0) break;
      for(Window window : windowList) {
        try {
          window.render(scene);
        } catch (Exception e) {
          e.printStackTrace();
        }
        Canvas[] canvasList = window.getCanvasList();
        for(Canvas c : canvasList) {
          glViewport(c.pos.x, c.pos.y, c.size.width, c.size.height);
          try {
            c.render();
          } catch (Exception e) {
            e.printStackTrace();
          }
          setContext();
        }
      }
      Window.pollEvents(wait);
    }
  }
}
