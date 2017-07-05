package jfcontrols.images;

/** Image Resources
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.webui.*;
import static jfcontrols.app.Main.loader;
import jfcontrols.app.Paths;

public class Images {
  public static HashMap<String, Resource> images = new HashMap<String, Resource>();

  public static Resource getImage(String name) {
    synchronized(images) {
      Resource res = images.get(name);
      if (res == null) {
        File file = new File(Paths.imagesPath + "/" + name);
        if (file.exists()) {
          try {
            FileInputStream fis = new FileInputStream(file);
            res = Resource.readStream(fis, name);
            fis.close();
          } catch (Exception e) {
            JFLog.log(e);
          }
        } else {
          res = Resource.readResource("jfcontrols/images/" + name + ".png", Resource.PNG);
        }
        if (res != null) {
          images.put(name, res);
        }
      }
      if (res == null) {
        JFLog.log("Error:Image not found:" + name);
      }
      return res;
    }
  }

  public static boolean exists(String name) {
    synchronized(images) {
      Resource res = images.get(name);
      if (res != null) return true;
      File file = new File(Paths.imagesPath + "/" + name);
      if (file.exists()) return true;
      InputStream is = Images.class.getResourceAsStream("jfcontrols/images/" + name + ".png");
      if (is == null) return false;
      try {is.close();} catch (Exception e) {}
      return true;
    }
  }
}
