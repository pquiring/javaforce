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
import javaforce.awt.*;
import javaforce.jni.*;

import javaforce.webui.*;
import javaforce.webui.event.*;

public class MediaCoder {
  private long ctx = 0;
  public static boolean loaded = false;
  /** Loads the media framework native libraries. */
  public static boolean init() {
    if (loaded) return true;
    JFNative.load();
    boolean libav_org = false;
    File[] sysFolders;
    String ext = "";
    String apphome = System.getProperty("java.app.home");
    if (apphome == null) apphome = ".";
    if (JF.isWindows()) {
      sysFolders = new File[] {new File(apphome), new File(System.getenv("appdata") + "/ffmpeg"), new File(".")};
      ext = ".dll";
    } else if (JF.isMac()) {
      sysFolders = new File[] {new File(apphome), new File(".")};
      ext = ".dylib";
    } else {
      sysFolders = new File[] {new File("/usr/lib"), new File(LnxNative.getArchLibFolder())};
      ext = ".so";
    }
    Library[] libs = {
      new Library("avcodec")
      , new Library("avdevice")
      , new Library("avfilter")
      , new Library("avformat")
      , new Library("avutil")
      , new Library("swscale")
      , new Library("postproc")
      , new Library("swresample", true)  //(ffmpeg)
      , new Library("avresample", true)  //(libav_org)
    };
    JFNative.findLibraries(sysFolders, libs, ext, libs.length-1);
    if (libs[6].path != null) {
      libav_org = false;
    } else if (libs[7].path != null) {
      libav_org = true;
    }
    if (!haveLibs(libs)) {
      for(int a=0;a<libs.length;a++) {
        if (a == 7 && libav_org) continue;
        if (a == 8 && !libav_org) continue;
        if (libs[a].path == null) {
          System.out.println("Error:Unable to find library:" + libs[a].name + ext);
        }
      }
//      JFLog.logTrace("MediaCoder.init() failed");
      return false;
    }
    loaded = ffmpeg_init(libs[0].path, libs[1].path, libs[2].path, libs[3].path
      , libs[4].path, libs[5].path, libs[6].path, libav_org ? libs[8].path : libs[7].path, libav_org);
    return loaded;
  }
  private static native boolean ffmpeg_init(String codec, String device, String filter, String format, String util, String scale, String postproc, String resample, boolean libav_org);
  public static native void ffmpeg_set_logging(boolean state);

  private static boolean haveLibs(Library[] libs) {
    int cnt = 0;
    for(int a=0;a<7;a++) {
      if (libs[a].path != null) cnt++;
    }
    if (libs[7].path != null) cnt++;
    else if (libs[8].path != null) cnt++;
    return cnt == 8;
  }

  //seek types
  public static final int SEEK_SET = 0;
  public static final int SEEK_CUR = 1;
  public static final int SEEK_END = 2;

  //profile levels
  public static final int PROFILE_BASELINE = 1;  //default
  public static final int PROFILE_MAIN = 2;
  public static final int PROFILE_HIGH = 3;

  //video codecs (VideoDecoder)
  public static final int AV_CODEC_ID_NONE = 0;
  public static final int AV_CODEC_ID_MPEG1VIDEO = 1;
  public static final int AV_CODEC_ID_MPEG2VIDEO = 2;
  public static final int AV_CODEC_ID_H263 = 4;
  public static final int AV_CODEC_ID_MPEG4 = 12;
  public static final int AV_CODEC_ID_H264 = 27;
  public static final int AV_CODEC_ID_THEORA = 30;
  public static final int AV_CODEC_ID_VP8 = 139;
  public static final int AV_CODEC_ID_VP9 = 167;
  public static final int AV_CODEC_ID_H265 = 173;

  //audio codecs
  public static final int AV_CODEC_ID_PCM_S16LE = 0x10000;  //wav file
  public static final int AV_CODEC_ID_MP2 = 0x15000;
  public static final int AV_CODEC_ID_MP3 = 0x15001;
  public static final int AV_CODEC_ID_AAC = 0x15002;
  public static final int AV_CODEC_ID_AC3 = 0x15003;
  public static final int AV_CODEC_ID_VORBIS = 0x15005;
  public static final int AV_CODEC_ID_FLAC = 0x1500c;
  public static final int AV_CODEC_ID_GSM_MS = 0x1501e;
  public static final int AV_CODEC_ID_OPUS = 0x1503c;

  //returned by MediaDecoder.read()
  public static final int END_FRAME = -1;
  public static final int NULL_FRAME = 0;  //could be metadata frame
  public static final int AUDIO_FRAME = 1;
  public static final int VIDEO_FRAME = 2;

  /** Downloads the codec pack for Windows (supports .zip or .7z) */
  public static boolean download() {
    if (!JF.isWindows()) {
      JFAWT.showError("Notice", "This application requires the codecpack which was not detected.\n"
        + "Please visit http://pquiring.github.io/javaforce/codecpack.html for more info.\n"
        + "Press OK to visit this page now");
      JFAWT.openURL("http://pquiring.github.io/javaforce/codecpack.html");
      return false;
    }
    if (!JFAWT.showConfirm("Notice", "This application requires the codecpack which was not detected.\n"
      + "Please visit http://pquiring.github.io/javaforce/codecpack.html for more info.\n"
      + "Press OK to download and install now.\n"
      + "Press CANCEL to visit website now.\n"))
    {
      JFAWT.openURL("http://pquiring.github.io/javaforce/codecpack.html");
      return false;
    }
    JFTask task = getDownloadTask();
    new ProgressDialog(null, true, task).setVisible(true);
    return task.getStatus();
  }
  private static JFTask getDownloadTask() {
    return new JFTask() {
      public boolean work() {
        this.setTitle("Downloading CodecPack");
        this.setLabel("Downloading CodecPack...");
        this.setProgress(-1);
        String destFolder = System.getenv("appdata") + "/ffmpeg";
        try {
          new File(destFolder).mkdir();
          File file = new File(destFolder + "/$testfile$.tmp");
          FileOutputStream fos = new FileOutputStream(file);
          fos.close();
          file.delete();
        } catch (Exception e) {
          this.setLabel("Download failed (no write access to folder)");
          return false;
        }
        //first download latest URL from github
        try {
          BufferedReader reader = new BufferedReader(new InputStreamReader(
            new URI("http://pquiring.github.io/javaforce/codecpackwin"
            + (JF.is64Bit() ? "64" : "32") + ".html").toURL().openStream()));
          String url = reader.readLine();
          int zLength = JF.atoi(reader.readLine());
          byte[] buf = new byte[64 * 1024];
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
            JFAWT.showError("Error", "Unsupported file type");
            return false;
          }
          {
            URL urlAddr = new URI(url).toURL();
            URLConnection conn = urlAddr.openConnection();
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393");
            conn.connect();
            InputStream is = conn.getInputStream();
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
            URL urlAddr = new URI(url7).toURL();
            URLConnection conn = urlAddr.openConnection();
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393");
            conn.connect();
            InputStream is = conn.getInputStream();
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
  }

  private static Label label;
  private static ProgressBar progressBar;

  public static Panel downloadWebUI() {
    Panel panel = new Panel();
    Column col = new Column();
    panel.add(col);
    label = new Label("jfDVR needs to download a codec pack to decode video");
    col.add(label);
    progressBar = new ProgressBar(Component.HORIZONTAL, 100, 14);
    col.add(progressBar);
    Row row = new Row();
    col.add(row);
    Button button = new Button("Download");
    row.add(button);
    button.addClickListener((MouseEvent me, Component c) -> {
      JFLog.log("Starting codec pack download...");
      c.setVisible(false);
      JFTask task = getDownloadTask();
      task.start(new WebUIUpdate());
    });
    return panel;
  }
  private static class WebUIUpdate implements JFTaskListener {
    public void setLabel(String text) {
      label.setText(text);
    }

    public void setTitle(String text) {
      //nop
    }

    public void setProgress(int value) {
      progressBar.setValue(value);
    }

    public void done() {
      init();
      label.setText("Press refresh your browser");
    }

    public void dispose() {
      label.setText("Press refresh your browser");
    }
  }
}
