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
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<ca.length;a++) {
      char ch = ca[a];
      switch (ch) {
        case '\\': {
          switch (ca[++a]) {
            case '\\': {
              sb.append(ch);
              break;
            }
            case 't': {
              sb.append("\t");
              break;
            }
            case 'n': {
              sb.append(System.lineSeparator());
              break;
            }
          }
          break;
        }
        default:
          sb.append(ch);
          break;
      }
    }
    return sb.toString();
  }
}
