package javaforce.webui;

/** Icon
 *
 * Displays an icon from classpath /javaforce/icons
 *
 * @author pquiring
 */

public class Icon extends Component {
  private int size;
  private String name;
  private String type;

  /** Loads a 16px icon by name (png) */
  public Icon(String name) {
    size = 16;
    this.name = name;
    type = "png";
  }

  /** Loads an icon of size and name */
  public Icon(int size, String name) {
    this.size = size;
    this.name = name;
    type = "png";
  }

  /** Loads an icon by name of
   * @param name = icon name
   * @param svg = use svg icons (else png)
   */
  public Icon(String name, boolean svg) {
    size = 16;
    this.name = name;
    type = svg ? "svg" : "png";
  }

  private String getsrc() {
    if (type.equals("svg")) {
      return "/icons/svg/" + name + ".svg";
    } else {
      return "/icons/" + size + "/" + name + ".png";
    }
  }

  public String html() {
    return "<img" + getAttrs() +  " src='" + getsrc() + "'>";
  }

}
