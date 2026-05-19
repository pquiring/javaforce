package javaforce.media;

/**
 * Web Camera API
 *
 * @author pquiring
 *
 * Created : Aug 20, 2013
 */

import javaforce.api.*;

public class Camera {
  private long ctx = 0;
  private int[] mirror;
  private int[] flip;

  public Camera() {
  }

  public boolean init() {
    ctx = CameraAPI.getInstance().cameraInit();
    return ctx != 0;
  }

  public boolean uninit() {
    return CameraAPI.getInstance().cameraUninit(ctx);
  }

  public String[] listDevices() {
    return CameraAPI.getInstance().cameraListDevices(ctx);
  }

  public String[] listModes(int deviceIdx) {
    return CameraAPI.getInstance().cameraListModes(ctx, deviceIdx);
  }

  public boolean start(int deviceIdx, int width, int height) {
    return CameraAPI.getInstance().cameraStart(ctx, deviceIdx, width, height);
  }

  public boolean stop() {
    return CameraAPI.getInstance().cameraStop(ctx);
  }

  /** Get next image. */
  public int[] getFrame() {
    return CameraAPI.getInstance().cameraGetFrame(ctx);
  }

  /** Mirror horizontally. */
  private int[] mirror(int[] px) {
    if (mirror == null || mirror.length != px.length) {
      mirror = new int[px.length];
    }
    int width = getWidth();
    int height = getHeight();
    int src = 0;
    int dst = 0;
    for(int y=0;y<height;y++) {
      src += width;
      for(int x=0;x<width;x++) {
        mirror[dst++] = px[--src];
      }
      src += width;
    }
    return mirror;
  }

  /** Get next image and mirror horizontally. */
  public int[] getFrameMirror() {
    int[] px = CameraAPI.getInstance().cameraGetFrame(ctx);
    if (px == null) return null;
    return mirror(px);
  }

  /** Flip image vertically. */
  private int[] flip(int[] px) {
    if (flip == null || flip.length != px.length) {
      flip = new int[px.length];
    }
    int width = getWidth();
    int height = getHeight();
    int src = 0;
    int dst = (height - 1) * width;
    for(int y=0;y<height;y++) {
      for(int x=0;x<width;x++) {
        flip[dst++] = px[src++];
      }
      dst -= width * 2;
    }
    return flip;
  }

  /** Get next image and flip vertically. */
  public int[] getFrameFlip() {
    int[] px = CameraAPI.getInstance().cameraGetFrame(ctx);
    if (px == null) return null;
    return flip(px);
  }

  /** Get next image and mirror horizontally and flip vertically. */
  public int[] getFrameMirrorAndFlip() {
    int[] px = CameraAPI.getInstance().cameraGetFrame(ctx);
    if (px == null) return null;
    px = mirror(px);
    px = flip(px);
    return px;
  }

  public int getWidth() {
    return CameraAPI.getInstance().cameraGetWidth(ctx);
  }

  public int getHeight() {
    return CameraAPI.getInstance().cameraGetHeight(ctx);
  }
}
