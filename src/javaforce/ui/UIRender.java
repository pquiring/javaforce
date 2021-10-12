package javaforce.ui;

/** UIRender - main render loop.
 *
 * @author pquiring
 */

import javaforce.gl.*;
import static javaforce.gl.GL.*;

public class UIRender {

  private GLScene scene;
  public GLMatrix m_ortho;  //perspective matrix
  public GLMatrix m_view;  //view matrix
  public GLMatrix m_model;  //model matrix

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
    scene = new GLScene();
    m_ortho = new GLMatrix();
    //+x right : +y up : +z toward user
    m_ortho.ortho(0, 1, 0, 1, 0.1f, 10f);  //left right bottom top near far
    m_view = new GLMatrix();
//    m_view.setIdentity();
    m_model = new GLMatrix();
//    m_model.setIdentity();
    scene.init(GLVertexShader.source, GLFragmentShader.source);
    while (true) {
      setContext();
      Window[] windowList = Window.getWindows();
      if (windowList.length == 0) break;
      for(Window window : windowList) {
        window.render(scene);
        Canvas[] canvasList = window.getCanvasList();
        for(Canvas c : canvasList) {
          c.render();
          setContext();
        }
      }
      Window.pollEvents(wait);
    }
  }
}
