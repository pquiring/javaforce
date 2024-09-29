/** Contact JLabel. */

public class ContactLabel extends javax.swing.JLabel {
  public ContactLabel(Settings.Contact contact) {
    super(contact.name);
    this.contact = contact;
  }
  Settings.Contact contact;
  public String callid;
  public boolean monitor() {
    return contact.monitor;
  }
}
