/** Contact JLabel. */

import javaforce.voip.*;

public class Contact extends javax.swing.JLabel {
  public Contact(String label, String sip_user) {
    super(label);
    this.sip_user = sip_user;
  }
  public String sip_user;  //full contact : "name" <sip:number@server;flgs1>;flgs2
  public String callid;
  public boolean monitor() {
    String[] fs = SIP.split(sip_user);
    return SIP.getFlag2(fs, "monitor").equals("true");
  }
}
