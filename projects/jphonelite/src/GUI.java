import javax.swing.*;

import javaforce.voip.*;

public interface GUI {
  public void selectLine(int newline);
  public void updateLine();
  public void updateCallButton(boolean state);
  public void updateEndButton(boolean state);
  public void endLineUpdate(int xline);
  public void callInviteUpdate();
  public void setStatus(String number, String server, String status);
  public void hld_setIcon(ImageIcon ii);
  public void aa_setIcon(ImageIcon ii);
  public void ac_setIcon(ImageIcon ii);
  public void dnd_setIcon(ImageIcon ii);
  public void cnf_setIcon(ImageIcon ii);
  public void mute_setIcon(ImageIcon ii);
  public void spk_setIcon(ImageIcon ii);
  public void onRegister(SIPClient sip);
  public void updateRecentList();
  public String getLineStatus();
  public void doConfig();
}
