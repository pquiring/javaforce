package jpbx.core;

public interface PBXEventHandler {
  public static final int DIGIT = 1;  //a DTMF digit receieved
  public static final int SOUND = 2;  //sound playback complete
  public void event(CallDetailsPBX cd, int type, char digit, boolean interrupted);
  public void samples(CallDetailsPBX cd, short sam[]);
}
