/** Contact JLabel. */

public class Contact extends javax.swing.JLabel {
  public Contact(String label, String contact) {
    super(label);
    this.contact = contact;
  }
  public String contact;  //full contact : "name" <sip:number@server;flgs1>;flgs2
  public String callid;
}
