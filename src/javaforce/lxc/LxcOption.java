package javaforce.lxc;

/** Container run-time option.
 *
 * @author pquiring
 */

public class LxcOption {

  public LxcOption() {}

  public LxcOption(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String name;
  public String value;

  //options
  public static final String restart = "restart";  //restart policy
  public static final String port = "port";  //port redirect
  public static final String mount = "mount";  //mount volume

  public LxcOption addRestartPolicy(String policy) {
    return new LxcOption(restart, policy);
  }

  public LxcOption addPort(String host, String container) {
    return new LxcOption(port, host + ":" + container);
  }

  public LxcOption addMount(String host, String container) {
    return new LxcOption(mount, host + ":" + container);
  }

  public String[] splitValue() {
    String[] vs = value.split("[:]");
    if (vs.length != 2) return null;
    return vs;
  }
}
