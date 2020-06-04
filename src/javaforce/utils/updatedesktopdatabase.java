package javaforce.utils;

/**
 * Created : Apr 27, 2012
 *
 * @author pquiring
 *
 * Reads all .desktop files and generates mimeaction.cache
 *
 * This differs from the freedesktop.org version with a new field:
 * Action=open|edit .desktop files can contain this field to distinguish how the
 * app handles the mimetype. It can do both open and edit (comma seperated) but
 * is unlikely. More actions may be created in the future.
 *
 * Format of mimeaction.cache [open] mimetype=desktops... ... [edit]
 * mimetype=desktops... ...
 *
 * jfopen (and jfile) will use this mimeaction.cache to determine how to open
 * files.
 *
 * This allows different apps to handle the same mime-types for different
 * purposes. ie: jview can 'open' jpeg images while jpaint can 'edit' jpeg
 * images. ie: jmedia can 'open' wav files while audacity can 'edit' wav files.
 *
 * If no action field is found "open" is assumed for now. Some pre-defined
 * Actions are built-in.
 *
 * This utility is run by japps automatically. If you install/remove apps
 * manually you will have to run this command manually.
 *
 */
import java.io.*;
import java.util.*;

public class updatedesktopdatabase {

  public static class MimeGroup {

    public String mime;
    public ArrayList<String> desktopsList = new ArrayList<String>();
  }

  public static class ActionGroup {

    public String action;
    public ArrayList<MimeGroup> mimeList = new ArrayList<MimeGroup>();
  }
  public static ArrayList<ActionGroup> actionsList = new ArrayList<ActionGroup>();
  public static boolean recursive = true;

  public static void main(String[] args) {
    String folderPath = "/usr/share/applications";  //default if none specified
    for (int a = 0; a < args.length; a++) {
      if (args[a].equals("--no-recursive")) {
        recursive = false;
        continue;
      }
      if (args[a].startsWith("-")) {
        continue;
      }
      if (args[a].equals("--help")) {
        System.out.println("usage : update-desktop-database <folder> [--no-recursive]");
        System.exit(0);
      }
      folderPath = args[a];
    }
    File folder = new File(folderPath);
    parseFolder(folder);
    writeCache(folder);
  }

  public static void parseFolder(File folder) {
//System.out.println("folder=" + folder.getAbsolutePath());
    try {
      File[] files = folder.listFiles();
      for (int f = 0; f < files.length; f++) {
        if (files[f].isDirectory()) {
          if (recursive) {
            parseFolder(files[f]);
          }
          continue;
        }
        String name = files[f].getName();
        if (!name.endsWith(".desktop")) {
          continue;
        }
        FileInputStream fis = new FileInputStream(files[f]);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        String ln, mimetype = null, action = null;
        while ((ln = br.readLine()) != null) {
          if (ln.startsWith("MimeType=")) {
            mimetype = ln.substring(9).trim();
          }
          if (ln.startsWith("Action=")) {
            action = ln.substring(7).trim().toLowerCase();
          }
        }
        if ((mimetype == null) || (mimetype.length() == 0)) {
          continue;
        }
        String[] mimes = mimetype.split(";");
        if (mimes.length == 0) {
          continue;
        }
//System.out.println("file="+name);
        if (action == null) {
          action = "open";
        }
        String[] actions = action.split(",");
//System.out.println("actions.length=" + actions.length + ",mimes.length=" + mimes.length);
        for (int m = 0; m < mimes.length; m++) {
          for (int a = 0; a < actions.length; a++) {
            add(mimes[m], actions[a], folder.getAbsolutePath(), files[f].getAbsolutePath());
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void add(String mime, String action, String folder, String desktop) {
//System.out.println("add:" + mime + "," + action);
    if (!folder.endsWith("/")) {
      folder += "/";
    }
    int idx = -1;
    //apply some pre-defined rules
    if (desktop.endsWith("/audacity.desktop")) {
      action = "edit";
    }
    //this's it for now
    for (int a = 0; a < actionsList.size(); a++) {
      if (actionsList.get(a).action.equals(action)) {
        idx = a;
        break;
      }
    }
    ActionGroup agroup = null;
    if (idx == -1) {
      //new action
      agroup = new ActionGroup();
      agroup.action = action;
      actionsList.add(agroup);
    } else {
      agroup = actionsList.get(idx);
    }
    idx = -1;
    for (int m = 0; m < agroup.mimeList.size(); m++) {
      if (agroup.mimeList.get(m).mime.equals(mime)) {
        idx = m;
        break;
      }
    }
    MimeGroup mgroup = null;
    if (idx == -1) {
      //new mime for this action
      mgroup = new MimeGroup();
      mgroup.mime = mime;
      agroup.mimeList.add(mgroup);
    } else {
      mgroup = agroup.mimeList.get(idx);
    }
    mgroup.desktopsList.add(desktop.substring(folder.length()));
  }

  public static void writeCache(File folder) {
    File cache = new File(folder.getAbsolutePath() + "/mimeinfo.cache");
    try {
      FileOutputStream fos = new FileOutputStream(cache);
      for (int a = 0; a < actionsList.size(); a++) {
        ActionGroup agroup = actionsList.get(a);
        fos.write(("[" + agroup.action + "]\n").getBytes());
        for (int m = 0; m < agroup.mimeList.size(); m++) {
          MimeGroup mgroup = agroup.mimeList.get(m);
          fos.write((mgroup.mime + "=").getBytes());
          for (int d = 0; d < mgroup.desktopsList.size(); d++) {
            if (d > 0) {
              fos.write(";".getBytes());
            }
            fos.write(mgroup.desktopsList.get(d).getBytes());
          }
          fos.write("\n".getBytes());
        }
      }
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
