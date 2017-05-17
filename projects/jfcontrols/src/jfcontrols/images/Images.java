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

public class Images {
  public static HashMap<String, Resource> images = new HashMap<String, Resource>();

  public static Resource getImage(String name) {
    synchronized(images) {
      Resource res = images.get(name);
      if (res == null) {
        res = Resource.readResource("jfcontrols/images/" + name + ".png", Resource.PNG);
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
}
