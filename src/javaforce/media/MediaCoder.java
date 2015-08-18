package javaforce.media;

/** Base class for Media Coders.
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javaforce.*;
import javaforce.jni.*;

public class MediaCoder {
  private long ctx = 0;
  private static boolean inited = false;
  public static boolean loaded = false;
  static {
    if (!inited) {
      JFNative.load();
      if (JFNative.loaded) {
        init();
        inited = true;
      }
    }
  }
  /** Loads the media framework native libraries. */
  public static boolean init() {
    if (loaded) return true;
    boolean libav_org = false;
    File sysFolder;
    String ext = "";
    if (JF.isWindows()) {
      sysFolder = new File(".");
      ext = ".dll";
    } else {
      sysFolder = new File("/usr/lib");
      if (JF.isMac()) {
        ext = ".dylib";
      } else {
        ext = ".so";
      }
    }
    Library libs[] = {
      new Library("avcodec")
      , new Library("avdevice")
      , new Library("avfilter")
      , new Library("avformat")
      , new Library("avutil")
      , new Library("swscale")
      , new Library("swresample", true)  //(ffmpeg)
      , new Library("avresample", true)  //(libav_org)
    };
    JFNative.findLibraries(sysFolder, libs, ext, libs.length-1, true);
    if (libs[6].path != null) {
      libav_org = false;
    } else if (libs[7].path != null) {
      libav_org = true;
    }
    if (!haveLibs(libs)) {
      for(int a=0;a<libs.length;a++) {
        if (a == 6 && libav_org) continue;
        if (a == 7 && !libav_org) continue;
        if (libs[a].path == null) {
          System.out.println("Error:Unable to load library:" + libs[a].name + ext);
        }
      }
//      JFLog.logTrace("MediaCoder.init() failed");
      return false;
    }
    loaded = ffmpeg_init(libs[0].path, libs[1].path, libs[2].path, libs[3].path
      , libs[4].path, libs[5].path, libav_org ? libs[7].path : libs[6].path, libav_org);
    return loaded;
  }
  private static native boolean ffmpeg_init(String codec, String device, String filter, String format, String util, String scale, String resample, boolean libav_org);

  private static boolean haveLibs(Library libs[]) {
    int cnt = 0;
    for(int a=0;a<6;a++) {
      if (libs[a].path != null) cnt++;
    }
    if (libs[6].path != null) cnt++;
    else if (libs[7].path != null) cnt++;
    return cnt == 7;
  }

  //constants

  //constants
  public static final int SEEK_SET = 0;
  public static final int SEEK_CUR = 1;
  public static final int SEEK_END = 2;

  //for use with VideoDecoder
  public static final int AV_CODEC_ID_NONE = 0;
  public static final int AV_CODEC_ID_MPEG1VIDEO = 1;
  public static final int AV_CODEC_ID_MPEG2VIDEO = 2;
  public static final int AV_CODEC_ID_H263 = 5;
  public static final int AV_CODEC_ID_MPEG4 = 13;
  public static final int AV_CODEC_ID_H264 = 28;
  public static final int AV_CODEC_ID_THEORA = 31;
  public static final int AV_CODEC_ID_VP8 = 141;

  //a few audio codecs
  public static final int AV_CODEC_ID_PCM_S16LE = 0x10000;  //wav file
  public static final int AV_CODEC_ID_FLAC = 0x1500c;

  //returned by MediaDecoder.read()
  public static final int END_FRAME = -1;
  public static final int NULL_FRAME = 0;  //could be metadata frame
  public static final int AUDIO_FRAME = 1;
  public static final int VIDEO_FRAME = 2;

  /** Downloads the codec pack for Windows (supports .zip or .7z) */
  public static boolean download() {
    if (!JF.isWindows()) {
      JF.showError("Notice", "This application requires the codecpack which was not detected.\n"
        + "Please visit http://pquiring.github.io/javaforce/codecpack.html for more info.\n"
        + "Press OK to visit this page now");
      JF.openURL("http://pquiring.github.io/javaforce/codecpack.html");
      return false;
    }
    if (!JF.showConfirm("Notice", "This application requires the codecpack which was not detected.\n"
      + "Please visit http://pquiring.github.io/javaforce/codecpack.html for more info.\n"
      + "NOTE:To install the codecpack this app may require administrative rights.\n"
      + "To run with admin rights, right click this app and select 'Run as Administrator'.\n"
      + "Press OK to download and install now.\n"
      + "Press CANCEL to visit website now.\n"))
    {
      JF.openURL("http://pquiring.github.io/javaforce/codecpack.html");
      return false;
    }
    JFTask task = new JFTask() {
      public boolean work() {
        this.setTitle("Downloading CodecPack");
        this.setLabel("Downloading CodecPack...");
        this.setProgress(-1);
        String destFolder = ".";
        //find best place to extract to
        try {
          File file = new File(destFolder + "\\$testfile$.tmp");
          FileOutputStream fos = new FileOutputStream(file);
          fos.close();
          file.delete();
        } catch (Exception e) {
          this.setLabel("Download failed (no write access to folder)");
          return false;
        }
        //first download latest URL from javaforce.sf.net
        try {
          BufferedReader reader = new BufferedReader(new InputStreamReader(
            new URL("http://pquiring.github.io/javaforce/codecpackwin"
            + (JF.is64Bit() ? "64" : "32") + ".html").openStream()));
          String url = reader.readLine();
          int zLength = JF.atoi(reader.readLine());
          byte buf[] = new byte[64 * 1024];
          int length = 0;
          File zfile;
          boolean z7;
          int z7Length = 0;
          String url7 = null;
          String filename = null;
          if (url.endsWith(".zip")) {
            zfile = new File(destFolder + "\\codecpack.zip");
            z7 = false;
          } else if (url.endsWith(".7z")) {
            zfile = new File(destFolder + "\\codecpack.7z");
            z7 = true;
            url7 = reader.readLine();
            z7Length = JF.atoi(reader.readLine());
            int i1 = url.lastIndexOf("/")+1;
            int i2 = url.lastIndexOf(".");
            filename = url.substring(i1,i2);
          } else {
            JF.showError("Error", "Unsupported file type");
            return false;
          }
          {
            InputStream is = new URL(url).openStream();
            FileOutputStream fos = new FileOutputStream(zfile);
            System.out.println("Downloading:" + url);
            while (true) {
              int read = is.read(buf);
              if (read == -1) break;
              if (read == 0) {
                JF.sleep(10);
                continue;
              }
              fos.write(buf, 0, read);
              length += read;
              this.setProgress(length * 100 / zLength);
            }
            fos.close();
            if (length != zLength) {
              this.setLabel("Download failed...");
              return false;
            }
          }
          if (z7) {
            //download 7za.exe (~500KB)
            InputStream is = new URL(url7).openStream();
            FileOutputStream fos = new FileOutputStream(new File(destFolder + "/7za.exe"));
            length = 0;
            System.out.println("Downloading:" + url7);
            while (true) {
              int read = is.read(buf);
              if (read == -1) break;
              if (read == 0) {
                JF.sleep(10);
                continue;
              }
              fos.write(buf, 0, read);
              length += read;
            }
            fos.close();
            if (length != z7Length) {
              this.setLabel("Download failed (7za.exe)...");
              return false;
            }
          }
          this.setLabel("Download complete, now installing...");
          this.setProgress(0);
          if (z7) {
            ShellProcess sp = new ShellProcess();
//            sp.setFolder(new File(destFolder));
            System.out.println("Exec:7za e " + zfile.getName()+ " " + filename+"/bin/*.dll");
            sp.run(new String[]{"7za.exe", "e", zfile.getName(), filename+"\\bin\\*.dll"}, false);
          } else {
            ZipFile zf = new ZipFile(zfile);
            int cnt = 0;
            FileOutputStream fos;
            for (Enumeration e = zf.entries(); e.hasMoreElements();) {
              ZipEntry ze = (ZipEntry)e.nextElement();
              String name = ze.getName().toLowerCase();
              if (!name.endsWith(".dll")) continue;
              int idx = name.lastIndexOf("/");
              if (idx != -1) {
                name = name.substring(idx+1);  //remove any path
              }
              fos = new FileOutputStream(destFolder + "\\" + name);
              InputStream zis = zf.getInputStream(ze);
              JFLog.log("extracting:" + name + ",length=" + ze.getSize());
              JF.copyAll(zis, fos, ze.getSize());
              zis.close();
              fos.close();
              cnt++;
              this.setProgress(cnt * 100 / 8);
            }
          }
          this.setProgress(100);
          this.setLabel("Install complete");
          return true;
        } catch (Exception e) {
          this.setLabel("An error occured, see console output for details.");
          JFLog.log(e);
          return false;
        }
      }
    };
    new ProgressDialog(null, true, task).setVisible(true);
    return task.getStatus();
  }
}
