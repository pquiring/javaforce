/**
 * Created : July 4, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class TrackPanel extends javax.swing.JPanel {

  /**
   * Creates new form TrackPanel
   */
  public TrackPanel(ProjectPanel project) {
    initComponents();
    this.project = project;
    initDND();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    addMouseListener(new java.awt.event.MouseAdapter() {
      public void mousePressed(java.awt.event.MouseEvent evt) {
        formMousePressed(evt);
      }
      public void mouseReleased(java.awt.event.MouseEvent evt) {
        formMouseReleased(evt);
      }
    });
    addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      public void mouseDragged(java.awt.event.MouseEvent evt) {
        formMouseDragged(evt);
      }
      public void mouseMoved(java.awt.event.MouseEvent evt) {
        formMouseMoved(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 619, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 87, Short.MAX_VALUE)
    );
  }// </editor-fold>//GEN-END:initComponents

  private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
    project.selectTrack(this);
    selectedOffset = project.offset + evt.getX() / 16 * project.scale;
    repaint();
  }//GEN-LAST:event_formMousePressed

  private void formMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseDragged
    project.drag(evt, this);
  }//GEN-LAST:event_formMouseDragged

  private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
    if (project.dragTrack != null) {
      project.stopDrag();
    }
  }//GEN-LAST:event_formMouseReleased

  private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
  }//GEN-LAST:event_formMouseMoved

  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables

  private ProjectPanel project;
  public boolean selected = false;
  public int selectedOffset = -1;
  public ArrayList<Element> list = new ArrayList<Element>();

  public void paint(Graphics g) {
    g.setColor(Color.GRAY);
    g.fillRect(0,0,getWidth(),getHeight());
    if (selected)
      g.setColor(Color.YELLOW);
    else
      g.setColor(Color.BLUE);
    g.drawRect(0,0,getWidth()-1,getHeight()-1);
    int offset = project.offset;
    int px = 0;
    int width = getWidth();
    g.setColor(Color.BLACK);
    while (px < width) {
      if (selectedOffset >= offset && selectedOffset <= (offset + project.scale-1)) {
        if (project.dragTrack == this) {
          g.setColor(Color.GREEN);
        } else {
          g.setColor(Color.RED);
        }
      } else {
        g.setColor(Color.BLACK);
      }
      g.drawRect(px,1,15,61);
      drawElements(g, px, offset, project.scale);
      px += 16;
      offset += project.scale;
    }
  }

  public void drawElements(Graphics g, int px, int offset, int length) {
    int len = list.size();
    int d1 = offset;
    int d2 = offset + length - 1;
    for(int a=0;a<len;a++) {
      Element e = list.get(a);
      int e1 = e.offset;
      int e2 = e.offset + e.length - 1;
      if ((d1 >= e1 && d1 <= e2) || (d2 >= e1 && d2 <= e2) || (d1 <= e1 && d2 >= e2))
      {
        int off = offset - e.offset;
        if (off < 0) off = 0;
        drawElement(g, e, px, off);
      }
    }
  }

  public void drawElement(Graphics g, Element e, int px, int offset) {
    g.drawImage(e.getPreview(offset, project.scale), px, 2, null);
  }

  public Dimension getPreferredSize() {
    return new Dimension(project.getTracksWidth(), 64);
  }

  public int totalLength() {
    int max = 0;
    for(int a=0;a<list.size();a++) {
      Element e = list.get(a);
      int len = e.offset + e.length;
      if (len > max) max = len;
    }
    return max;
  }

  public boolean used(int offset, int length) {
    int len = list.size();
    int d1 = offset;
    int d2 = offset + length - 1;
    for(int a=0;a<len;a++) {
      Element e = list.get(a);
      int e1 = e.offset;
      int e2 = e.offset + e.length - 1;
      if ((d1 >= e1 && d1 <= e2) || (d2 >= e1 && d2 <= e2) || (d1 <= e1 && d2 >= e2))
      {
        return true;
      }
    }
    return false;
  }

  public void addElement(String file[], int type) {
    if (type == 0) {
      JFLog.log("Error:TrackPanel.addElement() : type == 0");
      return;
    }
    Element e;
    int len = list.size();
    for(int a=0;a<len;a++) {
      e = list.get(a);
      if (e.offset == selectedOffset) {
        if (!JFAWT.showConfirm("Warning", "Replace element?")) return;
        list.remove(a);
        break;
      }
    }
    e = new Element();
    e.path = file;
    e.offset = selectedOffset;
    e.type = type;
    if (type == Element.TYPE_SPECIAL_TEXT) {
      e.fx = "center,center,Arial,0,16,Enter your text here...";
      e.clr = 0xffffff;  //white
    }
    if (type == Element.TYPE_IMAGE || type == Element.TYPE_SPECIAL_CUT || type == Element.TYPE_SPECIAL_BLUR || type == Element.TYPE_SPECIAL_TEXT) {
      if (file.length == 1)
        e.length = 1;
      else
        e.length = (file.length + (project.config.videoRate-1)) / project.config.videoRate;
    } else {
      //audio / video
      String fullfile = JF.getUserPath() + "/" + file[0];
      if (!new File(fullfile).exists()) return;
      if (!e.start(project.config)) return;
      e.length = (int)e.getDuration();
      e.stop();
      JFLog.log("length = " + e.length);
    }
    if (used(e.offset, e.length)) {
      JFAWT.showError("Error", "That will overlap another element, try another track");
      return;
    }
    list.add(e);
    repaint();
    if (project.config.preview) {
      JFTask task = new JFTask() {
        public boolean work() {
          this.setTitle("Creating Preview");
          this.setLabel("Creating Preview");
          Element e = (Element)getProperty("element");
          e.createPreview(this);
          return true;
        }
      };
      task.setProperty("element", e);
      ProgressDialog dialog = new ProgressDialog(null, true, task);
      dialog.setAutoClose(true);
      dialog.setVisible(true);
      repaint();
    }
  }

  public void deleteElement() {
    //delete element at selectedOffset
    int len = list.size();
    int d1 = selectedOffset;
    int d2 = selectedOffset + project.scale - 1;
    for(int a=0;a<len;a++) {
      Element e = list.get(a);
      int e1 = e.offset;
      int e2 = e.offset + e.length - 1;
      if ((d1 >= e1 && d1 <= e2) || (d2 >= e1 && d2 <= e2) || (d1 <= e1 && d2 >= e2))
      {
        list.remove(a);
        break;
      }
    }
    repaint();
  }

  public void editElement() {
    //edit element properties at selectedOffset
    int len = list.size();
    int d1 = selectedOffset;
    int d2 = selectedOffset + project.scale - 1;
    for(int a=0;a<len;a++) {
      Element e = list.get(a);
      int e1 = e.offset;
      int e2 = e.offset + e.length - 1;
      if ((d1 >= e1 && d1 <= e2) || (d2 >= e1 && d2 <= e2) || (d1 <= e1 && d2 >= e2))
      {
        EditElementProperties dialog = new EditElementProperties(null, true, e, 0);
        dialog.setVisible(true);
        if (dialog.saved) {
          if (project.config.preview && e.type == Element.TYPE_IMAGE) {
            //redo preview for images since length may have changed
            e.createPreview(null);  //this is usually very fast, no need for JFTask/ProgressDialog
          }
        }
        break;
      }
    }
    repaint();
  }

  public void deleteElement(Element e) {
    list.remove(e);
    repaint();
  }

  public void addElement(Element e, boolean updateOffset) {
    if (updateOffset) e.offset = selectedOffset;
    list.add(e);
    repaint();
  }

  public void drag(MouseEvent evt) {
    if (project.dragElement == null) {
      startDrag();
    }
    selectedOffset = project.offset + evt.getX() / 16 * project.scale;
    repaint();
  }

  public void undrag() {}

  public void startDrag() {
    //delete element at selectedOffset
    int len = list.size();
    int d1 = selectedOffset;
    int d2 = selectedOffset + project.scale - 1;
    for(int a=0;a<len;a++) {
      Element e = list.get(a);
      int e1 = e.offset;
      int e2 = e.offset + e.length - 1;
      if ((d1 >= e1 && d1 <= e2) || (d2 >= e1 && d2 <= e2) || (d1 <= e1 && d2 >= e2))
      {
        project.dragElement = e;
        break;
      }
    }
  }

  public void stopDrag() {
    project.dragElement = null;
  }

  public void stop() {
    //unload all elements
    for(int a=0;a<list.size();a++) {
      list.get(a).stop();
    }
  }

  private boolean isCut;

  public boolean isCut() {
    return isCut;
  }

  public void renderVideo(final ProjectPanel.GLData gldata, final JFImage image, final int second, final int frame) {
    int len = list.size();
    int d1 = second;
    int d2 = d1;
    isCut = false;
    for(int a=0;a<len;a++) {
      final Element e = list.get(a);
      int e1 = e.offset;
      int e2 = e.offset + e.length - 1;
      if ((d1 >= e1 && d1 <= e2) || (d2 >= e1 && d2 <= e2) || (d1 <= e1 && d2 >= e2))
      {
        if (!e.isReady()) e.start(project.config);
        isCut = e.isCut();
        if (e.use3d) {
          MainPanel.runGL(new Runnable() {
            public void run() {
              MainPanel.gl.pollEvents();
              gldata.image3d.clear();
              e.preRenderVideo();
              e.renderVideo(gldata.image3d, second, frame);
              //render image3d and put into image
              gldata.scene.setTexture("0", gldata.image3d.getBuffer(), project.config.width, project.config.height, 0);
              gldata.model.setIdentity();
    //          gldata.model.translate(0,0,-20.f);  //base translate : TODO : based on width/height/fov
              gldata.model.translate(e.tx, e.ty, e.tz);
              gldata.model.rotate(e.rx, 1.0f, 0, 0);
              gldata.model.rotate(e.ry, 0, 1.0f, 0);
              gldata.model.rotate(e.rz, 0, 0, 1.0f);
              MainPanel.gl.setCurrent();
              gldata.render.render();
              gldata.image3d.putPixels(gldata.off.getOffscreenPixels(), 0, 0
                , project.config.width, project.config.height, 0);
              e.applyAlpha(gldata.image3d);
              image.putPixelsBlend(gldata.image3d.getBuffer(), 0, 0
                , project.config.width, project.config.height, 0, project.config.width, true);
            }
          });
        } else {
          e.preRenderVideo();
          e.applyAlpha();
          e.renderVideo(image, second, frame);
        }
      }
      if (frame == 0 && d2 == e2+1) {
        e.stop();
      }
    }
  }

  public void renderAudio(short audio[], int offset, int frame) {
    int len = list.size();
    int d1 = offset;
    int d2 = d1;
    for(int a=0;a<len;a++) {
      Element e = list.get(a);
      int e1 = e.offset;
      int e2 = e.offset + e.length - 1;
      if ((d1 >= e1 && d1 <= e2) || (d2 >= e1 && d2 <= e2) || (d1 <= e1 && d2 >= e2))
      {
        if (!e.isReady()) e.start(project.config);
        e.renderAudio(audio);
      }
      if (frame ==0 && d2 == e2+1) {
        e.stop();
      }
    }
  }

  private void initDND() {
    setTransferHandler(new TransferHandler() {
      public boolean canImport(TransferHandler.TransferSupport info) {
        // we only import Files
        if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          return false;
        }
/*
        TransferHandler.DropLocation dl = (TransferHandler.DropLocation) info.getDropLocation();
        Point pt = dl.getDropPoint();
        JComponent c = (JComponent)getComponentAt(pt);
*/
        return true;
      }

      public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
          return false;
        }

        // Check for file flavor
        if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          return false;
        }

        TransferHandler.DropLocation dl = info.getDropLocation();
        Point pt = dl.getDropPoint();

        project.selectTrack(TrackPanel.this);
        selectedOffset = project.offset + pt.x / 16 * project.scale;
        repaint();

        // Get the file(s) that are being dropped.
        Transferable t = info.getTransferable();
        java.util.List<File> data;
        try {
          data = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
        } catch (Exception e) {
          JFLog.log(e);
          return false;
        }
        ArrayList<String> files = new ArrayList<String>();

        // Perform the actual import.
        for(int a=0;a<data.size();a++) {
          switch (info.getDropAction()) {
            case COPY:
            case MOVE:
              files.add(data.get(a).getAbsolutePath());
              break;
            case LINK:
              return false;  //BUG : not supported : ???
          }
        }
        project.addElement(project.prepFiles(files));
        project.calcMaxLength();
        return true;
      }

      public int getSourceActions(JComponent c) {
        return NONE;
      }

      protected Transferable createTransferable(JComponent c) {
        return null;
      }

      protected void exportDone(JComponent source, Transferable data, int action) {
      }
    });
  }
}
