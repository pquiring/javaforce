/**
 * Created : July 5, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.media.*;

import com.jhlabs.image.*;

public class Element implements MediaIO {
  public String path[];  //relative to user home
  public int type;
  public int offset;  //offset of element within Track
  public int length;  //seconds (not used with audio)
  public int sx1, sy1, sx2, sy2;  //src position (%)
  public int dx1, dy1, dx2, dy2;  //dst position (%)
  public int db;  //sound level gain (dB)
  public boolean alphaFadeIn;
  public int alphaFadeInDuration;
  public int alphaLevel;
  public int clrAlpha;
  public int audioDelay;  //0-999 ms
  public boolean use3d, mute;
  public float tx, ty, tz;
  public float rx, ry, rz;
  public int clr;
  public String fx;  //; list of props

  public Element() {
    sx1 = sy1 = 0;
    sx2 = sy2 = 100;
    dx1 = dy1 = 0;
    dx2 = dy2 = 100;
    alphaFadeInDuration = 5;
    alphaLevel = 255;  //opaque
    fx = "";
  }

  public int read(MediaCoder coder, byte[] bytes) {
    try {
      return raf.read(bytes, 0, bytes.length);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return 0;
  }

  public int write(MediaCoder coder, byte[] bytes) {
    try {
      return raf.read(bytes, 0, bytes.length);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return 0;
  }

  public long seek(MediaCoder coder, long pos, int how) {
    try {
      switch (how) {
        case MediaCoder.SEEK_SET: break;  //seek set
        case MediaCoder.SEEK_CUR: pos += raf.getFilePointer(); break;  //seek cur
        case MediaCoder.SEEK_END: pos += raf.length(); break; //seek end
      }
      raf.seek(pos);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return pos;
  }

  //transient/private/static data (do not save to XML file)
  public static final int TYPE_IMAGE = 1;
  public static final int TYPE_VIDEO = 2;
  public static final int TYPE_AUDIO = 3;
  public static final int TYPE_SPECIAL_CUT = 4;
  public static final int TYPE_SPECIAL_BLUR = 5;
  public static final int TYPE_SPECIAL_TEXT = 6;

  public static final int TYPE_CAMERA = 99;  //not saved to tracks - just used to edit a camera key

  private RandomAccessFile raf;
  private ArrayList<JFImage> preview = new ArrayList<JFImage>();
  private static JFImage imgImage, imgVideo, imgAudio, imgCut, imgBlur, imgText;
  private static Config previewConfig = new Config();
  private boolean ready;
  private JFImage srcImage;
  private int width, height;
  private MediaDecoder ff;
  private short audio[];
  private int audioPos, audioSize;
  private ArrayList<int[]> frames = new ArrayList<int[]>();
  private int imgIdx;
  private double gain, attn;
  private double alphaFadeLevel, alphaFadeStep;
  private double frameRateRatio;
  private double currentFrame;
  private boolean eof;
  private double videoRate;
  //text details
  private Font font;
  private String text[];
  private int halign, valign;

  public static void init() {
    imgImage = new JFImage();
    imgImage.loadPNG(Element.class.getClassLoader().getResourceAsStream("img.png"));
    imgVideo = new JFImage();
    imgVideo.loadPNG(Element.class.getClassLoader().getResourceAsStream("vid.png"));
    imgAudio = new JFImage();
    imgAudio.loadPNG(Element.class.getClassLoader().getResourceAsStream("wav.png"));
    imgCut = new JFImage();
    imgCut.loadPNG(Element.class.getClassLoader().getResourceAsStream("cut.png"));
    imgBlur = new JFImage();
    imgBlur.loadPNG(Element.class.getClassLoader().getResourceAsStream("blur.png"));
    imgText = new JFImage();
    imgText.loadPNG(Element.class.getClassLoader().getResourceAsStream("text.png"));
    previewConfig.width = 64;
    previewConfig.height = 60;
    previewConfig.videoRate = 1;  //one frame per second
    previewConfig.audioChannels = 1;
  }
  public boolean isReady() {
    return ready;
  }
  public boolean start(Config config) {
    if (ready) return true;  //already done
    JFLog.log("Element.start:" + path[0]);
    ready = true;
    eof = false;
    videoRate = config.videoRate;
    if (config.v1001) {
      videoRate = videoRate * 1000.0 / 1001.0;
    }
    switch (type) {
      case TYPE_IMAGE:
        srcImage = new JFImage();
        imgIdx = 0;
        break;
      case TYPE_VIDEO:
        srcImage = new JFImage();
        srcImage.setImageSize(config.width, config.height);
        //no break
      case TYPE_AUDIO:
        try {
          raf = new RandomAccessFile(JF.getUserPath() + "/" + path[0], "r");
        } catch (Exception e) {
          JF.showError("Error", "Failed to open file:" + path[0]);
          JFLog.log(e);
          return false;
        }
        ff = new MediaDecoder();
        if (!ff.start(this, config.width, config.height, config.audioChannels, config.audioRate, true)) {
          JF.showError("Error", "Failed to decode file:" + path[0]);
          return false;
        }
        if (type == TYPE_VIDEO) {
          //calc frameRate
          JFLog.log("input frameRate=" + ff.getFrameRate());
          JFLog.log("output frameRate=" + videoRate);
          frameRateRatio = ff.getFrameRate() / videoRate;
          JFLog.log("frameRateRatio=" + frameRateRatio);
          currentFrame = 0.0;
        }
        audio = null;
        audioSize = 0;
        audioPos = 0;
        gain = 1.0 + ((double)db) / 10.0;
        attn = Math.abs(-1.0 + ((double)db) / 10.0);
        if (audioDelay > 0) {
          audioSize = config.audioChannels * config.audioRate * audioDelay / 1000;
          audio = new short[audioSize];
        }
        break;
      case TYPE_SPECIAL_BLUR:
        startBlur();
        break;
      case TYPE_SPECIAL_TEXT:
        String f[] = fx.split(",", 6);
        if (f[0].equals("left")) {
          halign = 0;
        } else if (f[0].equals("right")) {
          halign = 2;
        } else {
          halign = 1;
        }
        if (f[1].equals("top")) {
          valign = 0;
        } else if (f[1].equals("bottom")) {
          valign = 2;
        } else {
          valign = 1;
        }
        font = new Font(f[2], JF.atoi(f[3]), JF.atoi(f[4]));
        text = f[5].split("\r\n");
        break;
    }
    if (alphaFadeIn) {
      alphaFadeLevel = 0.0;
      alphaFadeStep = 256.0 / ((double)(videoRate * alphaFadeInDuration));
    }
    width = config.width;
    height = config.height;
    return true;
  }
  public void stop() {
    //unload any resources
    if (!ready) return;
    JFLog.log("Element.stop :" + path[0]);
    srcImage = null;
    audio = null;
    audioSize = 0;
    audioPos = 0;
    frames.clear();
    if (ff != null) {
      ff.stop();
      ff = null;
    }
    ready = false;
  }
  private void readMore() {
    if (eof) return;
    switch (ff.read()) {
      case MediaCoder.AUDIO_FRAME:
//JFLog.log("AUDIO_FRAME");
        short buf[] = ff.getAudio();
        if (audioSize == 0) {
          audio = buf;
          audioSize = buf.length;
          audioPos = 0;
        } else {
          //add to audio
          short newAudio[] = new short[audioSize + buf.length];
          System.arraycopy(audio, audioPos, newAudio, 0, audioSize);
          System.arraycopy(buf, 0, newAudio, audioSize, buf.length);
          audio = newAudio;
          audioPos = 0;
          audioSize += buf.length;
        }
        break;
      case MediaCoder.VIDEO_FRAME:
//JFLog.log("VIDEO_FRAME");
        int px[] = ff.getVideo();
        frames.add(px);
        break;
      case MediaCoder.END_FRAME:
//JFLog.log("END_FRAME");
        eof = true;
        break;
    }
  }

  public boolean isCut() {
    return type == TYPE_SPECIAL_CUT;
  }

  public void preRenderVideo() {
    switch (type) {
      case TYPE_AUDIO:
        break;
      case TYPE_IMAGE:
        if (imgIdx >= path.length) return;  //no more images
        srcImage.load(JF.getUserPath() + "/" + path[imgIdx]);
        if (path.length > 1) imgIdx++;
        break;
      case TYPE_VIDEO:
        while (!eof && frames.isEmpty()) readMore();
        if (frames.isEmpty()) return;  //no more video
        int px[] = frames.get(0);
        for(int a=0;a<px.length;a++) {
          px[a] |= 0xff000000;  //test - should already be done
        }
        srcImage.putPixels(px, 0, 0, width, height, 0);
        //BUG : this code to resample video rate is drifting...
        currentFrame += frameRateRatio;
        while (currentFrame >= 1.0) {
          while (!eof && frames.isEmpty()) readMore();
          currentFrame -= 1.0;
          if (frames.isEmpty()) break;  //no more video
          frames.remove(0);
        }
        break;
    }
  }

  public void applyAlpha() {
    applyAlpha(srcImage);
  }

  public void applyAlpha(JFImage image) {
    //apply alpha fx
    if (alphaFadeIn && alphaFadeLevel < alphaLevel) {
      //apply fade in level
      applyAlpha(image, (int)alphaFadeLevel);
      alphaFadeLevel += alphaFadeStep;
    } else {
      if (alphaLevel != 255) {
        //apply alpha level
        applyAlpha(image, alphaLevel);
      }
    }
    //apply clr alpha
    if (clrAlpha != 0) {
      //this is going to be slow
      if ((clrAlpha & 0xff0000) > 0) {
        applyAlphaRed(image, 255 - ((clrAlpha & 0xff0000) >> 16));
      } else if ((clrAlpha & 0xff00) > 0) {
        applyAlphaGreen(image, 255 - ((clrAlpha & 0xff00) >> 8));
      } else if ((clrAlpha & 0xff) > 0) {
        applyAlphaBlue(image, 255 - (clrAlpha & 0xff));
      }
    }
  }

  public void renderVideo(JFImage dstImage, int second, int frame) {
    if (type == TYPE_SPECIAL_CUT || type == TYPE_AUDIO) return;
    if (type == TYPE_SPECIAL_BLUR) {
      //apply blur to image
      applyBlur(dstImage, second, frame);
      return;
    }
    if (type == TYPE_SPECIAL_TEXT) {
      renderText(dstImage, second, frame);
      return;
    }
    dstImage.getGraphics().drawImage(srcImage.getImage()
      , dx1 * width / 100, dy1 * height / 100
      , dx2 * width / 100, dy2 * height / 100
      , sx1 * srcImage.getWidth() / 100, sy1 * srcImage.getHeight() / 100
      , sx2 * srcImage.getWidth() / 100, sy2 * srcImage.getHeight() / 100
      , null);
  }

  private void applyAlpha(JFImage img, int alpha) {
    //do not change if dst alpha == 0
    alpha <<= 24;
    int len = img.getWidth() * img.getHeight();
    int px[] = img.getBuffer();
    for(int a=0;a<len;a++) {
      if ((px[a] & 0xff000000) != 0) {
        px[a] &= 0xffffff;
        px[a] |= alpha;
      }
    }
  }
  private void applyAlphaRed(JFImage img, int lvl) {
    int x = img.getWidth();
    int y = img.getHeight();
    int xy = x * y;
    int pxs[] = img.getBuffer();
    for(int i=0;i<xy;i++) {
      int px = pxs[i];
      int r = (px & 0xff0000) >> 16;
      int g = (px & 0xff00) >> 8;
      int b = px & 0xff;
      if ((r > 128) && (g+b < lvl)) {
        pxs[i] = 0;
      }
    }
  }
  private void applyAlphaGreen(JFImage img, int lvl) {
//    JFLog.log("applyAlphaGreen:" + lvl);
    int x = img.getWidth();
    int y = img.getHeight();
    int xy = x * y;
    int pxs[] = img.getBuffer();
    for(int i=0;i<xy;i++) {
      int px = pxs[i];
      int r = (px & 0xff0000) >> 16;
      int g = (px & 0xff00) >> 8;
      int b = px & 0xff;
      if ((g > 128) && (r+b < lvl)) {
        pxs[i] = 0;
      }
    }
  }
  private void applyAlphaBlue(JFImage img, int lvl) {
    int x = img.getWidth();
    int y = img.getHeight();
    int xy = x * y;
    int pxs[] = img.getBuffer();
    for(int i=0;i<xy;i++) {
      int px = pxs[i];
      int r = (px & 0xff0000) >> 16;
      int g = (px & 0xff00) >> 8;
      int b = px & 0xff;
      if ((b > 128) && (g+r < lvl)) {
        pxs[i] = 0;
      }
    }
  }
  public void renderAudio(short render[]) {
    if (type == TYPE_IMAGE) return;
    if (type == TYPE_SPECIAL_CUT) return;
    if (type == TYPE_SPECIAL_BLUR) return;
    if (type == TYPE_SPECIAL_TEXT) return;
    int renderSize = render.length;
    int renderPos = 0;
    while (renderSize > 0) {
      while (!eof && audioSize < renderSize) readMore();
      int toCopy = renderSize;
      if (toCopy > audioSize) toCopy = audioSize;
      if (toCopy == 0) break;
      if (!mute) {
        if (db != 0) {
          if (db > 0)
            audioCopyGain(audio, audioPos, render, renderPos, toCopy);
          else
            audioCopyAttn(audio, audioPos, render, renderPos, toCopy);
        } else {
          audioCopy(audio, audioPos, render, renderPos, toCopy);
        }
      }
      audioPos += toCopy;
      audioSize -= toCopy;
      renderPos += toCopy;
      renderSize -= toCopy;
    }
  }
  public void audioCopy(short in[], int inpos, short out[], int outpos, int len) {
    for(int a=0;a<len;a++) {
      out[a + outpos] += in[a + inpos];  //TODO : clipping
    }
  }
  public void audioCopyGain(short in[], int inpos, short out[], int outpos, int len) {
    for(int a=0;a<len;a++) {
      out[a + outpos] += (short)(((double)in[a + inpos]) * gain);  //TODO : clipping
    }
  }
  public void audioCopyAttn(short in[], int inpos, short out[], int outpos, int len) {
    for(int a=0;a<len;a++) {
      out[a + outpos] += (short)(((double)in[a + inpos]) / attn);  //TODO : clipping
    }
  }
  public long getDuration() {
    if (ff == null) return 0;
    return ff.getDuration();
  }
  public void seek(int pos) {
    if (ff == null) return;
    ff.seek(pos);
  }
  public void createPreview(JFTask task) {
    if (!start(previewConfig)) return;
    JFImage previewImage;
    int samplesPerPixel;
    preview.clear();
    switch (type) {
      case TYPE_IMAGE:
        for(int frame=0;frame<length;frame += 4) {
          if (task != null) task.setProgress(frame * 100 / length);
          if (imgIdx >= path.length) break;
          srcImage.load(JF.getUserPath() + "/" + path[imgIdx]);
          if (path.length > 1) imgIdx += 4 * frameRateRatio;
          previewImage = new JFImage(64, 60);
          previewImage.getGraphics().drawImage(srcImage.getImage()
            , dx1 * width / 100, dy1 * height / 100
            , dx2 * width / 100, dy2 * height / 100
            , sx1 * srcImage.getWidth() / 100, sy1 * srcImage.getHeight() / 100
            , sx2 * srcImage.getWidth() / 100, sy2 * srcImage.getHeight() / 100
            , null);
          //split image into 4 pieces
          for(int a=0;a<4;a++) {
            preview.add(previewImage.getJFImage(a * 16, 0, 16, 60));
          }
        }
        break;
      case TYPE_VIDEO:
        for(int frame=0;frame<length;frame += 4) {
          if (task != null) task.setProgress(frame * 100 / length);
          frames.clear();
          audio = null;
          audioSize = 0;
          previewImage = new JFImage(64, 60);
          ff.seek(frame);
          preRenderVideo();
          renderVideo(previewImage, 0, 0);
          if (audioSize > 0) {
            samplesPerPixel = audioSize / 64;
            for(int s=0;s<64;s++) {
              int peak = 0;
              if (s % 2 == 1) {
                int pos = s * 16;
                for(int a=0;a<samplesPerPixel;a++,pos++) {
                  if (audio[pos] > peak) peak = audio[pos];
                }
                peak = peak * 16 / 32768;
              } else {
                peak = 0;
              }
              previewImage.line(s, 52 - peak, s, 52 + peak, 0x0000ff);
            }
          }
          //split image into 4 pieces
          for(int a=0;a<4;a++) {
            preview.add(previewImage.getJFImage(a * 16, 0, 16, 60));
          }
        }
        break;
      case TYPE_AUDIO:
        short samples[] = new short[previewConfig.audioRate * previewConfig.audioChannels];
        while (!eof) {
          audio = null;
          audioSize = 0;
          previewImage = new JFImage(16, 60);
          Arrays.fill(samples, (short)0);
          renderAudio(samples);
          samplesPerPixel = samples.length / 16;
          for(int s=0;s<16;s++) {
            int peak = 0;
            if (s % 2 == 1) {
              int pos = s * 16;
              for(int a=0;a<samplesPerPixel;a++,pos++) {
                if (samples[pos] > peak) peak = samples[pos];
              }
              peak = peak * 16 / 32768;
            } else {
              peak = 0;
            }
            previewImage.line(s, 30 - peak, s, 30 + peak, 0x0000ff);
          }
          preview.add(previewImage);
        }
        break;
    }
    stop();
  }

  /*
   * scale 1 = 0,1,2,3 4, 5, 6, 7  8, 9,10,11
   * scale 2 = 0,2,4,6 8,10,12,14 16,18,20,22
   *       |-> 0,1,2,3 8, 9,10,11 16,17,18,19  (scale 2 becomes this)
   */
  public Image getPreview(int offset, int scale) {
    int mod = offset % (4 * scale);
    int off = (offset - mod) + (mod / scale);
    if (off >= preview.size()) {
      switch (type) {
        case TYPE_IMAGE: return imgImage.getImage();
        case TYPE_AUDIO: return imgAudio.getImage();
        case TYPE_VIDEO: return imgVideo.getImage();
        case TYPE_SPECIAL_CUT: return imgCut.getImage();
        case TYPE_SPECIAL_BLUR: return imgBlur.getImage();
        case TYPE_SPECIAL_TEXT: return imgText.getImage();
      }
    }
    return preview.get(off).getImage();
  }

  //blur props
  private int radius;
  private boolean fadein, fadeout;
  private int lastRadius;
  private Kernel kernel;
  private int[] dst;

  private void startBlur() {
    fadein = false;
    fadeout = false;
    String fs[] = fx.split(";");
    for(int a=0;a<fs.length;a++) {
      String f = fs[a];
      if (f.startsWith("radius=")) {
        radius = JF.atoi(f.substring(7));
      } else if (f.equals("fadein")) {
        fadein = true;
      } else if (f.equals("fadeout")) {
        fadeout = true;
      }
    }
    lastRadius = -1;
    dst = null;
  }

  private void applyBlur(JFImage img, int second, int frame) {
    second -= offset;
    int rad;
    if (fadein && second == 0) {
      rad = (int)(radius * (frame / videoRate));
    }
    else if (fadeout && second == length-1) {
      rad = (int)(radius * (1.0 - (frame / videoRate)));
    }
    else {
      rad = radius;
    }
    if (lastRadius != rad) {
      kernel = GaussianFilter.makeKernel(rad);
      lastRadius = rad;
    }
    if (dst == null) {
      dst = new int[width * height];
    }
    int inPixels[] = img.getBuffer();
    int outPixels[] = dst;
    boolean alpha = true, premultiplyAlpha = false;
    if (rad > 0) {
      GaussianFilter.convolveAndTranspose(kernel, inPixels, outPixels, width, height, alpha, alpha && premultiplyAlpha, false, GaussianFilter.CLAMP_EDGES);
      GaussianFilter.convolveAndTranspose(kernel, outPixels, inPixels, height, width, alpha, false, alpha && premultiplyAlpha, GaussianFilter.CLAMP_EDGES);
    }
  }

  public void renderText(JFImage dstImage, int second, int frame) {
    Graphics g = dstImage.getGraphics();
    g.setFont(font);
    g.setColor(new Color(clr));
    int px = 0;
    int py = 0;
    int metrics[] = JF.getFontMetrics(font, "sample");
    int fsy = metrics[1] * 2;  //height
    switch (valign) {
      case 0:
        //top
        py = fsy;
        break;
      case 1:
        //center
        py = (dstImage.getHeight() / 2) - (text.length * fsy / 2);
        break;
      case 2:
        //bottom
        py = dstImage.getHeight() - fsy;
        break;
    }
    for(int a=0;a<text.length;a++) {
      String ln = text[a];
      metrics = JF.getFontMetrics(font, ln);
      int fsx = metrics[0];
      switch (halign) {
        case 0:
          //left
          px = fsy;  //move over "height" of font (so it's not tight against side)
          break;
        case 1:
          //center
          px = (dstImage.getWidth() / 2) - (fsx / 2);
          break;
        case 2:
          //right
          px = (dstImage.getWidth()) - fsx;
          break;
      }
      dstImage.getGraphics().drawString(ln, px, py);
      py += fsy;
    }
  }
}
