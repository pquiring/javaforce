package jfcontrols.panels;

/** VisionSystem
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.webui.*;
import javaforce.webui.event.Changed;

import jfcontrols.app.*;
import jfcontrols.db.*;
import jfcontrols.images.*;

public class VisionSystem {
  public static JFImage noimg;
  public static final int OK = 1;
  public static final int NOK = 2;
  private static int scaleDownX(int x) {
    return x * 1024 / 1920;
  }
  private static int scaleDownY(int y) {
    return y * 576 / 1080;
  }
  private static int scaleUpX(int x) {
    return x * 1920 / 1024;
  }
  private static int scaleUpY(int y) {
    return y * 1080 / 576;
  }
  /** Generates vision image for ROI Editor. */
  public static JFImage generateImage(WebUIClient client) {
    //image size = 1024x576 (scaled down from 1920x1080)
    JFImage img = (JFImage)client.getProperty("vision-image");
    if (img == null) {
      img = new JFImage(1024, 576);
      client.setProperty("vision-image", img);
    }
    int pid = (Integer)client.getProperty("visionprogram");
    int sid = (Integer)client.getProperty("visionshot");
    int rid = (Integer)client.getProperty("visionarea");
    JFImage okimg = load(pid, sid, rid, OK);
    if (okimg != null) {
      img.putJFImage(okimg, 0, 0);
    } else {
      if (noimg == null) {
        noimg = new JFImage();
        noimg.loadPNG(new ByteArrayInputStream(Images.getImage("no-image").data));
      }
      img.putJFImage(noimg, 0 ,0);
    }
    //draw ROIs
/*
    VisionAreaRow rows[] = Database.getVisionAreasForPID(pid);
    java.awt.Graphics2D g = img.getGraphics2D();
    boolean first = true;
    for(VisionAreaRow row : rows) {
      int alpha = 128;
      if (row.id == rid) alpha += 64;
      if (first) {
        //locator = cyan
        g.setColor(new java.awt.Color(0, 255, 255, alpha));
        first = false;
      } else {
        //others = green
        g.setColor(new java.awt.Color(0, 255, 0, alpha));
      }
      img.getGraphics2D().drawRect(scaleX(row.x1), scaleY(row.y1), scaleX(row.x2 - row.x1 + 1), scaleY(row.y2 - row.y1 + 1));
    }
*/
    return img;
  }
  private static String typeToString(int type) {
    switch (type) {
      case OK: return "OK";
      case NOK: return "NOK";
    }
    return null;
  }
  private static HashMap<String, JFImage> imgCache = new HashMap<String, JFImage>();
  public static JFImage load(int pid, int sid, int rid, int type) {
    String filename = Paths.visionImagesPath + "/" + pid + "/" + sid + "/" + rid + "-" + typeToString(type) + ".png";
    JFImage img = imgCache.get(filename);
    if (img != null) return img;
    if (!new File(filename).exists()) {
      return null;
    }
    img = new JFImage();
    img.load(filename);
    imgCache.put(filename, img);
    return img;
  }
  public static void store(int pid, int rid, int type, JFImage img) {
    String filename = Paths.visionImagesPath + "/" + pid + "/" + rid + "-" + typeToString(type) + ".png";
    imgCache.put(filename, img);
  }
  public static void save(int pid, int rid, int type, JFImage img) {
    String filename = Paths.visionImagesPath + "/" + pid + "/" + rid + "-" + typeToString(type) + ".png";
    imgCache.put(filename, img);
    img.savePNG(filename);
  }
  public static void deleteAreaImages(int pid, int rid) {
    String folder = Paths.visionImagesPath + "/" + pid;
    File files[] = new File(folder).listFiles();
    String match = Integer.toString(rid) + "-";
    if (files == null) return;
    for(File file : files) {
      if (file.isDirectory()) continue;
      if (file.getName().startsWith(match)) {
        file.delete();
      }
    }
  }
  public static void deleteProgramImages(int pid) {
    String folder = Paths.visionImagesPath + "/" + pid;
    File files[] = new File(folder).listFiles();
    if (files == null) return;
    for(File file : files) {
      if (file.isDirectory()) continue;
      file.delete();
    }
    new File(folder).delete();
  }
  public static void setupVisionImage(LayersPanel panel, int p_id, int s_id, int r_id) {
    panel.removeAll();
    int pid = Database.getVisionProgramPID(p_id);
    VisionAreaRow rows[] = Database.getVisionAreas(pid, s_id);
    Rectangle rect = new Rectangle();
    //add image layer
    Image img = new Image(null);
    panel.add(img);
    //add layers
    for(int a=0;a<rows.length;a++) {
      VisionAreaRow layer = rows[a];
      rect.x = scaleDownX(layer.x1);
      rect.y = scaleDownY(layer.y1);
      rect.width = scaleDownX(layer.x2 - layer.x1 + 1);
      rect.height = scaleDownY(layer.y2 - layer.y1 + 1);
      Canvas canvas = new Canvas();
      canvas.setSize(1024, 576);
      canvas.drawRect(a == 0 ? Color.blue : Color.green, rect);
      if (layer.id == r_id) {
//        canvas.setStyle("opacity", "0.5f");
      } else {
//        canvas.setStyle("opacity", "0.25f");
      }
      panel.add(canvas);
    }
    //add drawable layer
    Canvas draw = new Canvas();
    draw.setSize(1024, 576);
    draw.enableDrawRect();
    draw.addChangedListener(new Changed() {
      public void onChanged(Component cmp) {
        Rectangle rect = draw.getRect();
        WebUIClient client = panel.getClient();
        int pid = (Integer)client.getProperty("visionprogram");
        int sid = (Integer)client.getProperty("visionshot");
        int rid = (Integer)client.getProperty("visionarea");
        VisionAreaRow row = Database.getVisionArea(pid, sid, rid);
        if (row == null) {
          JFLog.log("Error:VisionAreaRow not found for pid:" + pid + ", rid:" + rid);
          return;
        }
        JFLog.log("VisionAreaRow update for pid:" + pid + ", rid:" + rid + "@" + rect);
        row.x1 = scaleUpX(rect.x);
        row.y1 = scaleUpY(rect.y);
        row.x2 = scaleUpX(rect.x + rect.width - 1);
        row.y2 = scaleUpY(rect.y + rect.height - 1);
        TextField x1 = (TextField)client.getPanel().getComponent("jfc_visionareas_x1_int_" + rid);
        x1.setText(Integer.toString(row.x1));
        TextField y1 = (TextField)client.getPanel().getComponent("jfc_visionareas_y1_int_" + rid);
        y1.setText(Integer.toString(row.y1));
        TextField x2 = (TextField)client.getPanel().getComponent("jfc_visionareas_x2_int_" + rid);
        x2.setText(Integer.toString(row.x2));
        TextField y2 = (TextField)client.getPanel().getComponent("jfc_visionareas_y2_int_" + rid);
        y2.setText(Integer.toString(row.y2));
        Database.visionareas.save();
        setupVisionImage(panel, p_id, s_id, r_id);
      }
    });
    panel.add(draw);
  }
}
