package javaforce.ffm;

/** MediaAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;
import javaforce.media.*;

public class MediaFFM implements MediaAPI {

  private FFM ffm;

  private static MediaFFM instance;
  public static MediaFFM getInstance() {
    if (instance == null) {
      instance = new MediaFFM();
      if (!instance.ffm_init()) {
        JFLog.log("MediaFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle mediaLoadLibs;
  public boolean mediaLoadLibs(String codec,String device,String filter,String format,String util,String scale,String postproc,String resample) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)mediaLoadLibs.invokeExact(arena.allocateFrom(codec),arena.allocateFrom(device),arena.allocateFrom(filter),arena.allocateFrom(format),arena.allocateFrom(util),arena.allocateFrom(scale),arena.allocateFrom(postproc),arena.allocateFrom(resample));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle mediaSetLogging;
  public void mediaSetLogging(boolean state) { try { mediaSetLogging.invokeExact(state); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle getVideoStream;
  public int getVideoStream(long ctx) { try { int _ret_value_ = (int)getVideoStream.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getAudioStream;
  public int getAudioStream(long ctx) { try { int _ret_value_ = (int)getAudioStream.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getVideoCodecID;
  public int getVideoCodecID(long ctx) { try { int _ret_value_ = (int)getVideoCodecID.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getAudioCodecID;
  public int getAudioCodecID(long ctx) { try { int _ret_value_ = (int)getAudioCodecID.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getVideoBitRate;
  public int getVideoBitRate(long ctx) { try { int _ret_value_ = (int)getVideoBitRate.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getAudioBitRate;
  public int getAudioBitRate(long ctx) { try { int _ret_value_ = (int)getAudioBitRate.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle inputOpenFile;
  public long inputOpenFile(String file,String format) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)inputOpenFile.invokeExact(arena.allocateFrom(file),arena.allocateFrom(format));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle inputOpenIO;
  public long inputOpenIO(MediaIO io) { try { Arena arena = Arena.global(); long _ret_value_ = (long)inputOpenIO.invokeExact(FFM.toMemory(arena, new MemorySegment[] {ffm.getFunctionUpCall(io, "read", int.class, new Class[] {MemorySegment.class, int.class}, arena), ffm.getFunctionUpCall(io, "write", int.class, new Class[] {MemorySegment.class, int.class}, arena), ffm.getFunctionUpCall(io, "seek", long.class, new Class[] {long.class, int.class}, arena)}));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getDuration;
  public long getDuration(long ctx) { try { long _ret_value_ = (long)getDuration.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getVideoWidth;
  public int getVideoWidth(long ctx) { try { int _ret_value_ = (int)getVideoWidth.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getVideoHeight;
  public int getVideoHeight(long ctx) { try { int _ret_value_ = (int)getVideoHeight.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getVideoFrameRate;
  public float getVideoFrameRate(long ctx) { try { float _ret_value_ = (float)getVideoFrameRate.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getVideoKeyFrameInterval;
  public int getVideoKeyFrameInterval(long ctx) { try { int _ret_value_ = (int)getVideoKeyFrameInterval.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getAudioChannels;
  public int getAudioChannels(long ctx) { try { int _ret_value_ = (int)getAudioChannels.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getAudioSampleRate;
  public int getAudioSampleRate(long ctx) { try { int _ret_value_ = (int)getAudioSampleRate.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle inputClose;
  public boolean inputClose(long ctx) { try { boolean _ret_value_ = (boolean)inputClose.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle inputOpenVideo;
  public boolean inputOpenVideo(long ctx,int width,int height) { try { boolean _ret_value_ = (boolean)inputOpenVideo.invokeExact(ctx,width,height);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle inputOpenAudio;
  public boolean inputOpenAudio(long ctx,int chs,int freq) { try { boolean _ret_value_ = (boolean)inputOpenAudio.invokeExact(ctx,chs,freq);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle inputRead;
  public int inputRead(long ctx) { try { int _ret_value_ = (int)inputRead.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getPacketKeyFrame;
  public boolean getPacketKeyFrame(long ctx) { try { boolean _ret_value_ = (boolean)getPacketKeyFrame.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle getPacketData;
  public int getPacketData(long ctx,byte[] data,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);int _ret_value_ = (int)getPacketData.invokeExact(ctx,_array_data,offset,length);FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle inputSeek;
  public boolean inputSeek(long ctx,long seconds) { try { boolean _ret_value_ = (boolean)inputSeek.invokeExact(ctx,seconds);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle outputCreateFile;
  public long outputCreateFile(String file,String format) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)outputCreateFile.invokeExact(arena.allocateFrom(file),arena.allocateFrom(format));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle outputCreateIO;
  public long outputCreateIO(MediaIO io,String format) { try { Arena arena = Arena.global(); long _ret_value_ = (long)outputCreateIO.invokeExact(FFM.toMemory(arena, new MemorySegment[] {ffm.getFunctionUpCall(io, "read", int.class, new Class[] {MemorySegment.class, int.class}, arena), ffm.getFunctionUpCall(io, "write", int.class, new Class[] {MemorySegment.class, int.class}, arena), ffm.getFunctionUpCall(io, "seek", long.class, new Class[] {long.class, int.class}, arena)}),arena.allocateFrom(format));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle addVideoStream;
  public int addVideoStream(long ctx,int codec_id,int bit_rate,int width,int height,float fps,int keyFrameInterval) { try { int _ret_value_ = (int)addVideoStream.invokeExact(ctx,codec_id,bit_rate,width,height,fps,keyFrameInterval);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle addAudioStream;
  public int addAudioStream(long ctx,int codec_id,int bit_rate,int chs,int freq) { try { int _ret_value_ = (int)addAudioStream.invokeExact(ctx,codec_id,bit_rate,chs,freq);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle outputClose;
  public boolean outputClose(long ctx) { try { boolean _ret_value_ = (boolean)outputClose.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle writeHeader;
  public boolean writeHeader(long ctx) { try { boolean _ret_value_ = (boolean)writeHeader.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle writePacket;
  public boolean writePacket(long ctx,int stream,byte[] data,int offset,int length,boolean keyFrame) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);boolean _ret_value_ = (boolean)writePacket.invokeExact(ctx,stream,_array_data,offset,length,keyFrame);FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle audioDecoderStart;
  public long audioDecoderStart(int codec_id,int new_chs,int new_freq) { try { long _ret_value_ = (long)audioDecoderStart.invokeExact(codec_id,new_chs,new_freq);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle audioDecoderStop;
  public void audioDecoderStop(long ctx) { try { audioDecoderStop.invokeExact(ctx); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle audioDecoderDecode;
  public short[] audioDecoderDecode(long ctx,byte[] data,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);short[] _ret_value_ = FFM.toArrayShort((MemorySegment)audioDecoderDecode.invokeExact(ctx,_array_data,offset,length));FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle audioDecoderGetChannels;
  public int audioDecoderGetChannels(long ctx) { try { int _ret_value_ = (int)audioDecoderGetChannels.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle audioDecoderGetSampleRate;
  public int audioDecoderGetSampleRate(long ctx) { try { int _ret_value_ = (int)audioDecoderGetSampleRate.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle audioDecoderChange;
  public boolean audioDecoderChange(long ctx,int chs,int freq) { try { boolean _ret_value_ = (boolean)audioDecoderChange.invokeExact(ctx,chs,freq);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle audioEncoderStart;
  public long audioEncoderStart(int codec_id,int bit_rate,int chs,int freq) { try { long _ret_value_ = (long)audioEncoderStart.invokeExact(codec_id,bit_rate,chs,freq);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle audioEncoderStop;
  public void audioEncoderStop(long ctx) { try { audioEncoderStop.invokeExact(ctx); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle audioEncoderEncode;
  public byte[] audioEncoderEncode(long ctx,short[] samples,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_samples = FFM.toMemory(arena, samples);byte[] _ret_value_ = FFM.toArrayByte((MemorySegment)audioEncoderEncode.invokeExact(ctx,_array_samples,offset,length));FFM.copyBack(_array_samples,samples);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle audioEncoderGetAudioFramesize;
  public int audioEncoderGetAudioFramesize(long ctx) { try { int _ret_value_ = (int)audioEncoderGetAudioFramesize.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle videoDecoderStart;
  public long videoDecoderStart(int codec_id,int new_width,int new_height) { try { long _ret_value_ = (long)videoDecoderStart.invokeExact(codec_id,new_width,new_height);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle videoDecoderStop;
  public void videoDecoderStop(long ctx) { try { videoDecoderStop.invokeExact(ctx); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle videoDecoderDecode;
  public int[] videoDecoderDecode(long ctx,byte[] data,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);int[] _ret_value_ = FFM.toArrayInt((MemorySegment)videoDecoderDecode.invokeExact(ctx,_array_data,offset,length));FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle videoDecoderGetWidth;
  public int videoDecoderGetWidth(long ctx) { try { int _ret_value_ = (int)videoDecoderGetWidth.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle videoDecoderGetHeight;
  public int videoDecoderGetHeight(long ctx) { try { int _ret_value_ = (int)videoDecoderGetHeight.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle videoDecoderGetFrameRate;
  public float videoDecoderGetFrameRate(long ctx) { try { float _ret_value_ = (float)videoDecoderGetFrameRate.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle videoDecoderChange;
  public boolean videoDecoderChange(long ctx,int width,int height) { try { boolean _ret_value_ = (boolean)videoDecoderChange.invokeExact(ctx,width,height);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle videoEncoderStart;
  public long videoEncoderStart(int codec_id,int bit_rate,int width,int height,float fps,int keyFrameInterval) { try { long _ret_value_ = (long)videoEncoderStart.invokeExact(codec_id,bit_rate,width,height,fps,keyFrameInterval);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle videoEncoderStop;
  public void videoEncoderStop(long ctx) { try { videoEncoderStop.invokeExact(ctx); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle videoEncoderEncode;
  public byte[] videoEncoderEncode(long ctx,int[] px,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_px = FFM.toMemory(arena, px);byte[] _ret_value_ = FFM.toArrayByte((MemorySegment)videoEncoderEncode.invokeExact(ctx,_array_px,offset,length));FFM.copyBack(_array_px,px);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle compareFrames;
  public float compareFrames(int[] frame1,int[] frame2,int width,int height) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_frame1 = FFM.toMemory(arena, frame1);MemorySegment _array_frame2 = FFM.toMemory(arena, frame2);float _ret_value_ = (float)compareFrames.invokeExact(_array_frame1,_array_frame2,width,height);FFM.copyBack(_array_frame1,frame1);FFM.copyBack(_array_frame2,frame2);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("MediaAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    mediaLoadLibs = ffm.getFunctionPtr("_mediaLoadLibs", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS,ADDRESS,ADDRESS,ADDRESS,ADDRESS,ADDRESS,ADDRESS));
    mediaSetLogging = ffm.getFunctionPtr("_mediaSetLogging", ffm.getFunctionDesciptorVoid(JAVA_BOOLEAN));
    getVideoStream = ffm.getFunctionPtr("_getVideoStream", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getAudioStream = ffm.getFunctionPtr("_getAudioStream", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getVideoCodecID = ffm.getFunctionPtr("_getVideoCodecID", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getAudioCodecID = ffm.getFunctionPtr("_getAudioCodecID", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getVideoBitRate = ffm.getFunctionPtr("_getVideoBitRate", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getAudioBitRate = ffm.getFunctionPtr("_getAudioBitRate", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    inputOpenFile = ffm.getFunctionPtr("_inputOpenFile", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,ADDRESS));
    inputOpenIO = ffm.getFunctionPtr("_inputOpenIO", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS));
    getDuration = ffm.getFunctionPtr("_getDuration", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_LONG));
    getVideoWidth = ffm.getFunctionPtr("_getVideoWidth", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getVideoHeight = ffm.getFunctionPtr("_getVideoHeight", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getVideoFrameRate = ffm.getFunctionPtr("_getVideoFrameRate", ffm.getFunctionDesciptor(JAVA_FLOAT,JAVA_LONG));
    getVideoKeyFrameInterval = ffm.getFunctionPtr("_getVideoKeyFrameInterval", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getAudioChannels = ffm.getFunctionPtr("_getAudioChannels", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getAudioSampleRate = ffm.getFunctionPtr("_getAudioSampleRate", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    inputClose = ffm.getFunctionPtr("_inputClose", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    inputOpenVideo = ffm.getFunctionPtr("_inputOpenVideo", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_INT,JAVA_INT));
    inputOpenAudio = ffm.getFunctionPtr("_inputOpenAudio", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_INT,JAVA_INT));
    inputRead = ffm.getFunctionPtr("_inputRead", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    getPacketKeyFrame = ffm.getFunctionPtr("_getPacketKeyFrame", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    getPacketData = ffm.getFunctionPtr("_getPacketData", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    inputSeek = ffm.getFunctionPtr("_inputSeek", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG));
    outputCreateFile = ffm.getFunctionPtr("_outputCreateFile", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,ADDRESS));
    outputCreateIO = ffm.getFunctionPtr("_outputCreateIO", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,ADDRESS));
    addVideoStream = ffm.getFunctionPtr("_addVideoStream", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_FLOAT,JAVA_INT));
    addAudioStream = ffm.getFunctionPtr("_addAudioStream", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    outputClose = ffm.getFunctionPtr("_outputClose", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    writeHeader = ffm.getFunctionPtr("_writeHeader", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    writePacket = ffm.getFunctionPtr("_writePacket", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_INT,ADDRESS,JAVA_INT,JAVA_INT,JAVA_BOOLEAN));
    audioDecoderStart = ffm.getFunctionPtr("_audioDecoderStart", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT));
    audioDecoderStop = ffm.getFunctionPtr("_audioDecoderStop", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    audioDecoderDecode = ffm.getFunctionPtr("_audioDecoderDecode", ffm.getFunctionDesciptor(ADDRESS,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    audioDecoderGetChannels = ffm.getFunctionPtr("_audioDecoderGetChannels", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    audioDecoderGetSampleRate = ffm.getFunctionPtr("_audioDecoderGetSampleRate", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    audioDecoderChange = ffm.getFunctionPtr("_audioDecoderChange", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_INT,JAVA_INT));
    audioEncoderStart = ffm.getFunctionPtr("_audioEncoderStart", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    audioEncoderStop = ffm.getFunctionPtr("_audioEncoderStop", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    audioEncoderEncode = ffm.getFunctionPtr("_audioEncoderEncode", ffm.getFunctionDesciptor(ADDRESS,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    audioEncoderGetAudioFramesize = ffm.getFunctionPtr("_audioEncoderGetAudioFramesize", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    videoDecoderStart = ffm.getFunctionPtr("_videoDecoderStart", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT));
    videoDecoderStop = ffm.getFunctionPtr("_videoDecoderStop", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    videoDecoderDecode = ffm.getFunctionPtr("_videoDecoderDecode", ffm.getFunctionDesciptor(ADDRESS,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    videoDecoderGetWidth = ffm.getFunctionPtr("_videoDecoderGetWidth", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    videoDecoderGetHeight = ffm.getFunctionPtr("_videoDecoderGetHeight", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    videoDecoderGetFrameRate = ffm.getFunctionPtr("_videoDecoderGetFrameRate", ffm.getFunctionDesciptor(JAVA_FLOAT,JAVA_LONG));
    videoDecoderChange = ffm.getFunctionPtr("_videoDecoderChange", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_INT,JAVA_INT));
    videoEncoderStart = ffm.getFunctionPtr("_videoEncoderStart", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_FLOAT,JAVA_INT));
    videoEncoderStop = ffm.getFunctionPtr("_videoEncoderStop", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    videoEncoderEncode = ffm.getFunctionPtr("_videoEncoderEncode", ffm.getFunctionDesciptor(ADDRESS,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    compareFrames = ffm.getFunctionPtr("_compareFrames", ffm.getFunctionDesciptor(JAVA_FLOAT,ADDRESS,ADDRESS,JAVA_INT,JAVA_INT));
    return true;
  }
}
