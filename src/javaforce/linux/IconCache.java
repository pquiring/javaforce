package javaforce.linux;

/** Cache of icons used in .desktop files.
 *
 * Created : Aug 16, 2012
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.utils.*;  //for jfopen

public class IconCache {
  private static HashMap<String, JFImage> icons = new HashMap<String, JFImage>();
  private static String prefix;

  public static JFImage loadIcon(String iconName) {
    JFImage icon = icons.get(iconName);
    if (icon != null) return icon;
    icon = new JFImage();
    if (iconName.startsWith("jfile-")) {
      icon.loadPNG(icon.getClass().getClassLoader().getResourceAsStream(iconName + ".png"));
      icons.put(iconName, icon);
      return icon;
    }
    if ((prefix != null) && (iconName.startsWith(prefix))) {
      icon.loadPNG(icon.getClass().getClassLoader().getResourceAsStream(iconName + ".png"));
      icons.put(iconName, icon);
      return icon;
    }
    //try to find the icon in different locations
    if (iconName.endsWith(".xpm")) {
      if (icon.loadXPM(iconName)) {
        icons.put(iconName, icon);
        return icon;
      }
    }
    if (icon.loadPNG(iconName)) {
      icons.put(iconName, icon);
      return icon;
    }
    if (icon.loadPNG("/usr/share/icons/hicolor/48x48/apps/" + iconName + ".png")) {
      icons.put(iconName, icon);
      return icon;
    }
    if (icon.loadPNG("/usr/share/pixmaps/" + iconName + ".png")) {
      icons.put(iconName, icon);
      return icon;
    }
    if (icon.loadPNG("/usr/share/app-install/icons/" + iconName + ".png")) {
      icons.put(iconName, icon);
      return icon;
    }
    if (icon.loadPNG(JF.getUserPath() + "/.local/share/icons/hicolor/48x48/apps/" + iconName + ".png")) {
      icons.put(iconName, icon);
      return icon;
    }
    if (icon.loadPNG(JF.getUserPath() + "/.local/share/icons/hicolor/32x32/apps/" + iconName + ".png")) {
      icons.put(iconName, icon);
      return icon;
    }
    if (icon.loadPNG(JF.getUserPath() + "/.local/share/icons/hicolor/16x16/apps/" + iconName + ".png")) {
      icons.put(iconName, icon);
      return icon;
    }
    if (icon.loadXPM("/usr/share/pixmaps/" + iconName + ".xpm")) {
      icons.put(iconName, icon);
      return icon;
    }
    //not found - 404
    JFLog.log("icon not found:" + iconName);
    icon.loadPNG(icon.getClass().getClassLoader().getResourceAsStream("jfile-404-icon.png"));
    return icon;
  }

  public static JFImage scaleIcon(JFImage image,int x,int y) {
    if ((image.getWidth() == x) && (image.getHeight() == y)) return image;
    JFImage scale = new JFImage(x, y);
    scale.fill(0, 0, x, y, 0x00000000, true);
    scale.getGraphics().drawImage(image.getImage(), 0,0, x,y, 0,0, image.getWidth(),image.getHeight(), null);
    return scale;
  }

  private static HashMap<String, String> mimes = new HashMap<String, String>();

  public static String findIcon(String fn) {
    int idx = fn.lastIndexOf("/");
    if (idx != -1) fn = fn.substring(idx+1);
    idx = fn.lastIndexOf(".");
    if (idx == -1) return "jfile-file";  //no extension - use generic icon
    //find Icon file based on ext (mime type)
    String ext = fn.substring(idx+1);
    String icon = mimes.get(ext);
    if (icon != null) return icon;
    try {
      if (JF.isWindows()) {
        icon = "jfile-file";  //TODO!!!
      } else {
        String mime = OpenFile.getMimeType(ext);
        icon = OpenFile.getIcon(mime, "open");
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    if (icon == null) icon = "jfile-file";  //none found - use generic icon
    mimes.put(ext, icon);
    return icon;
  }

  public static void setPrefix(String prefix) {
    IconCache.prefix = prefix;
  }
}
