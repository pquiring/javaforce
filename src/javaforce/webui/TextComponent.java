package javaforce.webui;

/** Text Component
 *
 * @author pquiring
 */

public abstract class TextComponent extends Container {
  protected String text;
  public abstract void update();
  public void setText(String text) {
    this.text = text;
    update();
    onChanged(new String[] {"text=" + text});
 }
  public String getText() {
    return text;
  }
  public String destringify(String in) {
    char[] ca = in.toCharArray();
    StringBuilder txt = new StringBuilder();
    for(int a=0;a<ca.length;a++) {
      char ch = ca[a];
      switch (ch) {
        case '\\': {
          switch (ca[++a]) {
            case '\\': {
              txt.append(ch);
              break;
            }
            case 't': {
              txt.append("\t");
              break;
            }
            case 'n': {
              txt.append(System.lineSeparator());
              break;
            }
          }
          break;
        }
        default:
          txt.append(ch);
          break;
      }
    }
    return txt.toString();
  }
}
