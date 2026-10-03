/** Config
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.media.*;

public class Config {
  public static Config current;

  public int idx;  //camera to use (0=first)
  public int fps;
  public int bit_rate;
  public int codec;
  public boolean dtls;

  public static final int H264 = MediaCoder.VIDEO_CODEC_ID_H264;  //5
  public static final int H265 = MediaCoder.VIDEO_CODEC_ID_H265;  //6
  public static final int VP8 = MediaCoder.VIDEO_CODEC_ID_VP8;  //8
  public static final int VP9 = MediaCoder.VIDEO_CODEC_ID_VP9;  //9

  public Config() {
    idx = 0;  //first camera
    fps = 10;  //10 fps
    bit_rate = 1 * 1024 * 1024;  //1Mbps
    codec = H264;  //H264
  }

  public static void load() {
    current = new Config();
    //load from %config%/jfrtspcamera.cfg
    String filename = Paths.config + "/jfrtspcamera.cfg";
    File file = new File(filename);
    if (!file.exists()) {
      current.save();
      return;
    }
    try {
      FileInputStream fis = new FileInputStream(file);
      String[] lns = new String(fis.readAllBytes()).replaceAll("\\r", "").split("\n");
      fis.close();
      for(String ln : lns) {
        int idx = ln.indexOf('=');
        if (idx == -1) continue;
        String key = ln.substring(0, idx).trim();
        String value = ln.substring(idx + 1).trim();
        switch (key) {
          case "idx": current.idx = clamp(Integer.valueOf(value), 0, 64); break;  //0-64
          case "fps": current.fps = clamp(Integer.valueOf(value), 5, 60); break;  //5-60 fps
          case "bit_rate": current.bit_rate = clamp(Integer.valueOf(value), 128 * 1024, 16 * 1024 * 1024); break;  //128k to 16M bps
          case "codec":
            current.codec = clamp(Integer.valueOf(value), 5, 9);
            if (current.codec == 7) {  //not supported
              current.codec = H264;
            }
            break;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void save() {
    String filename = Paths.config + "/jfrtspcamera.cfg";
    File file = new File(filename);
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("idx=" + idx + "\n");
      sb.append("fps=" + fps + "\n");
      sb.append("bit_rate=" + bit_rate + "\n");
      sb.append("codec=" + codec + "\n");
      FileOutputStream fis = new FileOutputStream(file);
      fis.write(sb.toString().getBytes());
      fis.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static int clamp(int value, int min, int max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }
}
