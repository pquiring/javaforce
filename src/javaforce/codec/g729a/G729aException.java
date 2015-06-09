/* G729aException - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

public class G729aException extends IllegalArgumentException
        implements Constants {

  private int reason;
  private String default_mes = "G729aException0";

  public G729aException() {
    /* empty */
  }

  public G729aException(String string) {
    super(G729aI18N.getString(string));
    default_mes = string;
  }

  public G729aException(int i) {
    reason = i;
  }

  public String toString() {
    String string;
    switch (reason) {
      case 1:
        string = "G729aException1";
        break;
      case 2:
        string = "G729aException2";
        break;
      default:
        string = default_mes;
    }
    String string_0_ = this.getClass().getName();
    return string_0_ + ": " + G729aI18N.getString(string);
  }

  public int getState() {
    return reason;
  }
}