/** Interface to send audio levels back to the Panel from the Sound class. Also added method to set speaker button status (green/red). */

public interface MeterController {
  public void setMeterRec(int lvl);
  public void setMeterPlay(int lvl);
  public void setSpeakerStatus(boolean state);
}