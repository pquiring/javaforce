package javaforce.service;

/** SMTP Events
 *
 * @author pquiring
 */

public interface SMTPEvents {
  public void message(SMTP server, String mailbox, String msgfile);
}
