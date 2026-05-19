package javaforce.ffm;

/** SpeexAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

public class SpeexFFM implements SpeexAPI {

  private FFM ffm;

  private static SpeexFFM instance;
  public static SpeexFFM getInstance() {
    if (instance == null) {
      instance = new SpeexFFM();
      if (!instance.ffm_init()) {
        JFLog.log("SpeexFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle speexCreate;
  public long speexCreate(int sample_rate,int echo_buffers) { try { long _ret_value_ = (long)speexCreate.invokeExact(sample_rate,echo_buffers);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle speexFree;
  public void speexFree(long ctx) { try { speexFree.invokeExact(ctx); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle speexDenoise;
  public void speexDenoise(long ctx,short[] audio,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_audio = FFM.toMemory(arena, audio);speexDenoise.invokeExact(ctx,_array_audio,length);FFM.copyBack(_array_audio,audio); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle speexEcho;
  public void speexEcho(long ctx,short[] audio_mic,short[] audio_spk,short[] audio_out,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_audio_mic = FFM.toMemory(arena, audio_mic);MemorySegment _array_audio_spk = FFM.toMemory(arena, audio_spk);MemorySegment _array_audio_out = FFM.toMemory(arena, audio_out);speexEcho.invokeExact(ctx,_array_audio_mic,_array_audio_spk,_array_audio_out,length);FFM.copyBack(_array_audio_mic,audio_mic);FFM.copyBack(_array_audio_spk,audio_spk);FFM.copyBack(_array_audio_out,audio_out); } catch (Throwable t) { JFLog.log(t); } }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("SpeexAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    speexCreate = ffm.getFunctionPtr("_speexCreate", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_INT,JAVA_INT));
    speexFree = ffm.getFunctionPtr("_speexFree", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    speexDenoise = ffm.getFunctionPtr("_speexDenoise", ffm.getFunctionDesciptorVoid(JAVA_LONG,ADDRESS,JAVA_INT));
    speexEcho = ffm.getFunctionPtr("_speexEcho", ffm.getFunctionDesciptorVoid(JAVA_LONG,ADDRESS,ADDRESS,ADDRESS,JAVA_INT));
    return true;
  }
}
